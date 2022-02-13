package com.demkom58.qoi_kotlin

import com.demkom58.qoi_kotlin.QoiDecoder.decode
import com.demkom58.qoi_kotlin.QoiEncoder.encode
import java.io.*

object QoiUtil {

    fun create(
        data: ByteArray, width: Int, height: Int,
        channels: Int = data.size / width / height,
        colorSpace: QoiColorSpace = QoiColorSpace.SRGB
    ): QoiImage {
        require(width > 0) {
            "Width must larger than zero"
        }

        require(height > 0) {
            "Height must larger than zero"
        }

        require(channels == 3 || channels == 4) {
            "Only 3 and 4 channels image supported"
        }

        require(data.size == width * height * channels) {
            "Pixels number not corresponds to received channels, width, height."
        }

        return QoiImage(width, height, channels, colorSpace, data)
    }

    fun from(input: InputStream, channels: Int = 0): QoiImage {
        return decode(input, channels)
    }

    fun from(file: File, channels: Int = 0): QoiImage {
        FileInputStream(file).use { input -> return from(input, channels) }
    }

    fun write(image: QoiImage, out: OutputStream) {
        encode(image, out)
    }

    fun write(image: QoiImage, file: File) {
        FileOutputStream(file).use { out -> write(image, out) }
    }

}