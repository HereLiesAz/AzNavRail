package com.hereliesaz.aznavrail

import com.hereliesaz.aznavrail.tutorial.AzTutorialController
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import android.app.Application
import android.content.Context

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AzTutorialControllerTest {

    @Before
    fun clearPrefs() {
        ApplicationProvider.getApplicationContext<Application>()
            .getSharedPreferences("az_tutorial_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    @Test
    fun `startTutorial with variables stores id and variables`() {
        val controller = AzTutorialController()
        controller.startTutorial("t1", mapOf("level" to "advanced", "count" to 3))
        assertEquals("t1", controller.activeTutorialId.value)
        assertEquals("advanced", controller.currentVariables["level"])
        assertEquals(3, controller.currentVariables["count"])
    }

    @Test
    fun `startTutorial without variables defaults to empty map`() {
        val controller = AzTutorialController()
        controller.startTutorial("t1")
        assertEquals("t1", controller.activeTutorialId.value)
        assertTrue(controller.currentVariables.isEmpty())
    }

    @Test
    fun `fireEvent sets pendingEvent`() {
        val controller = AzTutorialController()
        controller.fireEvent("menu_opened")
        assertEquals("menu_opened", controller.pendingEvent.value)
    }

    @Test
    fun `consumeEvent clears pendingEvent`() {
        val controller = AzTutorialController()
        controller.fireEvent("menu_opened")
        controller.consumeEvent()
        assertNull(controller.pendingEvent.value)
    }

    @Test
    fun `endTutorial clears activeTutorialId, variables, and pendingEvent`() {
        val controller = AzTutorialController()
        controller.startTutorial("t1", mapOf("x" to 1))
        controller.fireEvent("ev")
        controller.endTutorial()
        assertNull(controller.activeTutorialId.value)
        assertTrue(controller.currentVariables.isEmpty())
        assertNull(controller.pendingEvent.value)
    }

    @Test
    fun `markTutorialRead writes to SharedPreferences`() {
        val prefs = ApplicationProvider.getApplicationContext<Application>()
            .getSharedPreferences("az_tutorial_prefs", Context.MODE_PRIVATE)
        val controller = AzTutorialController(prefs = prefs)
        controller.markTutorialRead("tutorial-1")
        val saved = prefs.getStringSet("az_navrail_read_tutorials", emptySet())
        assertTrue(saved!!.contains("tutorial-1"))
    }

    @Test
    fun `AzTutorialController loads read tutorials from SharedPreferences`() {
        val prefs = ApplicationProvider.getApplicationContext<Application>()
            .getSharedPreferences("az_tutorial_prefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("az_navrail_read_tutorials", setOf("t-a", "t-b")).apply()
        val controller = AzTutorialController(
            initialReadTutorials = prefs.getStringSet("az_navrail_read_tutorials", emptySet())!!.toList(),
            prefs = prefs
        )
        assertTrue(controller.isTutorialRead("t-a"))
        assertTrue(controller.isTutorialRead("t-b"))
    }
}
