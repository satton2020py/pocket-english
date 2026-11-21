package com.example.pocketenglish.ui.chat

import com.example.pocketenglish.data.remote.ApiConfig
import com.example.pocketenglish.data.remote.HttpClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/** 会話履歴の1発言（API送信用）。 */
data class ChatTurn(val fromUser: Boolean, val text: String)

/** AIの返答。[reply] は英語2文以内、[tip] は直前のユーザー発話への日本語ワンポイント。 */
data class AiReply(val reply: String, val tip: String)

sealed interface ChatResult {
    data class Success(val reply: AiReply) : ChatResult

    /** UIにそのまま出せる日本語のメッセージを持つ失敗。 */
    data class Failure(val message: String, val retryable: Boolean) : ChatResult
}

/**
 * Claude Messages API を OkHttp で直接呼ぶ（AI会話タブ専用）。
 * Structured Outputs（output_config.format）で JSON を強制し、reply / tip を受け取る。
 */
class ClaudeChatClient(
    private val client: OkHttpClient = HttpClientProvider.client,
) {

    suspend fun reply(situation: ChatSituation, history: List<ChatTurn>): ChatResult {
        if (!ApiConfig.hasApiKey) {
            return ChatResult.Failure(
                "APIキーが設定されていないためAIと会話できません。local.properties に ANTHROPIC_API_KEY を設定してビルドしてください",
                retryable = false,
            )
        }
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(ApiConfig.BASE_URL)
                    .header("x-api-key", ApiConfig.apiKey)
                    .header("anthropic-version", ApiConfig.ANTHROPIC_VERSION)
                    .header("content-type", "application/json")
                    .post(buildBody(situation, history).toString().toRequestBody(JSON))
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext httpFailure(response.code)
                    }
                    parse(body)
                }
            } catch (e: IOException) {
                ChatResult.Failure("通信できませんでした。電波の良い場所でもう一度お試しください", retryable = true)
            } catch (e: JSONException) {
                ChatResult.Failure("AIの返答を読み取れませんでした。もう一度お試しください", retryable = true)
            }
        }
    }

    private fun buildBody(situation: ChatSituation, history: List<ChatTurn>): JSONObject {
        // APIの messages は user から始める必要があるため、AIの最初のひと言はシステムプロンプト側に入れる
        val trimmed = history.takeLast(MAX_TURNS).dropWhile { !it.fromUser }
        val messages = JSONArray()
        trimmed.forEach { turn ->
            messages.put(
                JSONObject()
                    .put("role", if (turn.fromUser) "user" else "assistant")
                    .put("content", turn.text),
            )
        }
        return JSONObject()
            .put("model", ApiConfig.model)
            .put("max_tokens", 1024)
            .put("system", systemPrompt(situation))
            .put("messages", messages)
            .put(
                "output_config",
                JSONObject().put(
                    "format",
                    JSONObject()
                        .put("type", "json_schema")
                        .put("schema", RESPONSE_SCHEMA),
                ),
            )
    }

    private fun systemPrompt(situation: ChatSituation): String = """
        You are an English conversation partner in a roleplay app for Japanese travelers who are beginners in English.
        ${situation.role}
        You opened the conversation by saying: "${situation.opening}"

        Rules:
        - Stay in character. Write "reply" in simple, natural spoken English with at most 2 short sentences, and usually end with a question that keeps the conversation going.
        - The user's words come from speech recognition, so ignore small recognition errors, missing punctuation and capitalization.
        - If the user writes in Japanese, answer in easy English as if they had said it in English.
        - Write "tip" in Japanese, in one line (under 60 characters). Give one concrete improvement for the user's latest message, such as a more natural phrase in English. If it was already good, praise it briefly and suggest one alternative expression.
    """.trimIndent()

    private fun parse(body: String): ChatResult {
        val root = JSONObject(body)
        when (root.optString("stop_reason")) {
            "refusal" -> return ChatResult.Failure("この内容には返答できませんでした。別の言い方で話しかけてみてください", retryable = false)
            "max_tokens" -> return ChatResult.Failure("返答が長すぎて途中で切れました。もう一度お試しください", retryable = true)
        }
        val content = root.getJSONArray("content")
        // 思考ブロックなどが先に来ることもあるので text ブロックを探す
        val text = (0 until content.length())
            .map { content.getJSONObject(it) }
            .firstOrNull { it.optString("type") == "text" }
            ?.optString("text")
            ?: return ChatResult.Failure("AIの返答が空でした。もう一度お試しください", retryable = true)
        val json = JSONObject(text)
        val reply = json.optString("reply").trim()
        if (reply.isEmpty()) {
            return ChatResult.Failure("AIの返答が空でした。もう一度お試しください", retryable = true)
        }
        return ChatResult.Success(AiReply(reply = reply, tip = json.optString("tip").trim()))
    }

    private fun httpFailure(code: Int): ChatResult.Failure = when (code) {
        401, 403 -> ChatResult.Failure("APIキーが無効です。local.properties の ANTHROPIC_API_KEY を確認してください", retryable = false)
        429 -> ChatResult.Failure("混み合っています。少し待ってからもう一度お試しください", retryable = true)
        in 500..599 -> ChatResult.Failure("AIサーバーが一時的に応答していません。少し待ってからお試しください", retryable = true)
        else -> ChatResult.Failure("AIとの通信でエラーが起きました（$code）。もう一度お試しください", retryable = true)
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()

        /** 料金と応答速度を抑えるため、送る履歴は直近の発言だけにする。 */
        const val MAX_TURNS = 16

        val RESPONSE_SCHEMA: JSONObject = JSONObject()
            .put("type", "object")
            .put(
                "properties",
                JSONObject()
                    .put("reply", JSONObject().put("type", "string"))
                    .put("tip", JSONObject().put("type", "string")),
            )
            .put("required", JSONArray().put("reply").put("tip"))
            .put("additionalProperties", false)
    }
}
