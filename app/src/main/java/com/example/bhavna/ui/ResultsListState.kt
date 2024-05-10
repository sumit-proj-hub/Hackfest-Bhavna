package com.example.bhavna.ui

import com.example.bhavna.viewmodel.MediaType
import java.io.File

data class Result(
    val resultDir: File,
    val mediaType: MediaType,
    val title: String,
    val uploadStatus: Int,
    val timestamp: Long,
    val isSelected: Boolean = false
) {
    companion object {
        fun getMediaFile(file: File) = File(file, "media")
        fun getDataFile(file: File) = File(file, "data")
        fun getTitleFile(file: File) = File(file, "title")
    }
}

data class ResultsListState(
    val isEnterTitleDialogOpen: Boolean = false,
    val results: Map<Int, Result> = mapOf(),
    val isInitialized: Boolean = false
) {
    val selectionModeActive = results.any { it.value.isSelected }
}