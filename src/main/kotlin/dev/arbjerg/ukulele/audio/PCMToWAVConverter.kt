package dev.arbjerg.ukulele.audio
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object PCMToWAVConverter {

    const val SAMPLE_RATE = 48000 // 48 kHz
    const val BITS_PER_SAMPLE = 16 // 16-bit audio
    const val CHANNELS = 2 // Stereo
    const val HEADER_SIZE = 44

    @Throws(Exception::class)
    fun writeWavFile(pcmData: ByteArray, outputPath: String) {
        val byteRate = SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8)
        val dataSize = pcmData.size
        val fileSize = HEADER_SIZE + dataSize - 8

        FileOutputStream(File(outputPath)).use { fos ->
            // Write WAV header
            fos.write(createWavHeader(dataSize, fileSize, byteRate))
            // Write the PCM data
            fos.write(pcmData)
        }
    }

    private fun createWavHeader(dataSize: Int, fileSize: Int, byteRate: Int): ByteArray {
        val buffer = ByteBuffer.allocate(HEADER_SIZE)
        buffer.order(ByteOrder.LITTLE_ENDIAN) // WAV format uses little-endian byte order

        // RIFF chunk descriptor
        buffer.put("RIFF".toByteArray()) // Chunk ID
        buffer.putInt(36 + fileSize) // Chunk Size
        buffer.put("WAVE".toByteArray()) // Format

        // "fmt " sub-chunk
        buffer.put("fmt ".toByteArray()) // Subchunk1 ID
        buffer.putInt(16) // Subchunk1 Size (16 for PCM)
        buffer.putShort(1.toShort()) // Audio format (1 = PCM)
        buffer.putShort(CHANNELS.toShort()) // Number of channels
        buffer.putInt(SAMPLE_RATE) // Sample rate
        buffer.putInt(byteRate) // Byte rate
        buffer.putShort((CHANNELS * (BITS_PER_SAMPLE / 8)).toShort()) // Block align
        buffer.putShort(BITS_PER_SAMPLE.toShort()) // Bits per sample

        // "data" sub-chunk
        buffer.put("data".toByteArray()) // Subchunk2 ID
        buffer.putInt(dataSize) // Subchunk2 Size

        return buffer.array()
    }
}
