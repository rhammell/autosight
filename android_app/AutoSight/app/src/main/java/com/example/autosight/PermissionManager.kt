package com.example.autosight

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow

class PermissionManager(private val activity: ComponentActivity) {

    private val _cameraPermissionState = MutableStateFlow(false)
    val cameraPermissionState: StateFlow<Boolean> = _cameraPermissionState

    private val _locationPermissionState = MutableStateFlow(false)
    val locationPermissionState: StateFlow<Boolean> = _locationPermissionState

    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var requestLocationPermissionLauncher: ActivityResultLauncher<String>

    init {
        registerPermissionLaunchers()
        checkInitialPermissions()
    }

    private fun registerPermissionLaunchers() {
        requestCameraPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            updateCameraPermissionState(isGranted)
        }

        requestLocationPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            updateLocationPermissionState(isGranted)
        }
    }

    private fun checkInitialPermissions() {
        updateCameraPermissionState(checkCameraPermission())
        updateLocationPermissionState(checkLocationPermission())
    }

    fun checkAndRequestCameraPermission() {
        when {
            checkCameraPermission() -> updateCameraPermissionState(true)
            activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showCameraPermissionRationale()
            }
            else -> requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun checkAndRequestLocationPermission() {
        when {
            checkLocationPermission() -> updateLocationPermissionState(true)
            activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showLocationPermissionRationale()
            }
            else -> requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun updateCameraPermissionState(granted: Boolean) {
        _cameraPermissionState.value = granted
    }

    private fun updateLocationPermissionState(granted: Boolean) {
        _locationPermissionState.value = granted
    }

    private fun showCameraPermissionRationale() {
        // Implement your rationale UI here
        // For simplicity, we'll just request the permission again
        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun showLocationPermissionRationale() {
        // Implement your rationale UI here
        // For simplicity, we'll just request the permission again
        requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}