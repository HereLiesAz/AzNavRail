// FILE: ./aznavrail/src/main/java/com/hereliesaz/aznavrail/AzActivity.kt
package com.hereliesaz.aznavrail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

interface AzGraphInterface {
    fun Run(activity: ComponentActivity)
}

abstract class AzActivity : ComponentActivity() {
    abstract val graph: AzGraphInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        graph.Run(this)
    }

    open fun AzNavRailScope.configureRail() {
        // Optional override for dynamic configuration
    }
}