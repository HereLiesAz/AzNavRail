// aznavrail/src/main/java/com/hereliesaz/aznavrail/AzActivity.kt
package com.hereliesaz.aznavrail

import android.os.Bundle
import androidx.activity.ComponentActivity

abstract class AzActivity : ComponentActivity() {

    abstract val graph: AzGraphInterface

    /**
     * The Aesthetic Escape Hatch.
     * The processor dictates the immutable structure of the NavGraph. 
     * Override this function to inject your subjective, runtime frailties—like colors, 
     * dynamic docking, and overlay settings—into the processor's rigid domain.
     */
    open fun AzNavRailScope.configureRail() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        graph.Run(this)
    }
}
