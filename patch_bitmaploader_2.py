import re

with open("app/src/main/java/com/example/MyBitmapLoader.kt", "r") as f:
    content = f.read()

target = """    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                com.example.LogKeeper.log("MyBitmapLoader: Loading bitmap for uri: $uri", "MyBitmapLoader")
                val req = ImageRequest.Builder(context).data(uri).size(512).videoFrameMillis(0).build()
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
                com.example.LogKeeper.logError("MyBitmapLoader", "Exception loading bitmap", e)
                future.setException(e) 
            }
        }
        return future
    }"""

replacement = """    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                com.example.LogKeeper.log("MyBitmapLoader: Loading bitmap for uri: $uri", "MyBitmapLoader")
                kotlinx.coroutines.withTimeout(2000L) {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    val bitmap = retriever.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    retriever.release()
                    
                    if (bitmap != null) {
                        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 512, (512.toFloat() / bitmap.width * bitmap.height).toInt(), true)
                        com.example.LogKeeper.log("MyBitmapLoader: Success", "MyBitmapLoader")
                        future.set(scaledBitmap)
                    } else {
                        com.example.LogKeeper.logError("MyBitmapLoader", "Bitmap is null", Exception("Null bitmap"))
                        future.setException(Exception("Null bitmap"))
                    }
                }
            } catch(e: Exception) { 
                com.example.LogKeeper.logError("MyBitmapLoader", "Exception loading bitmap: ${e.message}", e)
                future.setException(e) 
            }
        }
        return future
    }"""

content = content.replace(target, replacement)
with open("app/src/main/java/com/example/MyBitmapLoader.kt", "w") as f:
    f.write(content)
