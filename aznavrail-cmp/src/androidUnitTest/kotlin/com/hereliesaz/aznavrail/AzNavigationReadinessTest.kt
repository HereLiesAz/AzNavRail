package com.hereliesaz.aznavrail

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * CMP-module Robolectric navigation readiness tests. Mirrors the Android-module suite so that
 * CMP-specific changes to NavigationReadiness.kt are caught on the same set of scenarios.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AzNavigationReadinessTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        AzPendingNavigation.clearAllForTest()
    }

    @After
    fun tearDown() {
        AzPendingNavigation.clearAllForTest()
    }

    @Test
    fun azNavigateWhenReadyBeforeGraphDoesNotThrow() {
        lateinit var controller: NavHostController

        rule.setContent {
            controller = rememberNavController()
        }

        rule.runOnIdle {
            controller.azNavigateWhenReady("some-route")
            assertEquals(null, controller.currentDestination?.route)
        }
    }

    @Test
    fun azNavigateWhenReadyAfterGraphNavigatesImmediately() {
        lateinit var controller: NavHostController

        rule.setContent {
            controller = rememberNavController()
            NavHost(navController = controller, startDestination = "start") {
                composable("start") {}
                composable("details") {}
            }
        }

        rule.runOnIdle { controller.azNavigateWhenReady("details") }

        rule.runOnIdle {
            assertEquals("details", controller.currentDestination?.route)
        }
    }

    @Test
    fun routesEnqueuedBeforeGraphFireAfterInstallation() {
        lateinit var controller: NavHostController
        val hostReady = mutableStateOf(false)

        rule.setContent {
            controller = rememberNavController()
            if (hostReady.value) {
                NavHost(navController = controller, startDestination = "home") {
                    composable("home") {}
                    composable("target") {}
                }
            }
        }

        rule.runOnIdle { controller.azNavigateWhenReady("target") }

        rule.runOnUiThread { hostReady.value = true }
        rule.waitForIdle()

        rule.runOnIdle {
            assertEquals("target", controller.currentDestination?.route)
        }
    }

    @Test
    fun identicalPendingRoutesAreCoalesced() {
        lateinit var controller: NavHostController
        val hostReady = mutableStateOf(false)

        rule.setContent {
            controller = rememberNavController()
            if (hostReady.value) {
                NavHost(navController = controller, startDestination = "home") {
                    composable("home") {}
                    composable("library") {}
                }
            }
        }

        rule.runOnIdle {
            controller.azNavigateWhenReady("library")
            controller.azNavigateWhenReady("library")
            controller.azNavigateWhenReady("library")
        }

        rule.runOnUiThread { hostReady.value = true }
        rule.waitForIdle()

        rule.runOnIdle {
            assertEquals("library", controller.currentDestination?.route)
            // One pop must land on "home" — not another "library" copy
            controller.popBackStack()
        }

        rule.runOnIdle {
            assertEquals("home", controller.currentDestination?.route)
        }
    }

    @Test
    fun cancelPendingNavigationDropsQueue() {
        lateinit var controller: NavHostController
        val hostReady = mutableStateOf(false)

        rule.setContent {
            controller = rememberNavController()
            if (hostReady.value) {
                NavHost(navController = controller, startDestination = "home") {
                    composable("home") {}
                    composable("other") {}
                }
            }
        }

        rule.runOnIdle {
            controller.azNavigateWhenReady("other")
            controller.cancelPendingNavigation()
        }

        rule.runOnUiThread { hostReady.value = true }
        rule.waitForIdle()

        rule.runOnIdle {
            assertEquals("home", controller.currentDestination?.route)
        }
    }

    @Test
    fun cancelPendingNavigationOnCleanControllerDoesNotThrow() {
        lateinit var controller: NavHostController

        rule.setContent {
            controller = rememberNavController()
        }

        rule.runOnIdle {
            controller.cancelPendingNavigation()
        }
    }

    @Test
    fun controllerReplacementDoesNotReplayOldRoutes() {
        val scope = AzNavHostScopeImpl()
        lateinit var firstController: NavHostController
        lateinit var secondController: NavHostController
        val firstReady = mutableStateOf(false)
        val secondReady = mutableStateOf(false)

        rule.setContent {
            firstController = rememberNavController()
            secondController = rememberNavController()

            if (firstReady.value) {
                NavHost(navController = firstController, startDestination = "home") {
                    composable("home") {}
                    composable("old-route") {}
                }
            }
            if (secondReady.value) {
                NavHost(navController = secondController, startDestination = "home") {
                    composable("home") {}
                    composable("new-route") {}
                }
            }
        }

        rule.runOnIdle {
            scope.setController(firstController)
            firstController.azNavigateWhenReady("old-route")
            scope.setController(secondController)
        }

        rule.runOnUiThread {
            firstReady.value = true
            secondReady.value = true
        }
        rule.waitForIdle()

        rule.runOnIdle {
            assertEquals("home", firstController.currentDestination?.route)
            assertEquals("home", secondController.currentDestination?.route)
        }
    }

    @Test
    fun recompositionDoesNotMultiplyPendingRoutes() {
        val trigger = mutableStateOf(0)
        lateinit var controller: NavHostController
        val hostReady = mutableStateOf(false)

        rule.setContent {
            @Suppress("UNUSED_EXPRESSION") trigger.value
            controller = rememberNavController()
            if (hostReady.value) {
                NavHost(navController = controller, startDestination = "home") {
                    composable("home") {}
                    composable("dest") {}
                }
            }
        }

        rule.runOnIdle { controller.azNavigateWhenReady("dest") }

        rule.runOnUiThread { trigger.value++ }
        rule.waitForIdle()
        rule.runOnUiThread { trigger.value++ }
        rule.waitForIdle()

        rule.runOnUiThread { hostReady.value = true }
        rule.waitForIdle()

        rule.runOnIdle {
            assertEquals("dest", controller.currentDestination?.route)
            // One pop must return to "home" — recomposition must not have duplicated the route
            controller.popBackStack()
        }

        rule.runOnIdle {
            assertEquals("home", controller.currentDestination?.route)
        }
    }

    @Test
    fun afterDispatchFurtherCallsNavigateImmediately() {
        lateinit var controller: NavHostController
        val hostReady = mutableStateOf(false)

        rule.setContent {
            controller = rememberNavController()
            if (hostReady.value) {
                NavHost(navController = controller, startDestination = "a") {
                    composable("a") {}
                    composable("b") {}
                    composable("c") {}
                }
            }
        }

        rule.runOnIdle { controller.azNavigateWhenReady("b") }

        rule.runOnUiThread { hostReady.value = true }
        rule.waitForIdle()

        rule.runOnIdle {
            assertEquals("b", controller.currentDestination?.route)
            controller.azNavigateWhenReady("c")
        }

        rule.runOnIdle {
            assertEquals("c", controller.currentDestination?.route)
        }
    }
}
