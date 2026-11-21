package com.example.pocketenglish.ui.interpreter

import com.example.pocketenglish.data.PhraseData
import com.example.pocketenglish.model.Phrase

/**
 * オフライン・APIキー未設定時のフォールバック。
 * 入力した日本語と内蔵フレーズの日本語を文字の2-gramで比べ、近いものを返す。
 */
object LocalPhraseMatcher {

    fun findSimilar(japanese: String, limit: Int = 3): List<Phrase> {
        val query = normalize(japanese)
        if (query.isEmpty()) return emptyList()
        val queryGrams = bigrams(query)

        return PhraseData.all
            .map { phrase ->
                val target = normalize(phrase.japanese)
                val score = if (queryGrams.isEmpty()) {
                    if (target.contains(query)) 1.0 else 0.0
                } else {
                    val targetGrams = bigrams(target)
                    queryGrams.count { it in targetGrams }.toDouble() / queryGrams.size
                }
                phrase to score
            }
            .filter { (_, score) -> score >= MIN_SCORE }
            .sortedByDescending { (_, score) -> score }
            .take(limit)
            .map { (phrase, _) -> phrase }
    }

    private fun normalize(text: String): String =
        text.filterNot { it.isWhitespace() || it in PUNCTUATION }

    private fun bigrams(text: String): Set<String> =
        if (text.length < 2) emptySet() else text.windowed(2).toSet()

    private const val MIN_SCORE = 0.3
    private const val PUNCTUATION = "。、．，？！?!・「」『』（）()…ー〜~"
}
