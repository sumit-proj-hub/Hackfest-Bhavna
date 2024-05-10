package com.example.bhavna

import android.os.Handler
import android.os.Looper
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

interface UploadClient {
    @POST("/upload")
    @Multipart
    suspend fun upload(
        @Part fileType: MultipartBody.Part,
        @Part file: MultipartBody.Part,
    ): Response<ResponseBody>

    companion object {
        private val okHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(0, TimeUnit.MILLISECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build()
        }


        val instance: UploadClient by lazy {
            Retrofit.Builder()
                .baseUrl("http://192.168.203.26:5000")
                .client(okHttpClient)
                .build()
                .create(UploadClient::class.java)
        }
    }
}

fun interface ProgressCallback {
    fun onProgress(bytesUploaded: Long, totalBytes: Long)
}

internal class ProgressRequestBody(
    private val delegate: RequestBody,
    private val callback: ProgressCallback,
) : RequestBody() {
    override fun contentType(): MediaType? = delegate.contentType()
    override fun contentLength(): Long = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val countingSink = CountingSink(sink).buffer()
        delegate.writeTo(countingSink)
        countingSink.flush()
    }

    private inner class CountingSink(delegate: Sink) : ForwardingSink(delegate) {
        private val handler = Handler(Looper.getMainLooper())
        private val total = contentLength()
        private var uploaded = 0L

        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            uploaded += byteCount

            handler.post { callback.onProgress(uploaded, total) }
        }
    }
}
