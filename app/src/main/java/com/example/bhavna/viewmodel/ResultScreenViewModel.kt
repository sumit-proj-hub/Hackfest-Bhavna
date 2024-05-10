package com.example.bhavna.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.bhavna.ui.Result
import com.example.bhavna.ui.ResultScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

@Suppress("UNCHECKED_CAST")
class ResultScreenViewModelFactory(private val dirName: String) :
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ResultScreenViewModel(dirName) as T
}

class ResultScreenViewModel(private val dirName: String) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ResultScreenState(
            emotionValues = mapOf(
                "Happiness" to 20f,
                "Anger" to 50f,
                "Sadness" to 20f,
                "Surprise" to 100f,
                "Contempt" to 60f,
                "Disgust" to 25f,
                "Neutral" to 60f,
                "Fear" to 10f,
            ),
        )
    )
    val uiState = _uiState.asStateFlow()

    lateinit var player: ExoPlayer private set
    lateinit var mediaType: MediaType private set
    lateinit var mediaFile: File private set

    fun initialize(context: Context) {
        mediaType = MediaType.fromDirName(dirName)
        mediaFile = Result.getMediaFile(File(context.filesDir, dirName))
        if (mediaType == MediaType.Video) {
            player = ExoPlayer.Builder(context).build()
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(mediaFile)))
            player.prepare()
        }
        _uiState.value = _uiState.value.copy(isInitialized = true)
    }

    override fun onCleared() {
        super.onCleared()
        if (this::player.isInitialized)
            player.release()
        Log.d("Tag", "onCleared: ")
    }
}