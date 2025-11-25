package com.example.assign6_5

import android.Manifest
import android.annotation.SuppressLint
import android.location.Geocoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.assign6_5.ui.theme.Assign6_5Theme
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
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var hasPermission by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                hasPermission = granted
            }

        setContent {
            MapsApp(requestPermissionLauncher, hasPermission)
        }
    }
}

@Composable
fun MapsApp(permissionLauncher: ActivityResultLauncher<String>, hasPermission: Boolean) {
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    if (hasPermission) {
        MapsScreen()
    } else{
        Text("Requesting location permission…")
    }
}


@SuppressLint("MissingPermission")
@Composable
fun MapsScreen() {
    val context = LocalContext.current
    val fused = LocationServices.getFusedLocationProviderClient(context)

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var addressText by remember { mutableStateOf("") }
    val geocoder = remember { Geocoder(context) }

    // Custom markers placed by user
    var markers by remember { mutableStateOf(listOf<LatLng>()) }

    // Get last known location
    LaunchedEffect(Unit) {
        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                userLocation = LatLng(loc.latitude, loc.longitude)

                // Move geocoder work off main thread
                CoroutineScope(Dispatchers.IO).launch {
                    val result = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                    addressText = result?.firstOrNull()?.getAddressLine(0) ?: "Unknown address"
                }
            }
        }
    }



    userLocation?.let { loc ->

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(loc, 16f)
        }

        Column {
            Text(
                text = "Address: $addressText",
                modifier = Modifier.padding(12.dp),
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
                // User location marker
                Marker(
                    state = MarkerState(position = loc),
                    title = "You are here"
                )

                // Custom markers
                markers.forEach {
                    Marker(
                        state = MarkerState(position = it),
                        title = "Custom Marker"
                    )
                }
            }
        }
    } ?: Text("Fetching your location…")
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