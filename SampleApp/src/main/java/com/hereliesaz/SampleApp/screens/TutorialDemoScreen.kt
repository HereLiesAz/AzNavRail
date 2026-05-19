package com.hereliesaz.SampleApp.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.tutorial.AzAdvanceCondition
import com.hereliesaz.aznavrail.tutorial.AzHighlight
import com.hereliesaz.aznavrail.tutorial.AzTutorial
import com.hereliesaz.aznavrail.tutorial.LocalAzTutorialController
import com.hereliesaz.aznavrail.tutorial.azTutorial

private const val TAG = "TutorialDemoScreen"

/** Tutorial that walks through every public AzAdvanceCondition and AzHighlight variant. */
val SampleTutorials: Map<String, AzTutorial> = mapOf(
    "showcase-tour" to azTutorial {
        onComplete { Log.d(TAG, "Showcase tour completed") }
        onSkip { Log.d(TAG, "Showcase tour skipped") }

        scene(
            id = "intro",
            content = { TutorialBackdrop(label = "Scene: intro", tint = Color(0xFF1565C0)) },
        ) {
            card(
                title = "Welcome",
                text = "This card uses AzAdvanceCondition.Button — tap the action to continue.",
                advanceCondition = AzAdvanceCondition.Button,
                actionText = "Next",
                highlight = AzHighlight.FullScreen,
            )
            card(
                title = "Tap-anywhere",
                text = "Tap anywhere on the screen to advance past this card.",
                advanceCondition = AzAdvanceCondition.TapAnywhere,
                highlight = AzHighlight.None,
            )
        }

        scene(
            id = "targeting",
            content = { TutorialBackdrop(label = "Scene: targeting", tint = Color(0xFF2E7D32)) },
        ) {
            card(
                title = "Highlight: Item",
                text = "AzHighlight.Item points at a nav-rail item by ID. The 'home' rail item is currently highlighted.",
                highlight = AzHighlight.Item(id = "home"),
                advanceCondition = AzAdvanceCondition.Button,
            )
            card(
                title = "Tap target",
                text = "AzAdvanceCondition.TapTarget requires the user to tap the highlighted area.",
                highlight = AzHighlight.Item(id = "showcase-home"),
                advanceCondition = AzAdvanceCondition.TapTarget,
            )
        }

        scene(
            id = "events",
            content = { TutorialBackdrop(label = "Scene: events", tint = Color(0xFFEF6C00)) },
        ) {
            card(
                title = "Event-driven advance",
                text = "This card waits for the named event \"tutorial-go\". Fire it from the screen below to advance.",
                advanceCondition = AzAdvanceCondition.Event("tutorial-go"),
                highlight = AzHighlight.FullScreen,
            )
            card(
                title = "Checklist card",
                text = "Cards can carry a checklist for users to tick off before moving on.",
                checklistItems = listOf("Inspect the highlight", "Read the body text", "Press Next when ready"),
                advanceCondition = AzAdvanceCondition.Button,
            )
            card(
                title = "Media card",
                text = "Cards can host arbitrary composable media — anything from images to live previews.",
                mediaContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .background(Color(0xFFFFD54F))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Live media content", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                advanceCondition = AzAdvanceCondition.Button,
                actionText = "Finish",
            )
        }
    },
    "branching-demo" to azTutorial {
        scene(
            id = "branch-root",
            content = { TutorialBackdrop(label = "Scene: branch-root", tint = Color(0xFF6A1B9A)) },
        ) {
            branch(varName = "path", branches = mapOf("a" to "branch-a", "b" to "branch-b"))
            card(
                title = "Branching",
                text = "When this tutorial is started with variables = mapOf(\"path\" to \"a\" or \"b\") the next scene is chosen at runtime.",
                advanceCondition = AzAdvanceCondition.Button,
            )
        }
        scene(id = "branch-a", content = { TutorialBackdrop("Branch A", Color(0xFFC2185B)) }) {
            card(title = "Path A", text = "You started this tutorial with path=a.")
        }
        scene(id = "branch-b", content = { TutorialBackdrop("Branch B", Color(0xFF0277BD)) }) {
            card(title = "Path B", text = "You started this tutorial with path=b.")
        }
    },
)

@Composable
private fun TutorialBackdrop(label: String, tint: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tint.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = tint, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TutorialDemoScreen() {
    val controller = LocalAzTutorialController.current
    val activeId by controller.activeTutorialId
    var branchChoice by remember { mutableStateOf("a") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Interactive Tutorials", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Tutorials are built from a DSL of scenes and cards. Each card declares its highlight target and advance condition. Read-state is persisted to SharedPreferences via AzTutorialController.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text("Active tutorial: ${activeId ?: "(none)"}", fontWeight = FontWeight.SemiBold)
        Text("Marked-read tutorials: ${controller.readTutorials.joinToString().ifEmpty { "(none)" }}")

        AzButton(
            onClick = { controller.startTutorial("showcase-tour") },
            text = "Start showcase-tour",
        )
        AzButton(
            onClick = { controller.fireEvent("tutorial-go") },
            text = "fireEvent(\"tutorial-go\")",
        )
        AzButton(
            onClick = { controller.startTutorial("branching-demo", variables = mapOf("path" to branchChoice)) },
            text = "Start branching-demo (path=$branchChoice)",
        )
        AzButton(
            onClick = { branchChoice = if (branchChoice == "a") "b" else "a" },
            text = "Toggle branch path",
        )
        AzButton(
            onClick = { controller.markTutorialRead("showcase-tour") },
            text = "markTutorialRead(\"showcase-tour\")",
        )
        AzButton(
            onClick = { controller.endTutorial() },
            text = "endTutorial()",
        )
    }
}
