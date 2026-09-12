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
 * Verifies navigation readiness semantics end-to-end.
 *
 * Concerns covered:
 *  1. Pre-readiness queueing: no throw, no immediate navigate()
 *  2. Post-readiness: immediate navigation
 *  3. Deferred dispatch: routes fire after NavHost installs the graph
 *  4. Ordering: multiple pending routes fire in declaration order
 *  5. Coalescing: duplicate pending routes are not duplicated
 *  6. Cancellation: cancelPendingNavigation() purges queue and listener
 *  7. Controller replacement: old routes are not replayed on new controller
 *  8. Recomposition: does not multiply pending routes
 *  9. Post-dispatch: further calls navigate immediately
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

    // -------------------------------------------------------------------------
    // 1. Pre-readiness: no throw, not navigated
    // -------------------------------------------------------------------------

    @Test
    fun `azNavigateWhenReady before graph readiness does not throw`() {
        lateinit var controller: NavHostController

        rule.setContent {
            controller = rememberNavController()
        }

        rule.runOnIdle {
            controller.azNavigateWhenReady("some-route")
            // No exception = pass; currentDestination is null (no graph)
            assertEquals(null, controller.currentDestination?.route)
        }
    }

    // -------------------------------------------------------------------------
    // 2. Post-readiness: navigates immediately
    // -------------------------------------------------------------------------

    @Test
    fun `azNavigateWhenReady after graph readiness navigates immediately`() {
        lateinit var controller: NavHostController

        rule.setContent {
            controller = rememberNavController()
            NavHost(navController = controller, startDestination = "start") {
                composable("start") {}
                composable("details") {}
            }
        }

        rule.runOnIdle {
            controller.azNavigateWhenReady("details")
        }

        rule.runOnIdle {
            assertEquals("details", controller.currentDestination?.route)
        }
    }

    // -------------------------------------------------------------------------
    // 3. Deferred dispatch: fires after graph installation
    // -------------------------------------------------------------------------

    @Test
    fun `routes enqueued before graph installation are dispatched after first destination`() {
        lateinit var controller: NavHostController
        val hostReady = mutableStateOf(false)

        rule.setContent {
            controller = rememberNavController()
            if (hostReady.value) {
                NavHost(navController = controller, startDestination = "start") {
                    composable("start") {}
                    composable("target") {}
                }
            }
        }

        rule.runOnIdle {
            controller.azNavigateWhenReady("target")
            assertEquals(null, controller.currentDestination?.route)
        }

        rule.runOnUiThread { hostReady.value = true }
        rule.waitForIdle()

        rule.runOnIdle {
            assertEquals("target", controller.currentDestination?.route)
        }
    }

    // -------------------------------------------------------------------------
    // 4. Ordering: multiple routes fire in declaration order; last destination is last route
    // -------------------------------------------------------------------------

    @Test
    fun `multiple pending routes fire in declaration order`() {
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

        rule.runOnIdle {
            controller.azNavigateWhenReady("b")
            controller.azNavigateWhenReady("c")
        }

        rule.runOnUiThread { hostReady.value = true }
        rule.waitForIdle()

        rule.runOnIdle {
            // Both routes fired in order; last destination is "c"
            assertEquals("c", controller.currentDestination?.route)
        }
    }

    // -------------------------------------------------------------------------
    // 5. Coalescing: identical routes are not duplicated
    // -------------------------------------------------------------------------

    @Test
    fun `identical pending routes are coalesced into one request`() {
        lateinit var controller: NavHostController
        val hostReady = mutableStateOf(false)
        var destinationChangeCount = 0

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
            // Only one entry for "library" — the controller must be on it and stack depth = 2
            assertEquals("library", controller.currentDestination?.route)
            // currentBackStack (public in 2.7+) or check via popBackStack; here we just check
            // that a second popBackStack returns us to "home", not another "library" copy
            controller.popBackStack()
        }

        rule.runOnIdle {
            assertEquals(
                "after one pop we must be on 'home', not a duplicated 'library'",
                "home",
                controller.currentDestination?.route
            )
        }
    }

    @Test
    fun `distinct pending routes are all enqueued`() {
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

        rule.runOnIdle {
            controller.azNavigateWhenReady("b")
            controller.azNavigateWhenReady("c")
        }

        rule.runOnUiThread { hostReady.value = true }
        rule.waitForIdle()

        rule.runOnIdle {
            // Destination is "c"; pop gives "b"; pop again gives "a"
            assertEquals("c", controller.currentDestination?.route)
            controller.popBackStack()
        }

        rule.runOnIdle {
            assertEquals("b", controller.currentDestination?.route)
        }
    }

    // -------------------------------------------------------------------------
    // 6a. Cancellation: drops pending routes
    // -------------------------------------------------------------------------

    @Test
    fun `cancelPendingNavigation drops pending routes for that controller`() {
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
    fun `cancelPendingNavigation on controller with no pending routes does not throw`() {
        lateinit var controller: NavHostController

        rule.setContent {
            controller = rememberNavController()
        }

        rule.runOnIdle {
            controller.cancelPendingNavigation()
        }
    }

    // -------------------------------------------------------------------------
    // 6b. Cancellation: listener is detached
    // -------------------------------------------------------------------------

    @Test
    fun `cancelPendingNavigation removes the destination-changed listener`() {
        lateinit var controller: NavHostController
        val hostReady = mutableStateOf(false)

        rule.setContent {
            controller = rememberNavController()
            if (hostReady.value) {
                NavHost(navController = controller, startDestination = "home") {
                    composable("home") {}
                    composable("target") {}
                    composable("unintended") {}
                }
            }
        }

        rule.runOnIdle {
            controller.azNavigateWhenReady("target")
            controller.cancelPendingNavigation()

            // To prove the listener was removed, re-populate the routes queue bypass
            // the listener setup. If the listener wasn't removed, it will fire and process this queue.
            val routesField = AzPendingNavigation::class.java.getDeclaredField("routes").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            val routesMap = routesField.get(AzPendingNavigation) as MutableMap<androidx.navigation.NavController, MutableList<String>>
            routesMap[controller] = mutableListOf("unintended")
        }

        rule.runOnUiThread { hostReady.value = true }
        rule.waitForIdle()

        rule.runOnIdle {
            // Must still be on home, proving the old listener did not execute our "unintended" injection
            assertEquals("home", controller.currentDestination?.route)
        }
    }

    // -------------------------------------------------------------------------
    // 7. Controller replacement: old routes are not replayed on new controller
    // -------------------------------------------------------------------------

    @Test
    fun `setController replacement cancels old controller pending routes`() {
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
            // Replace the controller — must cancel firstController's pending queue
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

    // -------------------------------------------------------------------------
    // 8. Recomposition: does not multiply pending routes
    // -------------------------------------------------------------------------

    @Test
    fun `recomposition during pre-readiness window does not multiply pending routes`() {
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
            // "dest" must appear exactly once — pop should return to "home", not another "dest"
            assertEquals("dest", controller.currentDestination?.route)
            controller.popBackStack()
        }

        rule.runOnIdle {
            assertEquals("home", controller.currentDestination?.route)
        }
    }

    // -------------------------------------------------------------------------
    // 9. Post-dispatch: queue cleared; further calls navigate immediately
    // -------------------------------------------------------------------------

    @Test
    fun `after dispatch further calls navigate immediately`() {
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
