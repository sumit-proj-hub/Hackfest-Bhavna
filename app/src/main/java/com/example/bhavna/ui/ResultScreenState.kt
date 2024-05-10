package com.example.bhavna.ui

data class ResultScreenState(
    val emotionValues: Map<String, Float> = emptyMap(),
    val isInitialized: Boolean = false
)