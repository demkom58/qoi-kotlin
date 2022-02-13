package com.demkom58.qoi_kotlin

import com.demkom58.qoi_kotlin.QoiConstants.QOI_LINEAR
import com.demkom58.qoi_kotlin.QoiConstants.QOI_MAGIC
import com.demkom58.qoi_kotlin.QoiConstants.QOI_OP_DIFF
import com.demkom58.qoi_kotlin.QoiConstants.QOI_OP_INDEX
import com.demkom58.qoi_kotlin.QoiConstants.QOI_OP_LUMA
import com.demkom58.qoi_kotlin.QoiConstants.QOI_OP_RGB
import com.demkom58.qoi_kotlin.QoiConstants.QOI_OP_RGBA
import com.demkom58.qoi_kotlin.QoiConstants.QOI_OP_RUN
import com.demkom58.qoi_kotlin.QoiConstants.QOI_PADDING
import com.demkom58.qoi_kotlin.QoiConstants.QOI_SRGB
import com.demkom58.qoi_kotlin.QoiConstants.createHashTableRGBA
import com.demkom58.qoi_kotlin.QoiConstants.getHashTableIndexRGB
import com.demkom58.qoi_kotlin.QoiConstants.getHashTableIndexRGBA
import java.io.OutputStream

object QoiEncoder {
    fun encode(image: QoiImage, outputStream: OutputStream) {
        val channels = image.channels
        val pixelData = image.data

        val writer = Writer(outputStream)
        writer.writeInt(QOI_MAGIC)
        writer.writeInt(image.width)
        writer.writeInt(image.height)
        writer.write(image.channels)
        writer.writeColorSpace(image.colorSpace)

        if (channels == 3) {
            encodeRGB(writer, pixelData)
        } else {
            encodeRGBA(writer, pixelData)
        }

        for (b in QOI_PADDING) {
            writer.write(b)
        }

        writer.flush()
    }

    private fun encodeRGB(out: Writer, pixelData: ByteArray) {
        val index = createHashTableRGBA()
        var run = 0

        var prevR = 0
        var prevG = 0
        var prevB = 0

        var r: Int
        var g: Int
        var b: Int

        var pixelPos = 0
        while (pixelPos < pixelData.size) {
            r = pixelData[pixelPos].toInt()
            g = pixelData[pixelPos + 1].toInt()
            b = pixelData[pixelPos + 2].toInt()
            if (eq(prevR, prevG, prevB, r, g, b)) {
                run++
                if (run == 62) {
                    out.write(QOI_OP_RUN or run - 1)
                    run = 0
                }
            } else {
                if (run > 0) {
                    out.write(QOI_OP_RUN or run - 1)
                    run = 0
                }

                val idxPos = getHashTableIndexRGB(r, g, b)
                if (eq(r, g, b, 0xFF,
                        index[idxPos].toInt(), index[idxPos + 1].toInt(), index[idxPos + 2].toInt(), index[idxPos + 3].toInt())) {
                    out.write(QOI_OP_INDEX or idxPos / 4)
                } else {
                    index[idxPos] = r.toByte()
                    index[idxPos + 1] = g.toByte()
                    index[idxPos + 2] = b.toByte()
                    index[idxPos + 3] = 0xFF.toByte()
                    writeRGB(r, prevR, g, prevG, b, prevB, out)
                }

                prevR = r
                prevG = g
                prevB = b
            }
            pixelPos += 3
        }
        if (run > 0) {
            out.write(QOI_OP_RUN or run - 1)
        }
    }

    private fun encodeRGBA(out: Writer, pixelData: ByteArray) {
        val ht = createHashTableRGBA()
        var run = 0

        var prevR = 0
        var prevG = 0
        var prevB = 0
        var prevA = 0xFF

        var r: Int
        var g: Int
        var b: Int
        var a: Int

        var pixelPos = 0

        while (pixelPos < pixelData.size) {
            r = pixelData[pixelPos].toInt()
            g = pixelData[pixelPos + 1].toInt()
            b = pixelData[pixelPos + 2].toInt()
            a = pixelData[pixelPos + 3].toInt()

            if (eq(prevR, prevG, prevB, prevA, r, g, b, a)) {
                run++
                if (run == 62) {
                    out.write(QOI_OP_RUN or run - 1)
                    run = 0
                }
            } else {
                if (run > 0) {
                    out.write(QOI_OP_RUN or run - 1)
                    run = 0
                }
                val idx = getHashTableIndexRGBA(r, g, b, a)
                if (eq(r, g, b, a, ht[idx].toInt(), ht[idx + 1].toInt(), ht[idx + 2].toInt(), ht[idx + 3].toInt())) {
                    out.write(QOI_OP_INDEX or idx / 4)
                } else {
                    ht[idx] = r.toByte()
                    ht[idx + 1] = g.toByte()
                    ht[idx + 2] = b.toByte()
                    ht[idx + 3] = a.toByte()

                    if (prevA == a) {
                        writeRGB(r, prevR, g, prevG, b, prevB, out)
                    } else {
                        out.write(QOI_OP_RGBA)
                        out.write(r.toByte(), g.toByte(), b.toByte(), a.toByte())
                    }
                }
                prevR = r
                prevG = g
                prevB = b
                prevA = a
            }
            pixelPos += 4
        }
        if (run > 0) {
            out.write(QOI_OP_RUN or run - 1)
        }
    }

    private fun writeRGB(
        pixelR: Int, prevR: Int,
        pixelG: Int, prevG: Int,
        pixelB: Int, prevB: Int,
        out: Writer) {
        val diffR = pixelR - prevR
        val diffG = pixelG - prevG
        val diffB = pixelB - prevB

        if (checkDiff(diffR) && checkDiff(diffG) && checkDiff(diffB)) {
            out.write(QOI_OP_DIFF or (diffR + 2 shl 4) or (diffG + 2 shl 2) or diffB + 2)
            return
        }

        val diffGR = diffR - diffG
        val diffGB = diffB - diffG

        if (smallerDiff(diffGR) && smallDiff(diffG) && smallerDiff(diffGB)) {
            out.write(QOI_OP_LUMA or diffG + 32)
            out.write(diffGR + 8 shl 4 or diffGB + 8)
        } else {
            out.write(QOI_OP_RGB)
            out.write(pixelR.toByte(), pixelG.toByte(), pixelB.toByte())
        }
    }

    private fun smallDiff(i: Int): Boolean {
        return i > -33 && i < 32
    }

    private fun smallerDiff(i: Int): Boolean {
        return i > -9 && i < 8
    }

    private fun checkDiff(i: Int): Boolean {
        return i > -3 && i < 2
    }

    private fun eq(r1: Int, g1: Int, b1: Int, a1: Int,
                   r2: Int, g2: Int, b2: Int, a2: Int): Boolean {
        return r1 == r2 && g1 == g2 && b1 == b2 && a1 == a2
    }

    private fun eq(r1: Int, g1: Int, b1: Int,
                   r2: Int, g2: Int, b2: Int): Boolean {
        return r1 == r2 && g1 == g2 && b1 == b2
    }

    private class Writer(private val output: OutputStream) {

        companion object {
            private const val BUFFER_SIZE = 8192
        }

        private val buffer = ByteArray(BUFFER_SIZE)
        private var written = 0

        fun write(value: Byte) {
            if (written == BUFFER_SIZE) {
                doFlush()
            }
            buffer[written++] = value
        }

        fun write(value: Int) {
            write(value.toByte())
        }

        fun write(a: Byte, b: Byte, c: Byte) {
            if (written > BUFFER_SIZE - 3) {
                doFlush()
            }
            buffer[written] = a
            buffer[written + 1] = b
            buffer[written + 2] = c
            written += 3
        }

        fun write(a: Byte, b: Byte, c: Byte, d: Byte) {
            if (written > BUFFER_SIZE - 4) {
                doFlush()
            }
            buffer[written] = a
            buffer[written + 1] = b
            buffer[written + 2] = c
            buffer[written + 3] = d
            written += 4
        }

        fun writeColorSpace(colorSpace: QoiColorSpace) {
            when (colorSpace) {
                QoiColorSpace.SRGB -> write(QOI_SRGB)
                QoiColorSpace.LINEAR -> write(QOI_LINEAR)
            }
        }

        fun writeInt(value: Int) {
            write(value shr 24)
            write(value shr 16)
            write(value shr 8)
            write(value)
        }

        fun flush() {
            if (written == 0) {
                return
            }
            doFlush()
        }

        private fun doFlush() {
            output.write(buffer, 0, written)
            written = 0
        }
    }
}