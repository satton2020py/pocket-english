package com.example.pocketenglish.ui.interpreter

import com.example.pocketenglish.data.remote.GeminiClient
import com.example.pocketenglish.data.remote.GeminiError
import com.example.pocketenglish.data.remote.GeminiResult
import com.example.pocketenglish.data.remote.GeminiTurn

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
 * Gemini API を呼び、日本語を3パターンの英語に変換する（JSON Schema で形を固定）。
 */
class InterpreterApi(
    private val gemini: GeminiClient = GeminiClient(),
) {

    suspend fun interpret(japanese: String): InterpretResult {
        val result = gemini.generateJson(
            systemInstruction = SYSTEM_PROMPT,
            turns = listOf(GeminiTurn(fromUser = true, text = japanese)),
            schema = RESPONSE_SCHEMA,
            maxOutputTokens = 512,
        )
        return when (result) {
            is GeminiResult.Failure -> InterpretResult.Failure(result.error.toInterpretError())
            is GeminiResult.Success -> {
                val polite = result.json.optString("polite").trim()
                val casual = result.json.optString("casual").trim()
                val short = result.json.optString("short").trim()
                if (polite.isEmpty() || casual.isEmpty() || short.isEmpty()) {
                    InterpretResult.Failure(InterpretError.BAD_RESPONSE)
                } else {
                    InterpretResult.Success(Translations(japanese, polite, casual, short))
                }
            }
        }
    }

    private fun GeminiError.toInterpretError(): InterpretError = when (this) {
        GeminiError.NO_API_KEY -> InterpretError.NO_API_KEY
        GeminiError.OFFLINE -> InterpretError.OFFLINE
        GeminiError.TIMEOUT -> InterpretError.TIMEOUT
        GeminiError.INVALID_KEY -> InterpretError.INVALID_KEY
        GeminiError.QUOTA -> InterpretError.BUSY
        GeminiError.SERVER -> InterpretError.SERVER
        GeminiError.BLOCKED, GeminiError.TRUNCATED, GeminiError.BAD_RESPONSE -> InterpretError.BAD_RESPONSE
    }

    private companion object {
        val RESPONSE_SCHEMA = GeminiClient.stringObjectSchema("polite", "casual", "short")

        val SYSTEM_PROMPT = """
            You are a pocket interpreter for a Japanese traveler abroad.
            Translate the user's Japanese into natural spoken English in exactly three styles:
            - "polite": courteous, complete sentence suitable for hotel/shop staff or strangers.
            - "casual": friendly, natural everyday English a native speaker would say.
            - "short": the shortest phrase (ideally 1-4 words) that still gets the meaning across.
            Keep the meaning faithful; fill in obvious omitted subjects naturally.
            If the input is not Japanese, still return the best English in the three styles.
            Return a JSON object with the keys "polite", "casual" and "short".
        """.trimIndent()
    }
}

fun Translations.text(style: EnglishStyle): String = when (style) {
    EnglishStyle.POLITE -> polite
    EnglishStyle.CASUAL -> casual
    EnglishStyle.SHORT -> short
}
