package com.demkom58.qoi_kotlin

class QoiImage(
    val width: Int,
    val height: Int,
    val channels: Int,
    val colorSpace: QoiColorSpace,
    val data: ByteArray
)