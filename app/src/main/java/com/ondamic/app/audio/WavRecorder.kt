package com.ondamic.app.audio

import android.content.Context
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SavedRecordingItem(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val durationSec: Int,
    val createdAtMillis: Long,
    val isStereo: Boolean
)

/**
 * Writes standard RIFF/WAVE 16-bit PCM files locally in Mono or Stereo.
 * 100% offline storage inside app-specific `filesDir/recordings`.
 */
class WavRecorder(private val context: Context) {

    private var raf: RandomAccessFile? = null
    private var currentFile: File? = null
    private var dataBytesWritten: Long = 0L
    private var recordingSampleRate: Int = 48000
    private var recordingChannels: Int = 2
    private var startTimeMillis: Long = 0L
    private val pcmScratchBuffer = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN)

    val isRecording: Boolean
        @Synchronized get() = raf != null

    val elapsedSeconds: Int
        @Synchronized get() = if (raf != null && startTimeMillis > 0L) {
            ((System.currentTimeMillis() - startTimeMillis) / 1000L).toInt()
        } else 0

    @Synchronized
    fun startRecording(sampleRate: Int, channels: Int): File? {
        stopRecording()
        return runCatching {
            val dir = File(context.filesDir, "recordings").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val modeSuffix = if (channels >= 2) "STEREO" else "MONO"
            val outFile = File(dir, "OndaMic_${stamp}_$modeSuffix.wav")
            val access = RandomAccessFile(outFile, "rw")
            recordingSampleRate = sampleRate
            recordingChannels = channels.coerceIn(1, 2)
            dataBytesWritten = 0L
            startTimeMillis = System.currentTimeMillis()
            writeWavHeader(access, recordingSampleRate, recordingChannels, 0L)
            raf = access
            currentFile = outFile
            outFile
        }.getOrNull()
    }

    @Synchronized
    fun writeStereoFloatFrames(stereoFrames: FloatArray, numFrames: Int) {
        val activeRaf = raf ?: return
        runCatching {
            pcmScratchBuffer.clear()
            for (frame in 0 until numFrames) {
                val left = stereoFrames[frame * 2].coerceIn(-1.0f, 1.0f)
                val right = stereoFrames[frame * 2 + 1].coerceIn(-1.0f, 1.0f)
                if (recordingChannels == 1) {
                    val mono = ((left + right) * 0.5f * 32767.0f).toInt().toShort()
                    if (pcmScratchBuffer.remaining() < 2) {
                        activeRaf.write(pcmScratchBuffer.array(), 0, pcmScratchBuffer.position())
                        dataBytesWritten += pcmScratchBuffer.position()
                        pcmScratchBuffer.clear()
                    }
                    pcmScratchBuffer.putShort(mono)
                } else {
                    val sL = (left * 32767.0f).toInt().toShort()
                    val sR = (right * 32767.0f).toInt().toShort()
                    if (pcmScratchBuffer.remaining() < 4) {
                        activeRaf.write(pcmScratchBuffer.array(), 0, pcmScratchBuffer.position())
                        dataBytesWritten += pcmScratchBuffer.position()
                        pcmScratchBuffer.clear()
                    }
                    pcmScratchBuffer.putShort(sL)
                    pcmScratchBuffer.putShort(sR)
                }
            }
            if (pcmScratchBuffer.position() > 0) {
                activeRaf.write(pcmScratchBuffer.array(), 0, pcmScratchBuffer.position())
                dataBytesWritten += pcmScratchBuffer.position()
            }
        }
    }

    @Synchronized
    fun stopRecording(): File? {
        val activeRaf = raf ?: return null
        val finishedFile = currentFile
        runCatching {
            activeRaf.seek(0L)
            writeWavHeader(activeRaf, recordingSampleRate, recordingChannels, dataBytesWritten)
            activeRaf.close()
        }
        raf = null
        currentFile = null
        startTimeMillis = 0L
        return finishedFile
    }

    fun listRecordings(): List<SavedRecordingItem> {
        val dir = File(context.filesDir, "recordings")
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension.equals("wav", ignoreCase = true) }
            .orEmpty()
            .sortedByDescending { it.lastModified() }
            .map { file ->
                val isStereo = file.name.contains("STEREO", ignoreCase = true)
                val bytesPerSec = 48000 * (if (isStereo) 2 else 1) * 2
                val dur = ((file.length() - 44L).coerceAtLeast(0L) / bytesPerSec).toInt()
                SavedRecordingItem(
                    file = file,
                    name = file.name,
                    sizeBytes = file.length(),
                    durationSec = dur,
                    createdAtMillis = file.lastModified(),
                    isStereo = isStereo
                )
            }
    }

    fun deleteRecording(file: File): Boolean {
        return runCatching { file.delete() }.getOrDefault(false)
    }

    private fun writeWavHeader(
        file: RandomAccessFile,
        sampleRate: Int,
        channels: Int,
        totalAudioLen: Long
    ) {
        val totalDataLen = totalAudioLen + 36L
        val byteRate = sampleRate * channels * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(totalDataLen.toInt())
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16) // Subchunk1Size for PCM
        header.putShort(1.toShort()) // AudioFormat 1 = PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort((channels * 2).toShort()) // BlockAlign
        header.putShort(16.toShort()) // BitsPerSample
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(totalAudioLen.toInt())
        file.write(header.array())
    }
}
