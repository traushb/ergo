package app.ergo

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import app.ergo.ui.ErgoApp
import app.ergo.ui.ErgoTheme

class MainActivity : ComponentActivity() {
    private val vm: ErgoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Paper background everywhere, so system bar icons are always dark.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            ErgoTheme {
                ErgoApp(vm, onExit = ::finish)
            }
        }
    }
}
