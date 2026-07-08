with open("app/src/main/java/com/example/MyBitmapLoader.kt", "r") as f:
    content = f.read()

target = """            try {
                val req = ImageRequest.Builder(context).data(uri).size(512).videoFrameMillis(0).build()
                val result = context.imageLoader.execute(req)
                val dr = result.drawable
                if (dr is android.graphics.drawable.BitmapDrawable) {
                    future.set(dr.bitmap)
                } else {
                    future.setException(Exception("err"))
                }
            } catch(e: Exception) { future.setException(e) }"""

replacement = """            try {
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
            }"""

content = content.replace(target, replacement)
with open("app/src/main/java/com/example/MyBitmapLoader.kt", "w") as f:
    f.write(content)
