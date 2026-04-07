package com.hereliesaz.aznavrail.internal

import android.Manifest
import android.app.Application
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowLog
import org.junit.Assert.assertTrue
import android.os.Looper
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onAllNodesWithText

@Implements(LocationManager::class)
class ExceptionShadowLocationManager : org.robolectric.shadows.ShadowLocationManager() {

    @Implementation
    override fun requestLocationUpdates(
        provider: String,
        minTimeMs: Long,
        minDistanceM: Float,
        listener: LocationListener,
        looper: Looper?
    ) {
        throw SecurityException("Mocked SecurityException")
    }

    @Implementation
    override fun requestLocationUpdates(
        provider: String,
        minTimeMs: Long,
        minDistanceM: Float,
        listener: LocationListener
    ) {
        throw SecurityException("Mocked SecurityException")
    }

    @Implementation
    override fun getProviders(enabledOnly: Boolean): MutableList<String> {
        val list = mutableListOf<String>()
        list.add(LocationManager.GPS_PROVIDER)
        list.add(LocationManager.NETWORK_PROVIDER)
        return list
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], shadows = [ExceptionShadowLocationManager::class])
class SecretScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testRequestLocationUpdatesException() {
        ShadowLog.stream = System.out

        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        composeTestRule.setContent {
            val trigger = SecretScreens("1234", 1234)
            androidx.compose.runtime.LaunchedEffect(Unit) {
                trigger()
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onAllNodes(hasSetTextAction())[0].performTextInput("1234")
        composeTestRule.onNodeWithText("Unlock").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Run as Source (Server)").performClick()
        composeTestRule.waitForIdle()

        val logs = ShadowLog.getLogsForTag("SecretScreens")
        val errorLog = logs.find { it.msg.contains("Error requesting location updates") }

        assertTrue("Expected an error log about location updates, but found none.", errorLog != null)
    }
}
