package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.videoFrameMillis

class MyBitmapLoader(val context: Context) : BitmapLoader {
    override fun supportsMimeType(mimeType: String) = true
    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        val bmp = android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size)
        if (bmp != null) future.set(bmp) else future.setException(Exception("err"))
        return future
    }
    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                com.example.LogKeeper.log("MyBitmapLoader: Loading bitmap for uri: $uri", "MyBitmapLoader")
                val req = ImageRequest.Builder(context).data(uri).size(512).build()
                val result = context.imageLoader.execute(req)
                val dr = result.drawable
                if (dr is android.graphics.drawable.BitmapDrawable) {
                    com.example.LogKeeper.log("MyBitmapLoader: Success", "MyBitmapLoader")
                    future.set(dr.bitmap)
                } else {
                    com.example.LogKeeper.logError("MyBitmapLoader", "Result is not BitmapDrawable: ${dr?.javaClass?.name}", Exception("Not BitmapDrawable"))
                    future.setException(Exception("Not BitmapDrawable: ${dr?.javaClass?.name}"))
                }
            } catch(e: Exception) { 
                com.example.LogKeeper.logError("MyBitmapLoader", "Exception loading bitmap: ${e.message}", e)
                future.setException(e) 
            }
        }
        return future
    }
}
