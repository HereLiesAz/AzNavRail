package com.hereliesaz.SampleApp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.tutorial.LocalAzGuidanceController

/**
 * Demonstrates the status-driven **guidance** framework (the reactive replacement for the old
 * scripted scene/card tutorial).
 *
 * The statuses, edges and goals are declared in the host DSL in `MainApp` (`azStatus`/`azEdge`/
 * `azGoal`). This screen only drives the [com.hereliesaz.aznavrail.tutorial.AzGuidanceController]:
 * it activates goals, and the framework figures out — live — which instruction to show next and
 * places each as a callout next to the control you'd use. There is no Next button; performing the
 * action flips a status and the callout advances on its own.
 *
 * @param taskDone Current value of the custom `guide_task_done` status (owned by `MainApp`).
 * @param onMarkTaskDone Flips that status true, satisfying the `guide_custom_task` goal.
 * @param onResetTask Flips it back so the custom goal can be demoed again.
 */
@Composable
fun TutorialDemoScreen(
    taskDone: Boolean,
    onMarkTaskDone: () -> Unit,
    onResetTask: () -> Unit,
) {
    val guidance = LocalAzGuidanceController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Status-driven guidance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Guidance describes the app as a flowchart of statuses and edges. You activate goals " +
                "(target statuses); the framework always shows the instruction to reach the next status " +
                "on the way there, auto-advancing the instant a target status becomes true and routing " +
                "around wherever you actually are. Several goals can guide at once — each callout sits " +
                "next to its own control.",
            style = MaterialTheme.typography.bodyMedium,
        )

        if (guidance == null) {
            Text(
                "No AzGuidanceController in scope — this screen must run inside AzHostActivityLayout.",
                color = MaterialTheme.colorScheme.error,
            )
            return@Column
        }

        Text("Guidance enabled: ${guidance.enabled}", fontWeight = FontWeight.SemiBold)
        Text("Active goals: ${guidance.activeGoals.joinToString().ifEmpty { "(none)" }}")
        Text("Completed goals: ${guidance.completedGoals.joinToString().ifEmpty { "(none)" }}")
        Text("Custom status guide_task_done: $taskDone")

        Text("Master switch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        AzButton(onClick = { guidance.enable() }, text = "enable()")
        AzButton(onClick = { guidance.disable() }, text = "disable()")

        Text("Activate a single goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        // Built-in target reached by an auto-generated edge ("Tap Bottom Sheets").
        AzButton(onClick = { guidance.activate("guide_onboarding") }, text = "Guide me to Bottom Sheets")
        // Built-in host target ("Tap Rail Host").
        AzButton(onClick = { guidance.activate("guide_expand_host") }, text = "Guide me to expand Rail Host")
        // Custom status reached by a hand-authored edge.
        AzButton(onClick = { guidance.activate("guide_custom_task") }, text = "Guide me through the custom task")

        Text("Two goals at once", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Activates both the host-expand goal and the custom-task goal. Both instructions show " +
                "simultaneously, each as a callout next to its own control.",
            style = MaterialTheme.typography.bodySmall,
        )
        AzButton(
            onClick = {
                guidance.activate("guide_expand_host")
                guidance.activate("guide_custom_task")
            },
            text = "Activate both (host + task)",
        )

        Text("Satisfy / reset the custom status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        AzButton(onClick = onMarkTaskDone, text = "Mark task done (flip guide_task_done)")
        AzButton(onClick = onResetTask, text = "Reset task")

        Text("Stop guiding", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        AzButton(
            onClick = { guidance.activeGoals.toList().forEach { guidance.deactivate(it) } },
            text = "Deactivate all goals",
        )
    }
}
