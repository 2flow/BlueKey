package com.flow.keyblue

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.flow.keyblue.ui.theme.KeyBlueTheme
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.P)
class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KeyBlueTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PermissionSwitch {
                        Greeting(
                            name = "Android",
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionSwitch(content: @Composable () -> Unit) {
    var permissionsGranted by remember {
        mutableStateOf(false)
    }


    if (permissionsGranted) {
        content()
    } else {
        PermissionCheck {
            permissionsGranted = true
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {

    val context = LocalContext.current
    val coroutine = rememberCoroutineScope()
    val btDevice = remember {
        Bluetooth(context)
    }

    Column {


        Text(
            text = "Hello $name!",
            modifier = modifier
        )
        Button(onClick = {
            coroutine.launch {
                btDevice.sendKeyInput(0x00, 'a'.code.toByte())
            }
        }) {
            Text("Connect")

        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KeyBlueTheme {
    }
}