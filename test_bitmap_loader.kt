import androidx.media3.common.util.BitmapLoader
import com.google.common.util.concurrent.ListenableFuture
import android.graphics.Bitmap
import androidx.media3.common.MediaMetadata
import android.net.Uri

class MyBitmapLoader : BitmapLoader {
    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> { TODO() }
    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> { TODO() }
    override fun supportsMimeType(mimeType: String): Boolean { return true }
}
