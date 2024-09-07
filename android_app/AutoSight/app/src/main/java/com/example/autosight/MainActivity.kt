package com.example.autosight

import AppConfig
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.autosight.ui.theme.AutoSightTheme
import com.example.autosight.ui.theme.DarkColor
import com.example.autosight.ui.theme.BlueColor
import com.example.autosight.ui.theme.TanColor
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import com.google.gson.Gson
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import android.content.pm.ActivityInfo
import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DriveEta
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.util.Locale
import java.math.BigDecimal
import java.math.RoundingMode
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration

class MainActivity : ComponentActivity() {
    private lateinit var devicePrivateKey: String
    private lateinit var cameraManager: CameraManager
    private lateinit var permissionManager: PermissionManager

    private val retrofit = Retrofit.Builder()
        .baseUrl(AppConfig.IMAGE_SERVER_ADDRESS)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val imageApiService = retrofit.create(ImageApiService::class.java)

    private val metagraphL0Retrofit = Retrofit.Builder()
        .baseUrl(AppConfig.METAGRAPH_L0_ADDRESS)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val metagraphL0ApiService = metagraphL0Retrofit.create(MetagraphL0ApiService::class.java)

    private val metagraphDataL1Retrofit = Retrofit.Builder()
        .baseUrl(AppConfig.METAGRAPH_DATA_L1_ADDRESS)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val metagraphDataL1ApiService = metagraphDataL1Retrofit.create(MetagraphDataL1ApiService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize managers
        cameraManager = CameraManager(this)
        permissionManager = PermissionManager(this)

        // Generate or retrieve the private key
        devicePrivateKey = CryptoUtils.generateOrRetrievePrivateKey(this)

        setContent {
            AutoSightTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AutoSightApp(
                        imageApiService = imageApiService,
                        metagraphL0ApiService = metagraphL0ApiService,
                        metagraphDataL1ApiService = metagraphDataL1ApiService,
                        devicePrivateKey = devicePrivateKey,
                        cameraManager = cameraManager,
                        permissionManager = permissionManager
                    )
                }
            }
        }
    }
}

@Composable
fun AutoSightApp(
    imageApiService: ImageApiService,
    metagraphL0ApiService: MetagraphL0ApiService,
    metagraphDataL1ApiService: MetagraphDataL1ApiService,
    devicePrivateKey: String,
    cameraManager: CameraManager,
    permissionManager: PermissionManager
) {
    var currentScreen by rememberSaveable { mutableStateOf(Screen.Opening) }
    var walletAddress by rememberSaveable { mutableStateOf("") }

    val cameraPermissionState by permissionManager.cameraPermissionState.collectAsState()
    val locationPermissionState by permissionManager.locationPermissionState.collectAsState()

    BackHandler(enabled = currentScreen != Screen.Opening) {
        currentScreen = when (currentScreen) {
            Screen.Home -> Screen.Opening
            Screen.Permissions -> Screen.Home
            Screen.Landscape -> if (cameraPermissionState && locationPermissionState) {
                Screen.Home
            } else {
                Screen.Permissions
            }
            else -> currentScreen
        }
    }

    when (currentScreen) {
        Screen.Opening -> {
            OpeningPage { address ->
                walletAddress = address
                currentScreen = Screen.Home
            }
        }
        Screen.Home -> {
            HomePage(
                walletAddress = walletAddress,
                metagraphL0ApiService = metagraphL0ApiService,
                onNewTripClick = {
                    currentScreen = if (cameraPermissionState && locationPermissionState) {
                        Screen.Landscape
                    } else {
                        Screen.Permissions
                    }
                }
            )
        }
        Screen.Permissions -> {
            PermissionsScreen(
                cameraPermissionGranted = cameraPermissionState,
                locationPermissionGranted = locationPermissionState,
                onEnableCameraClick = {
                    permissionManager.checkAndRequestCameraPermission()
                },
                onEnableLocationClick = {
                    permissionManager.checkAndRequestLocationPermission()
                }
            )
            // Check if permissions are granted after showing the PermissionsScreen
            if (cameraPermissionState && locationPermissionState) {
                currentScreen = Screen.Landscape
            }
        }
        Screen.Landscape -> {
            LandscapeAutoSightScreen(
                imageApiService,
                metagraphDataL1ApiService,
                devicePrivateKey,
                cameraManager,
                walletAddress
            )
        }
    }
}

@Composable
fun OpeningPage(onContinue: (String) -> Unit) {
    var walletInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    fun validateWalletAddress(address: String): Boolean {
        return address.startsWith("DAG") && address.length == 40
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Auto",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 68.sp,  // Increased from 48.sp
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold  // Added bold
                ),
                color = DarkColor,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Sight",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 68.sp,  // Increased from 48.sp
                    fontWeight = FontWeight.Bold  // Added bold
                ),
                color = TanColor,
                textAlign = TextAlign.Center,
            )
        }

        Text(
            text = "Earn crypto while you drive",
            style = MaterialTheme.typography.titleMedium,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 60 .dp)
        )

        OutlinedTextField(
            value = walletInput,
            onValueChange = {
                walletInput = it
                isError = false
                errorMessage = ""
            },
            label = { Text("Enter your wallet address") },
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        if (isError) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = {
                if (validateWalletAddress(walletInput)) {
                    onContinue(walletInput)
                } else {
                    isError = true
                    errorMessage = "Invalid wallet address. Valid addresses can be obtained from the Stargazer Wallet App."
                }
            },
            enabled = walletInput.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (walletInput.isNotBlank()) BlueColor else MaterialTheme.colorScheme.secondary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}

@Composable
fun HomePage(
    walletAddress: String,
    metagraphL0ApiService: MetagraphL0ApiService,
    onNewTripClick: () -> Unit
) {
    var rewards by remember { mutableStateOf<BigInteger?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(walletAddress) {
        try {
            val response = metagraphL0ApiService.getRewards(walletAddress)
            if (response.isSuccessful) {
                rewards = response.body()?.rewards
            } else {
                throw Exception("Failed to retrieve rewards data")
            }
        } catch (e: Exception) {
            errorMessage = e.message
            rewards = BigInteger.ZERO
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isLandscape) 8.dp else 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Auto",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = if (isLandscape) 30.sp else 40.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold
                ),
                color = DarkColor
            )
            Text(
                text = "Sight",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = if (isLandscape) 30.sp else 40.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TanColor
            )
        }

        // Centered content
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wallet Address
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${walletAddress.take(5)}...${walletAddress.takeLast(5)}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Wallet Address",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                // Rewards Display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AutoResizingText(
                        text = rewards?.let { formatReward(it) } ?: "-",
                        maxFontSize = 80.sp,
                        color = if (errorMessage == null) DarkColor else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(200.dp)
                    )
                    Text(
                        text = "Lifetime AUTO rewards",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Wallet Address
                Text(
                    text = "${walletAddress.take(5)}...${walletAddress.takeLast(5)}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 30.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 18.dp)
                )

                // Rewards Display
                AutoResizingText(
                    text = rewards?.let { formatReward(it) } ?: "-",
                    maxFontSize = 130.sp,
                    color = if (errorMessage == null) DarkColor else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Lifetime AUTO rewards",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 20.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        if (errorMessage != null) {
            Text(
                text = "Metagraph connection failed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // New Trip Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (isLandscape) 8.dp else 16.dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingActionButton(
                onClick = onNewTripClick,
                containerColor = BlueColor,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DriveEta,
                        contentDescription = "Car Icon",
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Earn AUTO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AutoResizingText(
    text: String,
    maxFontSize: TextUnit,
    color: Color,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier
) {
    var fontSize by remember { mutableStateOf(maxFontSize) }

    Text(
        text = text,
        color = color,
        fontWeight = fontWeight,
        fontSize = fontSize,
        softWrap = false,
        textAlign = TextAlign.Center,
        modifier = modifier,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.didOverflowWidth) {
                fontSize = fontSize.times(0.9f)
            }
        }
    )
}

@Composable
fun PermissionsScreen(
    cameraPermissionGranted: Boolean,
    locationPermissionGranted: Boolean,
    onEnableCameraClick: () -> Unit,
    onEnableLocationClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Camera and location services\n must be enabled",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = BlueColor
            ),
            onClick = onEnableCameraClick,
            enabled = !cameraPermissionGranted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Enable Camera")
        }
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = BlueColor
            ),
            onClick = onEnableLocationClick,
            enabled = !locationPermissionGranted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Enable Location")
        }
    }
}

@Composable
fun LandscapeAutoSightScreen(
    imageApiService: ImageApiService,
    metagraphDataL1ApiService: MetagraphDataL1ApiService,
    devicePrivateKey: String,
    cameraManager: CameraManager,
    walletAddress: String
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    AutoSightScreen(imageApiService, metagraphDataL1ApiService, devicePrivateKey, cameraManager, walletAddress)
}

@SuppressLint("MissingPermission")
@Composable
fun AutoSightScreen(
    imageApiService: ImageApiService,
    metagraphDataL1ApiService: MetagraphDataL1ApiService,
    devicePrivateKey: String,
    cameraManager: CameraManager,
    walletAddress: String
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isCapturing by remember { mutableStateOf(false) }
    var lastLocation by remember { mutableStateOf<Location?>(null) }
    var isCameraPositionConfirmed by remember { mutableStateOf(false) }
    var autoEarned by remember { mutableIntStateOf(0) }
    var lastCaptureStatus by remember { mutableStateOf(CaptureStatus.Success) }
    var captureProgress by remember { mutableFloatStateOf(1f) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationRequest = remember {
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(500)
            .build()
    }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                lastLocation = locationResult.lastLocation
            }
        }
    }

    DisposableEffect(Unit) {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    LaunchedEffect(isCapturing) {
        while (isCapturing)  {
            for (i in 150 downTo 0) {
                captureProgress = i / 150f
                delay(100)
            }
            val currentLocation = lastLocation
            val result = captureAndUploadImage(imageApiService, metagraphDataL1ApiService, currentLocation, devicePrivateKey, cameraManager, walletAddress)
            when (result) {
                is CaptureResult.Success -> {
                    autoEarned += 10
                    lastCaptureStatus = CaptureStatus.Success
                }
                is CaptureResult.ImageUploadFailed -> {
                    lastCaptureStatus = CaptureStatus.ImageUploadFailed
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Image upload failed")
                    }
                }
                is CaptureResult.MetagraphDataUploadFailed -> {
                    lastCaptureStatus = CaptureStatus.MetagraphDataUploadFailed
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Metagraph data upload failed")
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Camera Preview
            Box(modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }.also { previewView ->
                            cameraManager.setupCamera(lifecycleOwner, previewView)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Input Area
            Column(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isCameraPositionConfirmed) {
                    Text(
                        text = "Position the phone on the vehicle dashboard, with the camera centered on the road ahead.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BlueColor
                        ),
                        onClick = { isCameraPositionConfirmed = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text("Confirm Camera Position")
                    }
                } else {
                    Text(
                        text = "${walletAddress.take(6)}...${walletAddress.takeLast(4)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // AUTO Earned Counter with status
                    AutoResizingText(
                        text = autoEarned.toString(),
                        maxFontSize = 95.sp,
                        color = when (lastCaptureStatus) {
                            CaptureStatus.Success -> DarkColor
                            else -> MaterialTheme.colorScheme.error
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = when (lastCaptureStatus) {
                            CaptureStatus.Success -> "AUTO earned this drive"
                            CaptureStatus.ImageUploadFailed -> "Last image upload failed"
                            CaptureStatus.MetagraphDataUploadFailed -> "Last data upload failed"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = when (lastCaptureStatus) {
                            CaptureStatus.Success -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (isCapturing) {
                        CaptureProgressIndicator(captureProgress)
                    }

                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BlueColor
                        ),
                        onClick = {
                            if (!isCapturing) {
                                autoEarned = 0
                                lastCaptureStatus = CaptureStatus.Success
                            }
                            isCapturing = !isCapturing
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(if (isCapturing) "Stop Capture" else "Begin Capture")
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
fun CaptureProgressIndicator(progress: Float) {
    LinearProgressIndicator(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .padding(bottom = 8.dp),
        progress = { progress },
        color = BlueColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

suspend fun captureAndUploadImage(
    imageApiService: ImageApiService,
    metagraphDataL1ApiService: MetagraphDataL1ApiService,
    location: Location?,
    devicePrivateKey: String,
    cameraManager: CameraManager,
    walletAddress: String
): CaptureResult {
    val file = cameraManager.captureImage() ?: return CaptureResult.ImageUploadFailed
    val captureTime = System.currentTimeMillis()
    val uploadResponse = uploadImage(file, imageApiService)
    return uploadResponse?.let {
        val fullImageURL = "${AppConfig.IMAGE_SERVER_ADDRESS}/${it.path}"
        val captureData = generateCaptureData(
            captureTime = captureTime,
            imageURL = fullImageURL,
            latitude = location?.latitude?.toCoordinateString() ?: "0.000000",
            longitude = location?.longitude?.toCoordinateString() ?: "0.000000",
            rewardAddress = walletAddress,
            devicePrivateKey = devicePrivateKey
        )
        val postResult = postCaptureData(captureData, metagraphDataL1ApiService)
        if (postResult) CaptureResult.Success else CaptureResult.MetagraphDataUploadFailed
    } ?: CaptureResult.ImageUploadFailed
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

private suspend fun uploadImage(file: File, imageApiService: ImageApiService): UploadResponse? {
    return try {
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)
        val response: Response<UploadResponse> = imageApiService.uploadImage(body)
        if (response.isSuccessful) {
            response.body()?.also {
                Log.d("ImageUpload", "Image upload successful. Path: ${it.path}")
            }
        } else {
            Log.e("ImageUpload", "Image upload failed: ${response.code()}")
            null
        }
    } catch (e: Exception) {
        Log.e("ImageUpload", "Error uploading image", e)
        null
    }
}

suspend fun postCaptureData(captureData: CaptureData, metagraphDataL1ApiService: MetagraphDataL1ApiService): Boolean {
    return try {
        val response = metagraphDataL1ApiService.postData(captureData)
        if (response.isSuccessful) {
            Log.d("DataPost", "Data posted successfully")
            true
        } else {
            Log.e("DataPost", "Data post failed: ${response.code()}")
            false
        }
    } catch (e: Exception) {
        Log.e("DataPost", "Error posting data", e)
        false
    }
}

fun Context.createImageFile(): File {
    val storageDir = filesDir
    return File.createTempFile(
        "JPEG_${System.currentTimeMillis()}_",
        ".jpg",
        storageDir
    )
}

fun Double.toCoordinateString(): String = String.format(Locale.US, "%.6f", this)

fun generateCaptureData(
    captureTime: Long,
    imageURL: String,
    latitude: String,
    longitude: String,
    rewardAddress: String,
    devicePrivateKey: String
): CaptureData {
    val captureValue = CaptureValue(captureTime, imageURL, latitude, longitude, rewardAddress)

    Log.d("Signing", "PrivateKey: $devicePrivateKey")

    // Encode the capture value
    val gson = Gson()
    val encoded = gson.toJson(captureValue)
    Log.d("Signing", "Encoded: $encoded")

    // Serialize the encoded data
    val serialized = CryptoUtils.serializeMessage(encoded)
    Log.d("Signing", "Serialized: $serialized")

    // Calculate SHA256 hash
    val hash = CryptoUtils.calculateSHA256(serialized)
    Log.d("Signing", "256 Hash: $hash")

    // Calculate SHA512 hash
    val sha512Hash = CryptoUtils.calculateSHA512(hash)
    Log.d("Signing", "512 Hash: $sha512Hash")

    // Create signature
    val signature = CryptoUtils.createSignature(sha512Hash, devicePrivateKey)
    Log.d("Signing", "Signature: $signature")

    // Derive public key
    val publicKey = CryptoUtils.derivePublicKey(devicePrivateKey)
    Log.d("Signing", "Public key: $publicKey")

    // Build capture data
    val proof = Proof(id = publicKey, signature = signature)
    val captureData = CaptureData(value = captureValue, proofs = listOf(proof))
    Log.d("Signing", "CaptureData: $captureData.toString()")

    return captureData
}

fun formatReward(reward: BigInteger): String {
    val divisor = BigDecimal.TEN.pow(8) // 10^8
    val rewardDecimal = BigDecimal(reward)
    val result = rewardDecimal.divide(divisor, 0, RoundingMode.DOWN)
    return result.toBigInteger().toString()
}

data class CaptureData(
    val value: CaptureValue,
    val proofs: List<Proof>
)

data class CaptureValue(
    val captureTime: Long,
    val imageURL: String,
    val latitude:  String,
    val longitude: String,
    val rewardAddress: String
)

data class Proof(
    val id: String,
    val signature: String
)

sealed class CaptureResult {
    data object Success : CaptureResult()
    data object ImageUploadFailed : CaptureResult()
    data object MetagraphDataUploadFailed : CaptureResult()
}

enum class CaptureStatus {
    Success, ImageUploadFailed, MetagraphDataUploadFailed
}

enum class Screen {
    Opening, Home, Permissions, Landscape
}