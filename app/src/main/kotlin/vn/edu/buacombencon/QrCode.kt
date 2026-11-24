package vn.edu.buacombencon

/** Version 1, byte mode, error correction L, fixed mask 0. Payload <=17 ASCII bytes. */
class QrCode(payload: String) {
    val modules = Array(21) { BooleanArray(21) }
    private val function = Array(21) { BooleanArray(21) }

    init {
        val raw = payload.toByteArray(Charsets.US_ASCII)
        require(raw.size <= 17) { "QR payload too long" }
        val data = IntArray(26)
        val bits = BooleanArray(152)
        var position = 0
        for (bit in intArrayOf(0, 1, 0, 0)) bits[position++] = bit != 0
        for (bit in 7 downTo 0) bits[position++] = (raw.size ushr bit and 1) != 0
        for (byte in raw) {
            for (bit in 7 downTo 0) bits[position++] = (byte.toInt() ushr bit and 1) != 0
        }
        position = (minOf(bits.size, position + 4) + 7) / 8 * 8
        for (index in 0 until position) {
            if (bits[index]) data[index / 8] = data[index / 8] or (1 shl (7 - index % 8))
        }
        for (index in position / 8 until 19) {
            data[index] = if ((index - position / 8) % 2 == 0) 0xec else 0x11
        }

        val divisor = IntArray(7)
        divisor[6] = 1
        var root = 1
        repeat(7) {
            for (index in divisor.indices) {
                divisor[index] = multiply(divisor[index], root)
                if (index + 1 < divisor.size) divisor[index] = divisor[index] xor divisor[index + 1]
            }
            root = multiply(root, 2)
        }

        val remainder = IntArray(7)
        for (index in 0 until 19) {
            val factor = data[index] xor remainder[0]
            remainder.copyInto(remainder, startIndex = 1)
            remainder[6] = 0
            for (coefficient in remainder.indices) {
                remainder[coefficient] = remainder[coefficient] xor multiply(divisor[coefficient], factor)
            }
        }
        remainder.copyInto(data, 19)

        for (index in 0 until 21) {
            put(6, index, index % 2 == 0)
            put(index, 6, index % 2 == 0)
        }
        finder(3, 3)
        finder(17, 3)
        finder(3, 17)

        val format = 0x77c4
        for (index in 0..5) put(8, index, (format ushr index and 1) != 0)
        put(8, 7, (format ushr 6 and 1) != 0)
        put(8, 8, (format ushr 7 and 1) != 0)
        put(7, 8, (format ushr 8 and 1) != 0)
        for (index in 9 until 15) put(14 - index, 8, (format ushr index and 1) != 0)
        for (index in 0 until 8) put(20 - index, 8, (format ushr index and 1) != 0)
        for (index in 8 until 15) put(8, 6 + index, (format ushr index and 1) != 0)
        put(8, 13, true)

        var bit = 0
        var right = 20
        while (right >= 1) {
            if (right == 6) right = 5
            for (row in 0 until 21) {
                for (offset in 0..1) {
                    val x = right - offset
                    val y = if ((right + 1 and 2) == 0) 20 - row else row
                    if (!function[y][x]) {
                        val dark = bit < 208 && (data[bit / 8] ushr (7 - bit % 8) and 1) != 0
                        bit++
                        modules[y][x] = dark xor ((x + y) % 2 == 0)
                    }
                }
            }
            right -= 2
        }
    }

    private fun put(x: Int, y: Int, dark: Boolean) {
        modules[y][x] = dark
        function[y][x] = true
    }

    private fun finder(centerX: Int, centerY: Int) {
        for (dy in -4..4) {
            for (dx in -4..4) {
                val x = centerX + dx
                val y = centerY + dy
                if (x in 0 until 21 && y in 0 until 21) {
                    val distance = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                    put(x, y, distance != 2 && distance != 4)
                }
            }
        }
    }

    private fun multiply(a: Int, b: Int): Int {
        var result = 0
        for (bit in 7 downTo 0) {
            result = (result shl 1) xor ((result ushr 7) * 0x11d)
            result = result xor ((b ushr bit and 1) * a)
        }
        return result
    }
}
