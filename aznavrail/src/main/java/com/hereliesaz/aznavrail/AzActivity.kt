// aznavrail/src/main/java/com/hereliesaz/aznavrail/AzActivity.kt
package com.hereliesaz.aznavrail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.hereliesaz.aznavrail.model.AzDockingSide

abstract class AzActivity : ComponentActivity() {

    abstract val graph: AzGraphInterface

    // The escape hatch. Update this state to trigger a dynamic reconfiguration.
    open val dynamicDockingSide: MutableState<AzDockingSide?> = mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        graph.Run(this)
    }
}
