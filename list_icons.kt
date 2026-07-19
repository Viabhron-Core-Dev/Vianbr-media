import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

fun main() {
    val icons = Icons.Filled::class.java.methods.map { it.name }
    println(icons.filter { it.contains("fit", ignoreCase=true) || it.contains("screen", ignoreCase=true) || it.contains("aspect", ignoreCase=true) })
}
