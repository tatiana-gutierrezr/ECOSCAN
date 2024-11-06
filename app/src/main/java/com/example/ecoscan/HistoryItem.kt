package com.example.ecoscan

data class HistoryItem(
    val date: String = "",
    val resultTextMessage: String = "",
    var imageUrl: String = "",
    val objectLabel: String? = "",
)
