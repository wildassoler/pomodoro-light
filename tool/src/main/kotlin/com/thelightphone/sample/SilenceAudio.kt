package com.thelightphone.sample

import java.io.File
import java.io.RandomAccessFile

// Generates (or reuses) a silent WAV file with the exact duration needed,
// used to keep a "detached" audio session alive for the length of a
// Focus/Break, surviving the screen turning off / process dying.
object SilenceAudio {

    private const val SAMPLE_RATE = 4000 // low rate is fine, it's silence
    private const val BITS_PER_SAMPLE = 16
    private const val CHANNELS = 1

    fun file(filesDir: File, durationSeconds: Int): File {
        val file = File(filesDir, "silence_${durationSeconds}s.wav")
        if (file.exists() && file.length() > 44) return file
        writeSilentWav(file, durationSeconds)
        return file
    }

    private fun writeSilentWav(file: File, durationSeconds: Int) {
        val byteRate = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8
        val dataSize = byteRate * durationSeconds

        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            raf.writeBytes("RIFF")
            raf.writeIntLE(36 + dataSize)
            raf.writeBytes("WAVE")
            raf.writeBytes("fmt ")
            raf.writeIntLE(16)
            raf.writeShortLE(1)
            raf.writeShortLE(CHANNELS)
            raf.writeIntLE(SAMPLE_RATE)
            raf.writeIntLE(byteRate)
            raf.writeShortLE(CHANNELS * BITS_PER_SAMPLE / 8)
            raf.writeShortLE(BITS_PER_SAMPLE)
            raf.writeBytes("data")
            raf.writeIntLE(dataSize)

            val chunk = ByteArray(8192)
            var written = 0
            while (written < dataSize) {
                val toWrite = minOf(chunk.size, dataSize - written)
                raf.write(chunk, 0, toWrite)
                written += toWrite
            }
        }
    }

    private fun RandomAccessFile.writeIntLE(value: Int) {
        write(byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte(),
        ))
    }

    private fun RandomAccessFile.writeShortLE(value: Int) {
        write(byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
        ))
    }
}