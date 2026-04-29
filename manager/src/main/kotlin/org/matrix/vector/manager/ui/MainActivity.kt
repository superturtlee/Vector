package org.matrix.vector.manager.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.matrix.vector.manager.Graph
import org.matrix.vector.manager.ui.theme.VectorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Enables edge-to-edge drawing.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        Graph.init(applicationContext)

        setContent {
            // Wraps our app in Material 3 Dynamic Colors
            VectorTheme { VectorApp() }
        }
    }
}
