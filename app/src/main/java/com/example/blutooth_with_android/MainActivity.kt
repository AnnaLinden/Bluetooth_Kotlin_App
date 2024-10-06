package com.example.blutooth_with_android

import android.annotation.SuppressLint
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.blutooth_with_android.ui.theme.Blutooth_with_AndroidTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val requiredPermissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.BLUETOOTH_ADMIN)
        }
    }.toTypedArray()

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions granted or denied
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkPermissions()
        setContent {
            Blutooth_with_AndroidTheme {
                Scaffold {innerPadding ->
                    BluetoothScannerScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    private fun checkPermissions() {
        val missingPermissions = requiredPermissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun BluetoothScannerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter = bluetoothManager.adapter
    val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

    var scanning by remember { mutableStateOf(false) }
    val scanResults = remember { mutableStateListOf<ScanResult>() }

    val scanCallback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (!scanResults.any { it.device.address == result.device.address }) {
                    scanResults.add(result)
                }
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Text(text = "Please enable Bluetooth", color = Color.Red)
        } else {
            Button(
                onClick = {
                    if (!scanning) {
                        scanResults.clear()
                        scanning = true
                    }
                },
                enabled = !scanning
            ) {
                Text(text = if (scanning) "Scanning..." else "Start Scanning")
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(scanResults) { result ->
                    val deviceName = result.device.name ?: "Unknown Device"
                    val deviceAddress = result.device.address
                    val rssi = result.rssi
                    val isConnectable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        result.isConnectable
                    } else {
                        true
                    }
                    Text(
                        text = "$deviceName\n$deviceAddress\nRSSI: $rssi",
                        color = if (isConnectable) Color.Black else Color.Gray,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }

    if (scanning && bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
        LaunchedEffect(scanning) {
            // Before starting the scan, check permissions
            val hasPermission = checkPermissions(context)
            if (hasPermission) {
                try {
                    bluetoothLeScanner?.startScan(scanCallback)
                    delay(3000) // Scanning for 3 seconds
                    bluetoothLeScanner?.stopScan(scanCallback)
                } catch (e: SecurityException) {
                    // Handle the exception
                }
            } else {
                // Handle the case where permissions are not granted
            }
            scanning = false
        }
    }
}

// Function to check if permissions are granted
fun checkPermissions(context: Context): Boolean {
    val requiredPermissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.BLUETOOTH_ADMIN)
        }
    }
    return requiredPermissions.all { permission ->
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}
