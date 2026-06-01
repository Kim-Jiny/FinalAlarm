package app.finalalarm.ui.missions

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Average Hash (aHash) - 8x8 64-bit perceptual hash.
 * 비슷한 이미지는 hamming distance가 작음. 임계값 10 정도가 권장.
 */
object ImageHash {
    private const val SIZE = 8

    fun aHash(bitmap: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(bitmap, SIZE, SIZE, true)
        val pixels = IntArray(SIZE * SIZE)
        scaled.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE)
        val grays = IntArray(SIZE * SIZE) { i ->
            val p = pixels[i]
            (Color.red(p) + Color.green(p) + Color.blue(p)) / 3
        }
        val avg = grays.sum().toDouble() / grays.size
        var hash = 0L
        for (i in grays.indices) {
            if (grays[i] > avg) hash = hash or (1L shl i)
        }
        return hash
    }

    fun hamming(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

    fun toHex(hash: Long): String = "%016x".format(hash)

    fun fromHex(s: String): Long? = runCatching { java.lang.Long.parseUnsignedLong(s, 16) }.getOrNull()

    const val MATCH_THRESHOLD = 12  // 0~64 사이. 작을수록 엄격.
}
