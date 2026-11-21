package com.example.pocketenglish.data.remote

import android.util.Base64
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.coroutines.resume

/** Gemini に送る1発言。[fromUser] が false ならAI（role = "model"）の発言。 */
data class GeminiTurn(val fromUser: Boolean, val text: String)

/** 失敗の種類（各タブがUIの案内文を出し分けるため）。 */
enum class GeminiError {
    NO_API_KEY,
    OFFLINE,
    TIMEOUT,
    INVALID_KEY,

    /** 429。無料枠では1分・1日の回数上限に達したときもこれになる。 */
    QUOTA,
    SERVER,

    /** 安全フィルタで入力または出力がブロックされた。 */
    BLOCKED,

    /** 出力が最大トークン数で途中で切れた。 */
    TRUNCATED,
    BAD_RESPONSE,
}

sealed interface GeminiResult {
    data class Success(val json: JSONObject) : GeminiResult
    data class Failure(val error: GeminiError) : GeminiResult
}

/**
 * Gemini API（generateContent）を OkHttp で直接呼び、JSON Schema どおりの JSON を受け取る。
 * JSON の組み立て・解析は Android 標準の org.json を使い、追加ライブラリは入れない。
 */
class GeminiClient(
    private val client: OkHttpClient = HttpClientProvider.client,
) {

    suspend fun generateJson(
        systemInstruction: String,
        turns: List<GeminiTurn>,
        schema: JSONObject,
        maxOutputTokens: Int,
        jpegImage: ByteArray? = null,
    ): GeminiResult {
        if (!ApiConfig.hasApiKey) return GeminiResult.Failure(GeminiError.NO_API_KEY)

        val contents = JSONArray()
        turns.forEachIndexed { index, turn ->
            val parts = JSONArray()
            // 画像（カメラ翻訳）は最後のユーザー発言に添付する。Gemini は画像を先に置くと精度が上がる
            if (jpegImage != null && turn.fromUser && index == turns.lastIndex) {
                parts.put(
                    JSONObject().put(
                        "inlineData",
                        JSONObject()
                            .put("mimeType", "image/jpeg")
                            .put("data", Base64.encodeToString(jpegImage, Base64.NO_WRAP)),
                    ),
                )
            }
            parts.put(JSONObject().put("text", turn.text))
            contents.put(
                JSONObject()
                    .put("role", if (turn.fromUser) "user" else "model")
                    .put("parts", parts),
            )
        }
        val body = JSONObject()
            .put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))),
            )
            .put("contents", contents)
            .put(
                "generationConfig",
                JSONObject()
                    .put("maxOutputTokens", maxOutputTokens)
                    .put("responseMimeType", "application/json")
                    .put("responseJsonSchema", schema),
            )

        val request = Request.Builder()
            .url(ApiConfig.generateContentUrl)
            .header("x-goog-api-key", ApiConfig.apiKey)
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return when (val outcome = execute(request)) {
            is HttpOutcome.NetworkError -> GeminiResult.Failure(outcome.error)
            is HttpOutcome.Done -> when {
                outcome.code in 200..299 -> parse(outcome.body)
                // キーが無効なとき Gemini は 400（API_KEY_INVALID）を返す
                outcome.code == 400 && outcome.body.contains("API_KEY_INVALID") ->
                    GeminiResult.Failure(GeminiError.INVALID_KEY)
                outcome.code == 401 || outcome.code == 403 -> GeminiResult.Failure(GeminiError.INVALID_KEY)
                outcome.code == 429 -> GeminiResult.Failure(GeminiError.QUOTA)
                outcome.code >= 500 -> GeminiResult.Failure(GeminiError.SERVER)
                else -> GeminiResult.Failure(GeminiError.BAD_RESPONSE)
            }
        }
    }

    /** enqueue + コルーチンのキャンセルで通信も中断する（画面を離れたら古いリクエストを残さない）。 */
    private suspend fun execute(request: Request): HttpOutcome =
        suspendCancellableCoroutine { cont ->
            val call = client.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    val error = if (e is SocketTimeoutException) GeminiError.TIMEOUT else GeminiError.OFFLINE
                    if (cont.isActive) cont.resume(HttpOutcome.NetworkError(error))
                }

                override fun onResponse(call: Call, response: Response) {
                    val outcome = try {
                        response.use { HttpOutcome.Done(it.code, it.body?.string().orEmpty()) }
                    } catch (e: IOException) {
                        HttpOutcome.NetworkError(GeminiError.OFFLINE)
                    }
                    if (cont.isActive) cont.resume(outcome)
                }
            })
        }

    private fun parse(raw: String): GeminiResult = try {
        val root = JSONObject(raw)
        val blockReason = root.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
        val candidate = root.optJSONArray("candidates")?.optJSONObject(0)
        when {
            blockReason.isNotEmpty() || candidate == null -> GeminiResult.Failure(GeminiError.BLOCKED)
            else -> when (candidate.optString("finishReason")) {
                "MAX_TOKENS" -> GeminiResult.Failure(GeminiError.TRUNCATED)
                "SAFETY", "PROHIBITED_CONTENT", "BLOCKLIST", "SPII", "RECITATION" ->
                    GeminiResult.Failure(GeminiError.BLOCKED)
                else -> {
                    val parts = candidate.optJSONObject("content")?.optJSONArray("parts") ?: JSONArray()
                    val text = buildString {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            // 思考の要約パートは除外して本文だけをつなぐ
                            if (!part.optBoolean("thought")) append(part.optString("text"))
                        }
                    }
                    // 念のため前後の余計な文字（```json など）を除いて最初の {…} だけを読む
                    val start = text.indexOf('{')
                    val end = text.lastIndexOf('}')
                    if (start < 0 || end <= start) {
                        GeminiResult.Failure(GeminiError.BAD_RESPONSE)
                    } else {
                        GeminiResult.Success(JSONObject(text.substring(start, end + 1)))
                    }
                }
            }
        }
    } catch (e: JSONException) {
        GeminiResult.Failure(GeminiError.BAD_RESPONSE)
    }

    private sealed interface HttpOutcome {
        data class Done(val code: Int, val body: String) : HttpOutcome
        data class NetworkError(val error: GeminiError) : HttpOutcome
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        /** 文字列プロパティだけを持つ、全項目必須の JSON Schema を作る。 */
        fun stringObjectSchema(vararg names: String): JSONObject {
            val properties = JSONObject()
            val required = JSONArray()
            names.forEach { name ->
                properties.put(name, JSONObject().put("type", "string"))
                required.put(name)
            }
            return JSONObject()
                .put("type", "object")
                .put("properties", properties)
                .put("required", required)
                .put("additionalProperties", false)
        }
    }
}
