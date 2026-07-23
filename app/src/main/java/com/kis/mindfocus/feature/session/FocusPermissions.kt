package com.kis.mindfocus.feature.session

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Permission state is Android framework state, so it stays in the UI layer rather than being
 * hoisted into the ViewModel — the ViewModel would then need a `Context` and could not be
 * unit-tested.
 */
@Stable
class FocusPermissionsState(
    val hasMicrophone: Boolean,
    private val onRequest: () -> Unit,
) {
    fun request() = onRequest()
}

@Composable
fun rememberFocusPermissionsState(): FocusPermissionsState {
    val context = LocalContext.current

    var hasMicrophone by remember {
        mutableStateOf(context.isGranted(Manifest.permission.RECORD_AUDIO))
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        // Notifications are requested alongside but are not required to run a session — a denied
        // notification permission only costs the user the distraction alerts.
        hasMicrophone = results[Manifest.permission.RECORD_AUDIO] ?: hasMicrophone
    }

    return remember(hasMicrophone) {
        FocusPermissionsState(
            hasMicrophone = hasMicrophone,
            onRequest = { launcher.launch(requiredPermissions()) },
        )
    }
}

private fun requiredPermissions(): Array<String> = buildList {
    add(Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}.toTypedArray()

private fun android.content.Context.isGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
