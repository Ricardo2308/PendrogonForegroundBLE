package com.pendrogon.foregroundblesdk

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pendrogon.foregroundblesdk.ble.ConnectionEventListener
import com.pendrogon.foregroundblesdk.ble.ConnectionManager
import com.pendrogon.foregroundblesdk.ble.ConnectionManager.teardownConnection
import com.pendrogon.foregroundblesdk.ble.toHexString
import com.pendrogon.foregroundblesdk.ble.isReadable
import org.jetbrains.anko.alert
import timber.log.Timber
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList

private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2

class ForegroundBleMain(context: Context, idDevice: String) : AppCompatActivity() {

    private var mContext: Context? = context
    private var idDevice: String = idDevice
    var names = arrayOf("202112055")
    var bandera = 0

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private val bleScanner = bluetoothAdapter.bluetoothLeScanner

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    fun prueba(){
        Log.d("D", idDevice)
    }

    private var isScanning = false

    private val scanResults = mutableListOf<ScanResult>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        title = "Endless Service"
    }

    private val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ENABLE_BLUETOOTH_REQUEST_CODE -> {
                if (resultCode != Activity.RESULT_OK) {
                    promptEnableBluetooth()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
                    requestLocationPermission()
                } else {
                    startBleScan()
                }
            }
        }
    }

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityIfNeeded(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    fun startBleScan() {
        if (ContextCompat.checkSelfPermission(mContext!!, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {
            ConnectionManager.registerListener(connectionEventListener)
            var filters: MutableList<ScanFilter?>? = null
            if (names != null) {
                filters = ArrayList()
                for (name in names) {
                    val filter = ScanFilter.Builder()
                        .setDeviceName(name)
                        .build()
                    filters.add(filter)
                }
            }
            bleScanner.startScan(filters, scanSettings, scanCallback)
            isScanning = true
        } else {
            requestLocationPermission()
        }
    }

    fun startBleScanManual() : String{
        var respuesta = ""
        if (ContextCompat.checkSelfPermission(mContext!!, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {
            ConnectionManager.registerListener(connectionListenerManual)
            var filters: MutableList<ScanFilter?>? = null
            if (names != null) {
                filters = ArrayList()
                for (name in names) {
                    val filter = ScanFilter.Builder()
                        .setDeviceName(name)
                        .build()
                    filters.add(filter)
                }
            }
            bleScanner.startScan(filters, scanSettings, scanCallback)
            isScanning = true
        } else {
            requestLocationPermission()
        }
        if (bandera == 1){
            respuesta = "Success"
        }else if (bandera == 0){
            respuesta = "Error"
        }
        return respuesta
    }

    private fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
        isScanning = false
    }

    private fun requestLocationPermission() {
        if (isLocationPermissionGranted) {
            return
        }
        runOnUiThread {
            requestPermission(
                Manifest.permission.ACCESS_FINE_LOCATION,
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) { // A scan result already exists with the same address
                scanResults[indexQuery] = result
            } else {
                with(result.device) {
                    if (bandera == 1) {
                        teardownConnection(result.device)
                    }
                    Log.d("D","Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                    if (isScanning) {
                        stopBleScan()
                    }
                    ConnectionManager.connect(this, mContext!!)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Timber.e("onScanFailed: code $errorCode")
        }
    }

    fun obtenerLectura(device: BluetoothDevice){
        val characteristics by lazy {
            ConnectionManager.servicesOnDevice(device)?.flatMap { service ->
                service.characteristics ?: listOf()
            } ?: listOf()
        }
        characteristics.map { characteristic ->
            if (characteristic.isReadable()) {
                ConnectionManager.readCharacteristic(device, characteristic)
            }
        }
    }

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onDisconnect = {
                runOnUiThread { startBleScan() }
            }
            onConnectionSetupComplete = { gatt ->
                obtenerLectura(gatt.device)

                onCharacteristicRead = { _, characteristic ->
                    Log.d("Dispositivo",
                        "Lectura de antena ${characteristic.uuid}: " +
                                "${hexToASCII(characteristic.value.toHexString().replace(" ", ""))}"
                    )
                    if (hexToASCII(characteristic.value.toHexString().replace(" ", "")) == idDevice) {
                        with("233341317150396E663544") {
                            if (isNotBlank() && isNotEmpty()) {
                                val bytes = hexToBytes()
                                log("Escritura a la antena ${characteristic.uuid}: ${bytes.toHexString()}")
                                ConnectionManager.writeCharacteristic(
                                    gatt.device,
                                    characteristic,
                                    bytes
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private val connectionListenerManual by lazy {
        ConnectionEventListener().apply {
            onDisconnect = {
                runOnUiThread { Log.d("Mensaje", "Desconectado") }
            }
            onConnectionSetupComplete = { gatt ->
                obtenerLectura(gatt.device)

                onCharacteristicRead = { _, characteristic ->
                    Log.d("D",
                        "Read from ${characteristic.uuid}: " +
                                "${hexToASCII(characteristic.value.toHexString().replace(" ", ""))}"
                    )
                    if (hexToASCII(characteristic.value.toHexString().replace(" ", "")) == idDevice) {
                        bandera = 1
                        with("233341317150396E663544") {
                            if (isNotBlank() && isNotEmpty()) {
                                val bytes = hexToBytes()
                                log("Writing to ${characteristic.uuid}: ${bytes.toHexString()}")
                                ConnectionManager.writeCharacteristic(
                                    gatt.device,
                                    characteristic,
                                    bytes
                                )
                            } else {
                                log("Please enter a hex payload to write to ${characteristic.uuid}")
                            }
                        }
                    }else{
                        teardownConnection(gatt.device)
                    }
                }
            }
        }
    }

    private fun hexToASCII(hexValue: String): String? {
        val output = StringBuilder("")
        var i = 0
        while (i < hexValue.length) {
            val str = hexValue.substring(i, i + 2)
            output.append(str.toInt(16).toChar())
            i += 2
        }
        return output.toString()
    }

    private fun String.hexToBytes() =
        this.chunked(2).map { it.uppercase(Locale.US).toInt(16).toByte() }.toByteArray()

    private fun hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(mContext!!, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(mContext!! as Activity, arrayOf(permission), requestCode)
    }

    fun StartForeground(){
        actionOnService(Actions.START)
    }

    fun StopForeground(){
        actionOnService(Actions.STOP)
    }

    private fun actionOnService(action: Actions) {
        if (getServiceState(mContext!!) == ServiceState.STOPPED && action == Actions.STOP) return
        val serviceIntent = Intent(mContext, EndlessService::class.java)
        serviceIntent.putExtra("inputExtra", idDevice)
        serviceIntent.action = action.name
        ContextCompat.startForegroundService(mContext!!, serviceIntent)
    }
}