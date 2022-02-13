package com.demkom58.qoi_kotlin

import com.demkom58.qoi_kotlin.QoiConstants.QOI_LINEAR
import com.demkom58.qoi_kotlin.QoiConstants.QOI_MAGIC
import com.demkom58.qoi_kotlin.QoiConstants.QOI_MASK_2
import com.demkom58.qoi_kotlin.QoiConstants.QOI_OP_DIFF
import com.demkom58.qoi_kotlin.QoiConstants.QOI_OP_INDEX
import com.demkom58.qoi_kotlin.QoiConstants.QOI_OP_LUMA
import com.demkom58.qoi_kotlin.QoiConstants.QOI_OP_RGB
import com.demkom58.qoi_kotlin.QoiConstants.QOI_OP_RGBA
import com.demkom58.qoi_kotlin.QoiConstants.QOI_OP_RUN
import com.demkom58.qoi_kotlin.QoiConstants.QOI_PADDING
import com.demkom58.qoi_kotlin.QoiConstants.QOI_SRGB
import com.demkom58.qoi_kotlin.QoiConstants.createHashTableRGBA
import com.demkom58.qoi_kotlin.QoiConstants.getHashTableIndexRGBA
import java.io.InputStream
import java.io.StreamCorruptedException

object QoiDecoder {
    private const val BUFFER_SIZE = 8192

    fun decode(inputStream: InputStream, channels: Int, doNotTouchDataAfterImage: Boolean = false): QoiImage {
        var ch = channels
        require(ch == 0 || ch == 3 || ch == 4) { "Invalid channel count, must be 0, 3 or 4" }

        // This custom buffering class is faster than BufferedInputStream and allows for controlled buffering
        val input = Reader(inputStream, !doNotTouchDataAfterImage)
        val headerMagic = input.int()
        require(headerMagic == QOI_MAGIC) { "Invalid magic value, is it QOI format image?" }

        val width = input.int()
        require(width > 0) { "Image width $width is less than 1" }

        val height = input.int()
        require(height > 0) { "Image height $height is less than 1" }

        val readChannels = input.read().toInt() and 0xFF
        require(readChannels == 3 || readChannels == 4) {
            "Only 3 and 4 channel images are supported, by detected ${readChannels}!"
        }

        if (ch == 0) {
            ch = readChannels
        }
        val colorSpace = input.colorSpace()

        val pixelData =
            if (ch == 3)
                readRGB(input, width, height)
            else
                readRGBA(input, width, height)

        for (i in 0..7) {
            if (QOI_PADDING[i] != input.skipBuffer()) {
                throw StreamCorruptedException("Invalid padding")
            }
        }

        return QoiImage(width, height, ch, colorSpace, pixelData)
    }

    private fun readRGB(input: Reader, width: Int, height: Int): ByteArray {
        val bytesLength = Math.multiplyExact(Math.multiplyExact(width, height), 3)
        val data = ByteArray(bytesLength)
        val index = createHashTableRGBA()

        var r = 0
        var g = 0
        var b = 0
        var a = 0xFF

        var pixIdx = 0
        while (pixIdx < bytesLength) {
            when (val b1: Int = input.read().toInt() and 0xFF) {
                QOI_OP_RGB -> {
                    r = input.read().toInt()
                    g = input.read().toInt()
                    b = input.read().toInt()
                }
                QOI_OP_RGBA -> {
                    r = input.read().toInt()
                    g = input.read().toInt()
                    b = input.read().toInt()
                    a = input.read().toInt()
                }
                else -> {
                    when (b1 and QOI_MASK_2) {
                        QOI_OP_INDEX -> {
                            val idxPos = b1 and QOI_MASK_2.inv() shl 2
                            r = index[idxPos].toInt()
                            g = index[idxPos + 1].toInt()
                            b = index[idxPos + 2].toInt()
                            a = index[idxPos + 3].toInt()
                        }
                        QOI_OP_DIFF -> {
                            r += (b1 shr 4 and 0x03) - 2
                            g += (b1 shr 2 and 0x03) - 2
                            b += (b1 and 0x03) - 2
                        }
                        QOI_OP_LUMA -> {
                            val b2 = input.read().toInt()
                            val vg = (b1 and 0x3F) - 32
                            r += vg - 8 + (b2 shr 4 and 0x0F)
                            g += vg
                            b += vg - 8 + (b2 and 0x0F)
                        }
                        QOI_OP_RUN -> {
                            val run = b1 and 0x3F
                            var i = 0
                            while (i < run) {
                                data[pixIdx] = r.toByte()
                                data[pixIdx + 1] = g.toByte()
                                data[pixIdx + 2] = b.toByte()
                                pixIdx += 3
                                i++
                            }
                        }
                    }
                }
            }

            val idxPos = getHashTableIndexRGBA(r, g, b, a)
            index[idxPos] = r.toByte()
            index[idxPos + 1] = g.toByte()
            index[idxPos + 2] = b.toByte()
            index[idxPos + 3] = a.toByte()

            data[pixIdx] = r.toByte()
            data[pixIdx + 1] = g.toByte()
            data[pixIdx + 2] = b.toByte()

            pixIdx += 3
        }
        return data
    }

    private fun readRGBA(input: Reader, width: Int, height: Int): ByteArray {
        val bytesLength = Math.multiplyExact(Math.multiplyExact(width, height), 4)
        val data = ByteArray(bytesLength)
        val index = createHashTableRGBA()

        var r = 0
        var g = 0
        var b = 0
        var a = 0xFF

        var pixPos = 0
        while (pixPos < bytesLength) {
            when (val b1: Int = input.read().toInt() and 0xFF) {
                QOI_OP_RGB -> {
                    r = input.read().toInt()
                    g = input.read().toInt()
                    b = input.read().toInt()
                }
                QOI_OP_RGBA -> {
                    r = input.read().toInt()
                    g = input.read().toInt()
                    b = input.read().toInt()
                    a = input.read().toInt()
                }
                else -> {
                    when (b1 and QOI_MASK_2) {
                        QOI_OP_INDEX -> {
                            val idxPos = b1 and QOI_MASK_2.inv() shl 2
                            r = index[idxPos].toInt()
                            g = index[idxPos + 1].toInt()
                            b = index[idxPos + 2].toInt()
                            a = index[idxPos + 3].toInt()
                        }
                        QOI_OP_DIFF -> {
                            r += ((b1 shr 4 and 0x03) - 2).toByte()
                            g += ((b1 shr 2 and 0x03) - 2)
                            b += ((b1 and 0x03) - 2)
                        }
                        QOI_OP_LUMA -> {
                            val b2 = input.read().toInt()
                            val vg = (b1 and 0x3F) - 32
                            r += vg - 8 + (b2 shr 4 and 0x0F)
                            g += vg
                            b += vg - 8 + (b2 and 0x0F)
                        }
                        QOI_OP_RUN -> {
                            val run = b1 and 0x3F
                            var i = 0
                            while (i < run) {
                                data[pixPos] = r.toByte()
                                data[pixPos + 1] = g.toByte()
                                data[pixPos + 2] = b.toByte()
                                data[pixPos + 3] = a.toByte()
                                pixPos += 4
                                i++
                            }
                        }
                    }
                }
            }

            val idxPos = getHashTableIndexRGBA(r, g, b, a)

            index[idxPos] = r.toByte()
            index[idxPos + 1] = g.toByte()
            index[idxPos + 2] = b.toByte()
            index[idxPos + 3] = a.toByte()

            data[pixPos] = r.toByte()
            data[pixPos + 1] = g.toByte()
            data[pixPos + 2] = b.toByte()
            data[pixPos + 3] = a.toByte()

            pixPos += 4
        }
        return data
    }

    private class Reader(private val input: InputStream, useBuffer: Boolean) {
        private val buffer: ByteArray?
        private var position = 0
        private var read = 0

        init {
            buffer = if (useBuffer) ByteArray(BUFFER_SIZE) else null
        }

        fun read(): Byte {
            if (buffer == null) {
                return byte()
            }

            if (position == read) {
                read = input.read(buffer)
                require(read != -1) { "Unexpected end of stream" }
                position = 0
            }

            return buffer[position++]
        }

        fun skipBuffer(): Byte {
            if (buffer == null) {
                return byte()
            }

            return if (position == read) {
                byte()
            } else buffer[position++]
        }

        private fun byte(): Byte {
            val read = input.read()
            require(read != -1) { "Unexpected end of stream" }
            return read.toByte()
        }

        fun int(): Int {
            val a: Int = read().toInt() and 0xFF
            val b: Int = read().toInt() and 0xFF
            val c: Int = read().toInt() and 0xFF
            val d: Int = read().toInt() and 0xFF
            return a shl 24 or (b shl 16) or (c shl 8) or d
        }

        fun colorSpace(): QoiColorSpace {
            val value: Int = read().toInt() and 0xFF
            when (value) {
                QOI_SRGB -> return QoiColorSpace.SRGB
                QOI_LINEAR -> return QoiColorSpace.LINEAR
            }

            throw StreamCorruptedException("Invalid color space value $value")
        }
    }
}