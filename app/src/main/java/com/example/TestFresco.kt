package com.example
import com.facebook.animated.webp.WebPImage
fun test(bytes: ByteArray) {
    val img = WebPImage.createFromByteArray(bytes, com.facebook.imagepipeline.common.ImageDecodeOptions.defaults())
    val fc = img.frameCount
}
