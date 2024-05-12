package com.example.bhavna.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.bhavna.ui.Result
import com.example.bhavna.ui.ResultScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

const val delayBetweenFrames = 250

@Suppress("UNCHECKED_CAST")
class ResultScreenViewModelFactory(private val dirName: String) :
    ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ResultScreenViewModel(dirName) as T
}

class ResultScreenViewModel(private val dirName: String) : ViewModel() {
    private val _uiState = MutableStateFlow(ResultScreenState())
    val uiState = _uiState.asStateFlow()

    lateinit var player: ExoPlayer private set
    lateinit var mediaType: MediaType private set
    lateinit var mediaFile: File private set
    private lateinit var dataByteArray: ByteArray
    private var poll = true

    fun initialize(context: Context) {
        val resultDir = File(context.filesDir, dirName)
        val dataFile = Result.getDataFile(resultDir)
        dataByteArray = dataFile.readBytes()
        mediaType = MediaType.fromDirName(dirName)
        mediaFile = Result.getMediaFile(resultDir)
        if (mediaType == MediaType.Video) {
            player = ExoPlayer.Builder(context).build()
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(mediaFile)))
            player.prepare()

            val handler = Handler(Looper.getMainLooper())
            val runnable = object : Runnable {
                override fun run() {
                    _uiState.value = _uiState.value.copy(videoPosition = player.currentPosition)
                    if (poll)
                        handler.postDelayed(this, 50)
                }
            }
            handler.post(runnable)
        }

        _uiState.value = _uiState.value.copy(isInitialized = true)
    }

    private fun getFloatsFromByteArray(byteArray: ByteArray): FloatArray {
        val byteBuffer = ByteBuffer.wrap(byteArray).order(ByteOrder.BIG_ENDIAN)
        val floatArray = FloatArray(8)
        repeat(8) {
            floatArray[it] = byteBuffer.getFloat()
        }
        return floatArray
    }

    private fun getEmotionValues(floatArray: FloatArray): Map<String, Float> = mapOf(
        "Anger" to floatArray[0],
        "Contempt" to floatArray[1],
        "Disgust" to floatArray[2],
        "Fear" to floatArray[3],
        "Happiness" to floatArray[4],
        "Neutral" to floatArray[5],
        "Sadness" to floatArray[6],
        "Surprise" to floatArray[7],
    )

    private fun getOrderedEmotionValues(frameNumber: Int): FloatArray {
        val byteOffset = (frameNumber * 32)
        return if (byteOffset + 32 > dataByteArray.size) {
            getFloatsFromByteArray(
                dataByteArray.sliceArray(IntRange(dataByteArray.size - 32, dataByteArray.size - 1))
            )
        } else
            getFloatsFromByteArray(dataByteArray.sliceArray(IntRange(byteOffset, byteOffset + 31)))
    }

    fun getEmotionValues(time: Long): Map<String, Float> {
        val frameNumber = (time / delayBetweenFrames).toInt()
        val currFrameEmotion = getOrderedEmotionValues(frameNumber)
        val nextFrameEmotion = getOrderedEmotionValues(frameNumber + 1)
        val interpolatedEmotion = currFrameEmotion.mapIndexed { index, fl ->
            fl + (nextFrameEmotion[index] - currFrameEmotion[index]) *
                    (time % delayBetweenFrames) / delayBetweenFrames
        }

        return getEmotionValues(interpolatedEmotion.toFloatArray())
    }

    override fun onCleared() {
        super.onCleared()
        if (this::player.isInitialized)
            player.release()
        poll = false
    }
}