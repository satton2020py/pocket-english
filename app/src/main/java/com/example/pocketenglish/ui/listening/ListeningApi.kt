package com.example.pocketenglish.ui.listening

import com.example.pocketenglish.data.PhraseData
import com.example.pocketenglish.data.remote.GeminiClient
import com.example.pocketenglish.data.remote.GeminiError
import com.example.pocketenglish.data.remote.GeminiResult
import com.example.pocketenglish.data.remote.GeminiTurn
import com.example.pocketenglish.model.Phrase

/** 聞き取った英語の日本語訳。[english] は句読点などを整えた英文。 */
data class HeardTranslation(val english: String, val japanese: String)

/** 翻訳に失敗した理由（UIの案内文を出し分けるため）。 */
enum class TranslateError { NO_API_KEY, OFFLINE, TIMEOUT, INVALID_KEY, BUSY, SERVER, BAD_RESPONSE }

sealed interface TranslateResult {
    data class Success(val translation: HeardTranslation) : TranslateResult
    data class Failure(val error: TranslateError) : TranslateResult
}

/** Gemini API で、旅先で聞いた英語を自然な日本語に訳す（JSON Schema で形を固定）。 */
class ListeningApi(
    private val gemini: GeminiClient = GeminiClient(),
) {

    suspend fun translate(english: String): TranslateResult {
        val result = gemini.generateJson(
            systemInstruction = SYSTEM_PROMPT,
            turns = listOf(GeminiTurn(fromUser = true, text = english)),
            schema = RESPONSE_SCHEMA,
            maxOutputTokens = 512,
        )
        return when (result) {
            is GeminiResult.Failure -> TranslateResult.Failure(result.error.toTranslateError())
            is GeminiResult.Success -> {
                val fixed = result.json.optString("english").trim().ifEmpty { english }
                val japanese = result.json.optString("japanese").trim()
                if (japanese.isEmpty()) {
                    TranslateResult.Failure(TranslateError.BAD_RESPONSE)
                } else {
                    TranslateResult.Success(HeardTranslation(fixed, japanese))
                }
            }
        }
    }

    private fun GeminiError.toTranslateError(): TranslateError = when (this) {
        GeminiError.NO_API_KEY -> TranslateError.NO_API_KEY
        GeminiError.OFFLINE -> TranslateError.OFFLINE
        GeminiError.TIMEOUT -> TranslateError.TIMEOUT
        GeminiError.INVALID_KEY -> TranslateError.INVALID_KEY
        GeminiError.QUOTA -> TranslateError.BUSY
        GeminiError.SERVER -> TranslateError.SERVER
        GeminiError.BLOCKED, GeminiError.TRUNCATED, GeminiError.BAD_RESPONSE -> TranslateError.BAD_RESPONSE
    }

    private companion object {
        val RESPONSE_SCHEMA = GeminiClient.stringObjectSchema("english", "japanese")

        val SYSTEM_PROMPT = """
            You help a Japanese traveler understand English spoken to them abroad
            (by airport staff, hotel clerks, waiters, shop staff, drivers, and so on).
            The input is the output of speech recognition, so it may lack punctuation
            or contain small recognition mistakes.
            - "english": the same English with punctuation and capitalization fixed.
              Correct only obvious recognition mistakes; do not paraphrase.
            - "japanese": a natural, easy-to-understand Japanese translation of it.
              Keep the meaning faithful. If it is a question, make it clearly a question.
            Return a JSON object with the keys "english" and "japanese".
        """.trimIndent()
    }
}

/**
 * オフライン・APIキー未設定時のフォールバック。
 * 聞き取った英語と内蔵フレーズの英文を単語の重なりで比べ、近いものを返す。
 */
object EnglishPhraseMatcher {

    fun findSimilar(english: String, limit: Int = 2): List<Phrase> {
        val queryWords = words(english)
        if (queryWords.isEmpty()) return emptyList()
        return PhraseData.all
            .map { phrase ->
                val target = words(phrase.english)
                val common = queryWords.count { it in target }
                // 両方の長さで割り、短い定型文が何にでも当たるのを防ぐ
                val score = common.toDouble() / maxOf(queryWords.size, target.size)
                phrase to score
            }
            .filter { (_, score) -> score >= MIN_SCORE }
            .sortedByDescending { (_, score) -> score }
            .take(limit)
            .map { (phrase, _) -> phrase }
    }

    private fun words(text: String): Set<String> =
        text.lowercase()
            .replace("'", "")
            .split(Regex("[^a-z0-9]+"))
            .filter { it.isNotEmpty() }
            .toSet()

    private const val MIN_SCORE = 0.5
}
