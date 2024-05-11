package com.example.bhavna.ui

data class ResultScreenState(
    val emotionValues: Map<String, Float> = emptyMap(),
    val videoPosition: Long = 0,
    val isInitialized: Boolean = false
)