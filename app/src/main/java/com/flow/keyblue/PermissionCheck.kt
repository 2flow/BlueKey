package com.flow.keyblue

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.launch


enum class Permission {
    BLUETOOTH_CONNECT,
    FINE_LOCATION,
    BLUETOOTH_ENABLED,
}

sealed interface PermissionCheckState {
    data object Loading : PermissionCheckState
    data class GrantPermission(val permission: Permission) : PermissionCheckState
    data object AllPermissionsGranted : PermissionCheckState
}


class PermissionCheckerViewModel(private val context: Context, private val onGranted: () -> Unit) {
    private val unsetPermissions: MutableSet<Permission> = mutableSetOf()
    private val _uiState = mutableStateOf<PermissionCheckState>(PermissionCheckState.Loading)

    val uiState by _uiState


    suspend fun refreshPermissions() {
        unsetPermissions.clear()
        checkPermission(Permission.BLUETOOTH_CONNECT)
        checkPermission(Permission.FINE_LOCATION)
        updateState()
    }

    fun nextPermission() {

        if (unsetPermissions.isNotEmpty()) {
            unsetPermissions.remove(unsetPermissions.first())
        }
        updateState()
    }

    private fun updateState() {
        if (unsetPermissions.isEmpty()) {
            _uiState.value = PermissionCheckState.AllPermissionsGranted
            onGranted()
        } else {
            _uiState.value = PermissionCheckState.GrantPermission(unsetPermissions.first())
        }
    }

    private fun checkPermission(permission: Permission) {
        when (permission) {
            Permission.BLUETOOTH_CONNECT -> {
                checkMundanePermission(
                    Permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            }

            Permission.FINE_LOCATION -> {
                checkMundanePermission(
                    Permission.FINE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }

            Permission.BLUETOOTH_ENABLED -> {
                val bluetoothAdapter: BluetoothManager =
                    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                if (bluetoothAdapter.adapter == null || !bluetoothAdapter.adapter.isEnabled) {
                    unsetPermissions.add(permission)
                }
            }
        }
    }

    private fun checkMundanePermission(permission: Permission, manifestPermission: String) {
        if (ActivityCompat.checkSelfPermission(
                context, manifestPermission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            unsetPermissions.add(permission)
        }
    }
}

@Composable
fun PermissionCheck(onGranted: () -> Unit) {

    val context = LocalContext.current
    val coroutine = rememberCoroutineScope()
    val viewModel = remember {
        PermissionCheckerViewModel(context = context, onGranted = onGranted)
    }

    SideEffect {
        coroutine.launch {
            viewModel.refreshPermissions()
        }
    }

    when (val uiState = viewModel.uiState) {
        is PermissionCheckState.Loading -> {
            // Show loading
        }

        is PermissionCheckState.GrantPermission -> {
            GrantPermission(permission = uiState.permission) {
                coroutine.launch {
                    viewModel.nextPermission()
                }
            }
        }

        is PermissionCheckState.AllPermissionsGranted -> {

        }
    }

}

@Composable
private fun GrantPermission(permission: Permission, onGranted: () -> Unit) {

    when (permission) {
        Permission.BLUETOOTH_CONNECT -> {
            RequestMundanePermission(Manifest.permission.BLUETOOTH_CONNECT, onGranted = onGranted)
        }

        // Show permission granted
        Permission.FINE_LOCATION -> {
            RequestMundanePermission(mundanePermission = Manifest.permission.ACCESS_FINE_LOCATION) {
                onGranted()
            }
        }

        Permission.BLUETOOTH_ENABLED -> TODO()
    }
}

@Composable
private fun RequestMundanePermission(mundanePermission: String, onGranted: () -> Unit) {
    var retryState by remember { mutableIntStateOf(0) }
    val activityResult =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                onGranted()
            } else {
                retryState++
            }
        }


    LaunchedEffect(key1 = retryState) {
        activityResult.launch(mundanePermission)
    }
}

