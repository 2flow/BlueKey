package com.flow.keyblue

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@RequiresApi(Build.VERSION_CODES.P)
class Bluetooth(private val context: Context) {
    private var device: BluetoothDevice? = null
    private var hidDevice: BluetoothHidDevice? = null

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val hidDeviceCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            // Handle app status changes

            this@Bluetooth.device = pluggedDevice
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            // Handle connection state changes
            this@Bluetooth.device = device
        }
    }


    val HID_DESCRIPTOR = byteArrayOf(
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x06.toByte(), // Usage (Keyboard)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), 0x01.toByte(), // Report ID (1)
        0x05.toByte(), 0x07.toByte(), // Usage Page (Key Codes)
        0x19.toByte(), 0xE0.toByte(), // Usage Minimum (224)
        0x29.toByte(), 0xE7.toByte(), // Usage Maximum (231)
        0x15.toByte(), 0x00.toByte(), // Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), // Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(), // Report Size (1)
        0x95.toByte(), 0x08.toByte(), // Report Count (8)
        0x81.toByte(), 0x02.toByte(), // Input (Data, Variable, Absolute) ; Modifier byte
        0x95.toByte(), 0x01.toByte(), // Report Count (1)
        0x75.toByte(), 0x08.toByte(), // Report Size (8)
        0x81.toByte(), 0x01.toByte(), // Input (Constant) ; Reserved byte
        0x95.toByte(), 0x05.toByte(), // Report Count (5)
        0x75.toByte(), 0x01.toByte(), // Report Size (1)
        0x05.toByte(), 0x08.toByte(), // Usage Page (LEDs)
        0x19.toByte(), 0x01.toByte(), // Usage Minimum (1)
        0x29.toByte(), 0x05.toByte(), // Usage Maximum (5)
        0x91.toByte(), 0x02.toByte(), // Output (Data, Variable, Absolute) ; LED report
        0x95.toByte(), 0x01.toByte(), // Report Count (1)
        0x75.toByte(), 0x03.toByte(), // Report Size (3)
        0x91.toByte(), 0x01.toByte(), // Output (Constant) ; LED report padding
        0x95.toByte(), 0x06.toByte(), // Report Count (6)
        0x75.toByte(), 0x08.toByte(), // Report Size (8)
        0x15.toByte(), 0x00.toByte(), // Logical Minimum (0)
        0x25.toByte(), 0x65.toByte(), // Logical Maximum (101)
        0x05.toByte(), 0x07.toByte(), // Usage Page (Key Codes)
        0x19.toByte(), 0x00.toByte(), // Usage Minimum (0)
        0x29.toByte(), 0x65.toByte(), // Usage Maximum (101)
        0x81.toByte(), 0x00.toByte(), // Input (Data, Array) ; Key arrays (6 bytes)
        0xC0.toByte()                 // End Collection
    )

    val sdpSettings = BluetoothHidDeviceAppSdpSettings(
        "KeyBlue", "Bluetooth Keyboard", "1.0", BluetoothHidDevice.SUBCLASS1_COMBO, HID_DESCRIPTOR
    )

    val working = bluetoothAdapter.getProfileProxy(
        context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                val hidDevice = proxy as BluetoothHidDevice
                if (ActivityCompat.checkSelfPermission(
                        this@Bluetooth.context, Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }

                val success = hidDevice.registerApp(
                    sdpSettings, null, null, Executors.newCachedThreadPool(), hidDeviceCallback
                )
                if (!success) {
                    return
                }
                this@Bluetooth.hidDevice = hidDevice
            }

            override fun onServiceDisconnected(profile: Int) {
                // Handle service disconnected
            }
        }, BluetoothProfile.HID_DEVICE
    )


    @SuppressLint("MissingPermission")
    suspend fun sendKeyInput(
        modifier: Byte, keyCode: Byte
    ) {
        val report = byteArrayOf(
            0x01,       // Report ID (1)
            modifier,   // Modifier keys (e.g., Shift, Ctrl)
            0x00,       // Reserved
            0x04,    // Key code (e.g., 'a' key)
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00 // Rest of the key array (max 6 keys pressed simultaneously)
        )


        hidDevice?.sendReport(device, 1, report) // 1 is the Report ID

        delay(50)
        val report22 = byteArrayOf(
            0x01,       // Report ID (1)
            0x00,   // Modifier keys (e.g., Shift, Ctrl)
            0x00,       // Reserved
            0x00,    // Key code (e.g., 'a' key)
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00 // Rest of the key array (max 6 keys pressed simultaneously)
        )

        hidDevice?.sendReport(device, 1, report22) // 1 is the Report ID
    }
}
