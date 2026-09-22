package com.dettle.app.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.dettle.app.data.settings.ApiKeyStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class VoiceResourceProfile(
    val engineName: String,
    val modelSizeMb: Int,
    val activeRamMb: String,
    val latencyEstimate: String,
    val batteryImpact: String,
    val computeMode: String
)

@Singleton
class OpenWhisprClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiKeyStore: ApiKeyStore
) {
    companion object {
        private const val TAG = "OpenWhisprClient"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var outputFile: File? = null

    /**
     * Pre-calculated resource consumption profiles for various Whisper/speech engines.
     * Grounded in OpenWhispr benchmarks.
     */
    fun getResourceProfiles(): List<VoiceResourceProfile> = listOf(
        VoiceResourceProfile(
            engineName = "Android On-Device DSP (Default)",
            modelSizeMb = 0,
            activeRamMb = "15 - 25 MB",
            latencyEstimate = "< 100ms (real-time stream)",
            batteryImpact = "Minimal (< 0.3% / 10m)",
            computeMode = "Hardware DSP / NPU"
        ),
        VoiceResourceProfile(
            engineName = "Whisper Tiny (Local)",
            modelSizeMb = 75,
            activeRamMb = "250 - 350 MB",
            latencyEstimate = "0.8s - 1.5s per chunk",
            batteryImpact = "Low (~1.5% / 10m)",
            computeMode = "ARM CPU (4 cores)"
        ),
        VoiceResourceProfile(
            engineName = "Whisper Base (Local)",
            modelSizeMb = 142,
            activeRamMb = "450 - 650 MB",
            latencyEstimate = "2.2s - 4.0s per chunk",
            batteryImpact = "Moderate (~3.0% / 10m)",
            computeMode = "ARM CPU (All cores)"
        ),
        VoiceResourceProfile(
            engineName = "Whisper Small (Local)",
            modelSizeMb = 466,
            activeRamMb = "1,200 - 1,800 MB",
            latencyEstimate = "8.0s - 15.0s (laggy)",
            batteryImpact = "High (~6.0% / 10m, warm)",
            computeMode = "Heavy CPU / Thermal throttling"
        ),
        VoiceResourceProfile(
            engineName = "OpenWhispr LAN Server",
            modelSizeMb = 0,
            activeRamMb = "5 MB (Offloaded to PC/Mac)",
            latencyEstimate = "300ms - 800ms",
            batteryImpact = "Negligible",
            computeMode = "LAN Host GPU / Metal"
        )
    )

    fun startRecording(): Result<File> = runCatching {
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        val tempFile = File(context.cacheDir, "whisper_temp_${System.currentTimeMillis()}.wav")
        outputFile = tempFile

        audioRecord?.startRecording()
        isRecording = true

        Thread {
            writeAudioDataToFile(tempFile, bufferSize)
        }.start()

        tempFile
    }

    fun stopRecording(): File? {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping audio recorder: ${e.message}")
        } finally {
            audioRecord = null
        }
        return outputFile
    }

    suspend fun transcribeAudio(audioFile: File, endpointUrl: String = "http://10.0.2.2:8080/v1/audio/transcriptions"): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        audioFile.name,
                        audioFile.asRequestBody("audio/wav".toMediaType())
                    )
                    .addFormDataPart("model", "whisper-1")
                    .addFormDataPart("response_format", "json")
                    .build()

                val request = Request.Builder()
                    .url(endpointUrl)
                    .post(requestBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Transcription failed: HTTP ${response.code} ${response.message}")
                    }
                    val jsonStr = response.body?.string() ?: throw IllegalStateException("Empty response body")
                    val jsonObj = JSONObject(jsonStr)
                    jsonObj.optString("text", "")
                }
            }
        }

    private fun writeAudioDataToFile(file: File, bufferSize: Int) {
        val data = ByteArray(bufferSize)
        FileOutputStream(file).use { outputStream ->
            // Placeholder for 44-byte WAV header
            outputStream.write(ByteArray(44))

            var totalBytesRead = 0L
            while (isRecording) {
                val read = audioRecord?.read(data, 0, bufferSize) ?: -1
                if (read > 0) {
                    outputStream.write(data, 0, read)
                    totalBytesRead += read
                }
            }

            // Rewind and write valid WAV header with correct lengths
            writeWavHeader(file, totalBytesRead)
        }
    }

    private fun writeWavHeader(file: File, totalAudioLen: Long) {
        val totalDataLen = totalAudioLen + 36
        val longSampleRate = SAMPLE_RATE.toLong()
        val channels = 1
        val byteRate = 16 * SAMPLE_RATE * channels / 8

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xffL).toByte()
        header[5] = ((totalDataLen shr 8) and 0xffL).toByte()
        header[6] = ((totalDataLen shr 16) and 0xffL).toByte()
        header[7] = ((totalDataLen shr 24) and 0xffL).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 16 for PCM
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // PCM format
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xffL).toByte()
        header[25] = ((longSampleRate shr 8) and 0xffL).toByte()
        header[26] = ((longSampleRate shr 16) and 0xffL).toByte()
        header[27] = ((longSampleRate shr 24) and 0xffL).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 16 / 8).toByte() // block align
        header[33] = 0
        header[34] = 16 // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xffL).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xffL).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xffL).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xffL).toByte()

        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(0)
            raf.write(header)
        }
    }
}
