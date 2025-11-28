package com.example.assign6_5

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.assign6_5.ui.theme.Assign6_5Theme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    setContent {
                        MapsApp(location)
                    }
                }
            }
        }

        setContent {
            RequestPermissions(fusedLocationClient, locationRequest, locationCallback)
        }
    }
}

@Composable
fun MapsApp(location: Location) {
    if (location != null) {
        MapsScreen(location)
    } else{
        Text("Requesting location permission…", modifier = Modifier.padding(12.dp))
    }
}


@SuppressLint("MissingPermission")
@Composable
fun MapsScreen(location: Location) {
    val context = LocalContext.current
    var addressText by remember { mutableStateOf("") }
    val geocoder = remember { Geocoder(context) }
    var markers by remember { mutableStateOf(listOf<LatLng>()) }


    val userLocation by remember{ mutableStateOf(LatLng(location.latitude, location.longitude))}

    userLocation.let { loc ->
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(loc, 16f)
        }

        LaunchedEffect(loc) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(loc, 16f)
        }

        LaunchedEffect(loc) {
            addressText = "Finding address..."
            CoroutineScope(Dispatchers.IO).launch {
                val addr = runCatching {
                    geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                }.getOrNull()

                addressText =
                    addr?.firstOrNull()?.getAddressLine(0) ?: "Address unavailable. Try again."
            }
        }

        Column {
            Text(
                text = "Address: $addressText",
                modifier = Modifier.padding(40.dp),
                style = MaterialTheme.typography.bodyLarge
            )

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                onMapClick = { clickedLatLng ->
                    markers = markers + clickedLatLng
                }
            ) {
                Marker(state = MarkerState(position = loc), title = "You are here")

                markers.forEach {
                    Marker(state = MarkerState(position = it), title = "Custom Marker")
                }
            }
        }
    } ?: Text("Fetching your location…", Modifier.padding(20.dp))
}


@Composable
fun RequestPermissions(
    fusedLocationClient: FusedLocationProviderClient,
    locationRequest: LocationRequest,
    locationCallback: LocationCallback
) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasLocationPermission = isGranted
        if (isGranted) {
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            } catch (e: SecurityException) {
                Log.e("Location", "Security Exception: ${e.message}")
            }
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            } catch (e: SecurityException) {
                Log.e("Location", "Security Exception: ${e.message}")
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Assign6_5Theme {
        Greeting("Android")
    }
}