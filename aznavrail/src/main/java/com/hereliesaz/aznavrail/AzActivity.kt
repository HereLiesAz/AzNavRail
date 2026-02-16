package com.hereliesaz.aznavrail

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.hereliesaz.aznavrail.annotation.Az

abstract class AzActivity : ComponentActivity() {

    abstract val graph: AzGraphInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        graph.Run(this)
    }
}
