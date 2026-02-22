package com.hereliesaz.aznavrail

import android.os.Bundle
import androidx.activity.ComponentActivity

interface AzGraphInterface {
    fun Run(activity: ComponentActivity)
}

abstract class AzActivity : ComponentActivity() {
    abstract val graph: AzGraphInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        graph.Run(this)
    }

    open fun AzNavRailScope.configureRail() {
        // Optional override for dynamic configuration
    }
}