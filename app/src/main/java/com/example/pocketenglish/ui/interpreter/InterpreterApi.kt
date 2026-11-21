package com.example.pocketenglish.ui.interpreter

import com.example.pocketenglish.data.remote.ApiConfig
import com.example.pocketenglish.data.remote.HttpClientProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.resume

/** 1つの日本語に対する3パターンの英訳。 */
data class Translations(
    val japanese: String,
    val polite: String,
    val casual: String,
    val short: String,
)

/** 通訳に失敗した理由（UIの案内文を出し分けるため）。 */
enum class InterpretError {
    NO_API_KEY,
    OFFLINE,
    TIMEOUT,
    INVALID_KEY,
    BUSY,
    SERVER,
    BAD_RESPONSE,
}

sealed interface InterpretResult {
    data class Success(val translations: Translations) : InterpretResult
    data class Failure(val error: InterpretError) : InterpretResult
}

/**
 * Claude API（Messages API）を OkHttp で直接呼び、日本語を3パターンの英語に変換する。
 * JSON の組み立て・解析は Android 標準の org.json を使い、追加ライブラリは入れない。
 */
class InterpreterApi {

    suspend fun interpret(japanese: String): InterpretResult {
        if (!ApiConfig.hasApiKey) return InterpretResult.Failure(InterpretError.NO_API_KEY)

        val body = JSONObject()
            .put("model", ApiConfig.model)
            .put("max_tokens", 300)
            .put("temperature", 0.3)
            .put("system", SYSTEM_PROMPT)
            .put(
                "messages",
                JSONArray().put(JSONObject().put("role", "user").put("content", japanese)),
            )
            .toString()

        val request = Request.Builder()
            .url(ApiConfig.BASE_URL)
            .header("x-api-key", ApiConfig.apiKey)
            .header("anthropic-version", ApiConfig.ANTHROPIC_VERSION)
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return when (val response = execute(request)) {
            is HttpOutcome.NetworkError -> InterpretResult.Failure(response.error)
            is HttpOutcome.Done -> when (response.code) {
                in 200..299 -> parse(japanese, response.body)
                401, 403 -> InterpretResult.Failure(InterpretError.INVALID_KEY)
                429, 529 -> InterpretResult.Failure(InterpretError.BUSY)
                else -> InterpretResult.Failure(InterpretError.SERVER)
            }
        }
    }

    /** enqueue + コルーチンのキャンセルで通信も中断する（連続入力で古いリクエストが残らない）。 */
    private suspend fun execute(request: Request): HttpOutcome =
        suspendCancellableCoroutine { cont ->
            val call = HttpClientProvider.client.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    val error = when (e) {
                        is SocketTimeoutException -> InterpretError.TIMEOUT
                        is UnknownHostException -> InterpretError.OFFLINE
                        else -> InterpretError.OFFLINE
                    }
                    if (cont.isActive) cont.resume(HttpOutcome.NetworkError(error))
                }

                override fun onResponse(call: Call, response: Response) {
                    val outcome = try {
                        response.use { HttpOutcome.Done(it.code, it.body?.string().orEmpty()) }
                    } catch (e: IOException) {
                        HttpOutcome.NetworkError(InterpretError.OFFLINE)
                    }
                    if (cont.isActive) cont.resume(outcome)
                }
            })
        }

    private fun parse(japanese: String, raw: String): InterpretResult = try {
        val content = JSONObject(raw).getJSONArray("content")
        val text = buildString {
            for (i in 0 until content.length()) {
                val block = content.getJSONObject(i)
                if (block.optString("type") == "text") append(block.optString("text"))
            }
        }
        // 念のため前後の余計な文字（```json など）を除いて最初の {…} だけを読む
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) throw JSONException("no json object")
        val json = JSONObject(text.substring(start, end + 1))
        val polite = json.optString("polite").trim()
        val casual = json.optString("casual").trim()
        val short = json.optString("short").trim()
        if (polite.isEmpty() || casual.isEmpty() || short.isEmpty()) {
            InterpretResult.Failure(InterpretError.BAD_RESPONSE)
        } else {
            InterpretResult.Success(Translations(japanese, polite, casual, short))
        }
    } catch (e: JSONException) {
        InterpretResult.Failure(InterpretError.BAD_RESPONSE)
    }

    private sealed interface HttpOutcome {
        data class Done(val code: Int, val body: String) : HttpOutcome
        data class NetworkError(val error: InterpretError) : HttpOutcome
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        val SYSTEM_PROMPT = """
            You are a pocket interpreter for a Japanese traveler abroad.
            Translate the user's Japanese into natural spoken English in exactly three styles:
            - "polite": courteous, complete sentence suitable for hotel/shop staff or strangers.
            - "casual": friendly, natural everyday English a native speaker would say.
            - "short": the shortest phrase (ideally 1-4 words) that still gets the meaning across.
            Keep the meaning faithful; fill in obvious omitted subjects naturally.
            If the input is not Japanese, still return the best English in the three styles.
            Reply with ONLY a JSON object and nothing else:
            {"polite": "...", "casual": "...", "short": "..."}
        """.trimIndent()
    }
}

fun Translations.text(style: EnglishStyle): String = when (style) {
    EnglishStyle.POLITE -> polite
    EnglishStyle.CASUAL -> casual
    EnglishStyle.SHORT -> short
}
