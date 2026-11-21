package com.example.pocketenglish.ui.chat

import com.example.pocketenglish.data.remote.GeminiClient
import com.example.pocketenglish.data.remote.GeminiError
import com.example.pocketenglish.data.remote.GeminiResult
import com.example.pocketenglish.data.remote.GeminiTurn

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
 * ロールプレイの返答を Gemini API から受け取る（AI会話タブ専用）。
 * JSON Schema で {"reply", "tip"} の形を強制する。
 */
class AiChatClient(
    private val gemini: GeminiClient = GeminiClient(),
) {

    suspend fun reply(situation: ChatSituation, history: List<ChatTurn>): ChatResult {
        // contents は user から始める。AIの最初のひと言はシステム指示の側に入れる
        val turns = history.takeLast(MAX_TURNS)
            .dropWhile { !it.fromUser }
            .map { GeminiTurn(fromUser = it.fromUser, text = it.text) }
        val result = gemini.generateJson(
            systemInstruction = systemPrompt(situation),
            turns = turns,
            schema = RESPONSE_SCHEMA,
            maxOutputTokens = 1024,
        )
        return when (result) {
            is GeminiResult.Failure -> failure(result.error)
            is GeminiResult.Success -> {
                val reply = result.json.optString("reply").trim()
                if (reply.isEmpty()) {
                    ChatResult.Failure("AIの返答が空でした。もう一度お試しください", retryable = true)
                } else {
                    ChatResult.Success(AiReply(reply = reply, tip = result.json.optString("tip").trim()))
                }
            }
        }
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

    private fun failure(error: GeminiError): ChatResult.Failure = when (error) {
        GeminiError.NO_API_KEY -> ChatResult.Failure(
            "APIキーが設定されていないためAIと会話できません。local.properties に GEMINI_API_KEY を設定してビルドしてください",
            retryable = false,
        )
        GeminiError.OFFLINE -> ChatResult.Failure("通信できませんでした。電波の良い場所でもう一度お試しください", retryable = true)
        GeminiError.TIMEOUT -> ChatResult.Failure("通信がタイムアウトしました。もう一度お試しください", retryable = true)
        GeminiError.INVALID_KEY -> ChatResult.Failure(
            "APIキーが無効です。local.properties の GEMINI_API_KEY を確認してください",
            retryable = false,
        )
        GeminiError.QUOTA -> ChatResult.Failure(
            "無料枠の利用回数の上限に達したか、混み合っています。1分ほど待ってからお試しください（1日の上限なら日本時間の夕方5時ごろにリセットされます）",
            retryable = true,
        )
        GeminiError.SERVER -> ChatResult.Failure("AIサーバーが一時的に応答していません。少し待ってからお試しください", retryable = true)
        GeminiError.BLOCKED -> ChatResult.Failure("この内容には返答できませんでした。別の言い方で話しかけてみてください", retryable = false)
        GeminiError.TRUNCATED -> ChatResult.Failure("返答が長すぎて途中で切れました。もう一度お試しください", retryable = true)
        GeminiError.BAD_RESPONSE -> ChatResult.Failure("AIの返答を読み取れませんでした。もう一度お試しください", retryable = true)
    }

    private companion object {
        /** 無料枠のトークン消費と応答速度を抑えるため、送る履歴は直近の発言だけにする。 */
        const val MAX_TURNS = 16

        val RESPONSE_SCHEMA = GeminiClient.stringObjectSchema("reply", "tip")
    }
}
