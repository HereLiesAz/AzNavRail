package com.hereliesaz.aznavrail.updates.play

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import com.hereliesaz.aznavrail.updates.AzPlayUpdateSource
import com.hereliesaz.aznavrail.updates.AzUpdateOffer

/** Queries real Play update availability; include only in Play-distributed builds. */
object AzPlayUpdates : AzPlayUpdateSource {
    override fun check(activity: Activity, callback: (AzUpdateOffer?) -> Unit) {
        try {
            AppUpdateManagerFactory.create(activity).appUpdateInfo
                .addOnSuccessListener { info ->
                    callback(if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE)
                        AzUpdateOffer("Build ${info.availableVersionCode()}") else null)
                }
                .addOnFailureListener { callback(null) }
                .addOnCanceledListener { callback(null) }
        } catch (_: Exception) {
            callback(null)
        }
    }
}
