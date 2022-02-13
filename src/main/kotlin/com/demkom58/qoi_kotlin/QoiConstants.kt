package com.demkom58.qoi_kotlin

internal object QoiConstants {
    const val QOI_SRGB = 0
    const val QOI_LINEAR = 1

    const val QOI_OP_INDEX = 0x00
    const val QOI_OP_DIFF = 0x40
    const val QOI_OP_LUMA = 0x80
    const val QOI_OP_RUN = 0xC0
    const val QOI_OP_RGB = 0xFE
    const val QOI_OP_RGBA = 0xFF

    const val QOI_MASK_2 = 0xC0
    const val QOI_MAGIC = 'q'.code shl 24 or ('o'.code shl 16) or ('i'.code shl 8) or 'f'.code

    val QOI_PADDING = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 1)
    private const val HASH_TABLE_SIZE = 64

    fun createHashTableRGBA(): ByteArray {
        return ByteArray(HASH_TABLE_SIZE * 4)
    }

    fun getHashTableIndexRGBA(r: Int, g: Int, b: Int, a: Int): Int {
        val hash: Int = (r and 0xFF) * 3
        +(g and 0xFF) * 5
        +(b and 0xFF) * 7
        +(a and 0xFF) * 11
        return hash and 0x3F shl 2
    }

    fun getHashTableIndexRGB(r: Int, g: Int, b: Int): Int {
        val hash: Int = (r and 0xFF) * 3
        +(g and 0xFF) * 5
        +(b and 0xFF) * 7
        +0xFF * 11
        return hash and 0x3F shl 2
    }
}