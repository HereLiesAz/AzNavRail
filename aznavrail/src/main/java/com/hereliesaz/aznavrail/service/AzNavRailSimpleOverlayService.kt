package com.hereliesaz.aznavrail.service

/**
 * A convenience class for creating a standard system overlay with AzNavRail without a foreground service.
 * This relies solely on [android.permission.SYSTEM_ALERT_WINDOW].
 *
 * To use this, extend this class and implement [OverlayContent].
 *
 * Note: Since this is not a foreground service, the system may kill it more aggressively than [AzNavRailOverlayService].
 */
abstract class AzNavRailSimpleOverlayService : AzNavRailWindowService()
