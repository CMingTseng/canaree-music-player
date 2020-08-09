package dev.olog.navigation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.CheckResult
import androidx.core.content.ContextCompat
import dev.olog.core.isMarshmallow
import dev.olog.navigation.Navigator.Companion.REQUEST_CODE_HOVER_PERMISSION
import dev.olog.navigation.screens.NavigationIntent
import dev.olog.navigation.utils.ActivityProvider
import javax.inject.Inject

internal class ServiceNavigatorImpl @Inject constructor(
    private val activityProvider: ActivityProvider,
    private val intents: Map<NavigationIntent, @JvmSuppressWildcards Intent>
) : BaseNavigator(), ServiceNavigator {

    override fun toFloating() {
        val activity = activityProvider() ?: return
        val intent = intents[NavigationIntent.SERVICE_FLOATING]
        mandatory(activity, intent != null) ?: return

        if (hasOverlayPermission(activity)){
            ContextCompat.startForegroundService(activity, intent!!)
        } else {
            val permissionIntent = createIntentToRequestOverlayPermission(activity)
            activity.startActivityForResult(permissionIntent, REQUEST_CODE_HOVER_PERMISSION)
        }
    }

    @CheckResult
    private fun hasOverlayPermission(context: Context): Boolean {
        return if (isMarshmallow()) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    @SuppressLint("InlinedApi")
    @CheckResult
    private fun createIntentToRequestOverlayPermission(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }


}