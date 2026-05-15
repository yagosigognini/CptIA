package br.com.CapitularIA.data

data class ReadingHistoryEntry(
    val title: String = "",
    val author: String = "",
    val startDate: Long = 0L,
    val endDate: Long = 0L
)
