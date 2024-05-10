package com.example.bhavna.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bhavna.ProgressCallback
import com.example.bhavna.ProgressRequestBody
import com.example.bhavna.UploadClient
import com.example.bhavna.ui.Result
import com.example.bhavna.ui.ResultsListState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.io.InputStream

enum class MediaType {
    Image, Video;

    companion object {
        fun fromDirName(dirName: String): MediaType = if (dirName.endsWith(Image.name))
            Image
        else
            Video
    }
}

class ResultsListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ResultsListState())
    val uiState = _uiState.asStateFlow()

    fun onMediaPicked(context: Context, uri: Uri, title: String) {
        val mimeType = context.contentResolver.getType(uri) ?: return
        val mediaType = when {
            mimeType.startsWith("image/") -> MediaType.Image
            mimeType.startsWith("video/") -> MediaType.Video
            else -> return
        }
        val (resultDir, timestamp) = createFileStructure(context, title, mediaType)
        copyFileFromUri(uri, Result.getMediaFile(resultDir), context.contentResolver)
        val resultKey = addResult(Result(resultDir, mediaType, title, 0, timestamp))
        uploadFile(resultKey, resultDir, mediaType)
    }

    fun onMediaCapture(context: Context, mediaType: MediaType, title: String) {
        val (resultDir, timestamp) = createFileStructure(context, title, mediaType)
        File(context.cacheDir, "media").renameTo(Result.getMediaFile(resultDir))
        val resultKey = addResult(Result(resultDir, mediaType, title, 0, timestamp))
        uploadFile(resultKey, resultDir, mediaType)
    }

    fun requestMediaCapture(
        context: Context,
        launcher: ManagedActivityResultLauncher<Uri, Boolean>,
    ) {
        val file = File(context.cacheDir, "media")
        file.createNewFile()
        val uri = FileProvider.getUriForFile(context, "com.example.bhavna.provider", file)
        launcher.launch(uri)
    }

    private fun copyFileFromUri(uri: Uri, destinationFile: File, contentResolver: ContentResolver) {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(destinationFile)
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun addResult(result: Result): Int {
        val currentResults = _uiState.value.results.toMutableMap()
        val nextKey = (currentResults.keys.maxOrNull() ?: -1) + 1
        currentResults[nextKey] = result
        _uiState.value = _uiState.value.copy(results = currentResults)
        return nextKey
    }

    private fun createFileStructure(
        context: Context,
        title: String,
        mediaType: MediaType,
    ): Pair<File, Long> {
        val currentTimestamp = System.currentTimeMillis()
        val resultDir = File(context.filesDir, "$currentTimestamp.${mediaType.name}")
        resultDir.mkdir()
        val fileWriter = FileWriter(Result.getTitleFile(resultDir))
        fileWriter.write(title)
        fileWriter.close()
        return Pair(resultDir, currentTimestamp)
    }

    private fun updateUploadStatus(resultKey: Int, newUploadStatus: Int) {
        val resultMap = _uiState.value.results.toMutableMap()
        resultMap[resultKey] = resultMap[resultKey]!!.copy(uploadStatus = newUploadStatus)
        _uiState.value = _uiState.value.copy(results = resultMap)
    }

    fun uploadFile(resultKey: Int, resultDir: File, mediaType: MediaType) {
        val progressCallback = ProgressCallback { bytesUploaded, totalBytes ->
            updateUploadStatus(resultKey, (bytesUploaded * 100 / totalBytes).toInt())
        }

        val body = Result.getMediaFile(resultDir).asRequestBody()
        val progressRequestBody = ProgressRequestBody(body, progressCallback)
        val fileTypePart = MultipartBody.Part.createFormData("fileType", mediaType.name)
        val filePart = MultipartBody.Part.createFormData("file", "file", progressRequestBody)

        viewModelScope.launch {
            try {
                val response = UploadClient.instance.upload(fileTypePart, filePart)
                val outputStream = FileOutputStream(Result.getDataFile(resultDir))
                outputStream.write(response.body()!!.bytes())
                outputStream.close()
                updateUploadStatus(resultKey, 200)
            } catch (_: Exception) {
                if (_uiState.value.results.containsKey(resultKey))
                    updateUploadStatus(resultKey, -1)
            }
        }
    }

    fun changeTitleDialogState(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isEnterTitleDialogOpen = isOpen)
    }

    fun initializeState(context: Context) {
        val results = mutableMapOf<Int, Result>()
        var key = 0
        context.filesDir.listFiles()!!
            .filter { it.name !in listOf(".", "..") && it.isDirectory }
            .forEach {
                val isResultReady = Result.getDataFile(it).exists()
                val titleReader = FileReader(Result.getTitleFile(it))
                val title = titleReader.readText()
                titleReader.close()

                val mediaType = MediaType.fromDirName(it.name)
                val timestamp = it.name.split(".")[0].toLong()

                results[key++] = Result(
                    resultDir = it,
                    mediaType = mediaType,
                    title = title,
                    uploadStatus = if (isResultReady) 200 else -1,
                    timestamp = timestamp
                )
            }
        _uiState.value = _uiState.value.copy(results = results, isInitialized = true)
    }

    fun changeResultSelection(resultKey: Int, selectionState: Boolean) {
        val resultMap = _uiState.value.results.toMutableMap()
        resultMap[resultKey] = resultMap[resultKey]!!.copy(isSelected = selectionState)
        _uiState.value = _uiState.value.copy(results = resultMap)
    }

    fun deleteSelectedResults() {
        val resultMap = _uiState.value.results.toMutableMap()
        resultMap.toList().filter { it.second.isSelected }.forEach { (key, result) ->
            Result.getDataFile(result.resultDir).delete()
            Result.getMediaFile(result.resultDir).delete()
            Result.getTitleFile(result.resultDir).delete()
            result.resultDir.delete()
            resultMap.remove(key)
        }
        _uiState.value = _uiState.value.copy(results = resultMap)
    }
}