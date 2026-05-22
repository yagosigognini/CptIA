package br.com.CapitularIA.services.search

import java.text.Normalizer

object BookSearchQueryBuilder {

    data class QueryBuildResult(
        val normalizedTerm: String,
        val primaryQuery: String,
        val fallbackQuery: String
    )

    fun build(rawTerm: String): QueryBuildResult {
        val normalizedTerm = rawTerm
            .normalizeAccents()
            .trim()
            .replace(Regex("\\s+"), " ")

        if (normalizedTerm.isBlank()) {
            return QueryBuildResult(normalizedTerm = "", primaryQuery = "", fallbackQuery = "")
        }

        val isbnDigits = normalizedTerm.filter { it.isDigit() }
        if (isbnDigits.length == 10 || isbnDigits.length == 13) {
            return QueryBuildResult(
                normalizedTerm = normalizedTerm,
                primaryQuery = "isbn:$isbnDigits",
                fallbackQuery = normalizedTerm
            )
        }

        val titleAuthorPattern = Regex("^(.+?)\\s*(?:-|/)\\s*(.+)$")
        val titleAuthorMatch = titleAuthorPattern.find(normalizedTerm)
        if (titleAuthorMatch != null) {
            val title = titleAuthorMatch.groupValues[1].trim()
            val author = titleAuthorMatch.groupValues[2].trim()

            if (title.isNotBlank() && author.isNotBlank()) {
                return QueryBuildResult(
                    normalizedTerm = normalizedTerm,
                    primaryQuery = "intitle:${title.toGoogleTerm()}+inauthor:${author.toGoogleTerm()}",
                    fallbackQuery = "$title $author"
                )
            }
        }

        return QueryBuildResult(
            normalizedTerm = normalizedTerm,
            primaryQuery = "intitle:${normalizedTerm.toGoogleTerm()}+${normalizedTerm.toGoogleTerm()}",
            fallbackQuery = normalizedTerm
        )
    }

    private fun String.normalizeAccents(): String =
        Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

    private fun String.toGoogleTerm(): String = trim().replace(Regex("\\s+"), "+")
}
