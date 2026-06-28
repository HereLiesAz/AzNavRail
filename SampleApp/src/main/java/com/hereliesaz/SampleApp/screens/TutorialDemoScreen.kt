package com.hereliesaz.SampleApp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.tutorial.LocalAzGuidanceController
import kotlin.math.roundToInt

/**
 * Demonstrates the status-driven **guidance** framework (the reactive replacement for the old
 * scripted scene/card tutorial).
 *
 * The statuses, edges, goals and the moving-target registration are declared in the host DSL in
 * `MainApp` (`azStatus`/`azEdge`/`azGoal`/`azGuidanceTarget`/`azSuppressGuide`). This screen drives the
 * [com.hereliesaz.aznavrail.tutorial.AzGuidanceController]: it activates goals, draws the draggable
 * "coach ball" (an arbitrary on-screen highlight target), and observes the live `current` instruction.
 *
 * @param taskDone Current value of the custom `guide_task_done` status (owned by `MainApp`).
 * @param onMarkTaskDone Flips that status true, satisfying the `guide_custom_task` goal.
 * @param onResetTask Flips it back so the custom goal can be demoed again.
 * @param coachBallDragged Whether the coach ball has been dragged (the `coach_ball_dragged` status).
 * @param onCoachBallBounds Reports the coach ball's window-space bounds so the host target can track it.
 * @param onCoachBallDrag Called when the user starts dragging the coach ball (flips its status).
 * @param onResetCoach Resets the coach-ball status so the paged goal can be demoed again.
 * @param suppressed Whether host-driven guidance suppression is on.
 * @param onToggleSuppress Toggles suppression (the overlay hides while on, re-shows after the settle).
 */
@Composable
fun TutorialDemoScreen(
    taskDone: Boolean,
    onMarkTaskDone: () -> Unit,
    onResetTask: () -> Unit,
    coachBallDragged: Boolean,
    onCoachBallBounds: (Rect) -> Unit,
    onCoachBallDrag: () -> Unit,
    onResetCoach: () -> Unit,
    suppressed: Boolean,
    onToggleSuppress: () -> Unit,
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

        // ---------- Worked example: moving target + a mixed manual/reactive paged goal ----------
        Text("Moving-target coach (paged)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "One goal, three steps in one edge: two tap-advanced info steps, then an actionable step " +
                "that auto-advances when you drag the circle. The spotlight is an arbitrary on-screen " +
                "shape (a circle) registered with azGuidanceTarget — it tracks the circle as it moves.",
            style = MaterialTheme.typography.bodySmall,
        )
        AzButton(onClick = { guidance.activate("guide_coach") }, text = "Activate moving-target coach")

        // The draggable coach ball. It reports its window-space bounds so the host target can spotlight
        // it, and flips `coach_ball_dragged` on first drag (auto-advancing the last step).
        Box(modifier = Modifier.fillMaxSize().height(140.dp)) {
            var offset by remember { mutableStateOf(Offset.Zero) }
            Box(
                modifier = Modifier
                    .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(if (coachBallDragged) Color(0xFF4CAF50) else Color(0xFFFFC107))
                    .onGloballyPositioned { onCoachBallBounds(it.boundsInWindow()) }
                    .pointerInput(Unit) {
                        detectDragGestures(onDragStart = { onCoachBallDrag() }) { change, drag ->
                            change.consume(); offset += drag
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (coachBallDragged) "✓" else "drag",
                    color = Color.Black,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        AzButton(onClick = onResetCoach, text = "Reset coach status")

        // Observability: the live current instruction the framework is showing.
        Text("Observed current instruction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        val cur = guidance.current
        Text(
            cur?.let {
                "“${it.text}” — step ${it.stepIndex + 1}/${it.stepTotal}" +
                    (it.targetId?.let { id -> ", target=$id" } ?: "") +
                    (it.goalId?.let { id -> ", goal=$id" } ?: "")
            } ?: "(nothing showing)",
            style = MaterialTheme.typography.bodySmall,
        )

        Text("Suppression", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Drives azSuppressGuide from host state: while on, the overlay hides; when you turn it off " +
                "guidance re-shows after a ~700 ms settle (so a callout never pops over a finishing gesture).",
            style = MaterialTheme.typography.bodySmall,
        )
        AzButton(onClick = onToggleSuppress, text = if (suppressed) "Suppression: ON (tap to clear)" else "Suppression: off (tap to suppress)")

        Text("Stop / skip / replay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Swipe any callout away to cancel tutorial mode (the skip is remembered). A skipped or " +
                "completed goal won't re-activate until you reset.",
            style = MaterialTheme.typography.bodySmall,
        )
        AzButton(
            onClick = { guidance.activeGoals.toList().forEach { guidance.deactivate(it) } },
            text = "Deactivate all goals",
        )
        AzButton(onClick = { guidance.skip() }, text = "Skip (cancel tutorial mode)")
        AzButton(onClick = { guidance.resetGuidance() }, text = "Reset guidance (replay)")
    }
}
