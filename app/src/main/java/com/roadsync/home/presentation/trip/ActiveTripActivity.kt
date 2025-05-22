package com.roadsync.home.presentation.trip

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.PolyUtil
import com.roadsync.R
import com.roadsync.databinding.ActivityActiveTripBinding
import com.roadsync.home.models.Trip
import com.roadsync.home.presentation.MainActivity
import com.roadsync.home.presentation.MainActivity.Companion.LOCATION_PERMISSION_CODE
import com.roadsync.pref.PreferenceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import kotlin.math.sqrt

class ActiveTripActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityActiveTripBinding
    private val viewModel: TripViewModel by viewModels()
    private lateinit var googleMap: GoogleMap
    private val userMarkers = mutableMapOf<String, Marker>()
    private lateinit var fusedLocationProvider: FusedLocationProviderClient
    private var startLatLng: LatLng? = null
    private var endLatLng: LatLng? = null
    private var routePolyline: Polyline? = null
    private var isTripStarted = false
    private lateinit var tripId: String
    private lateinit var departure: String
    private lateinit var destination: String
    private lateinit var currentUserId: String
    private lateinit var currentUserName: String
    private lateinit var helper: PreferenceHelper
    private var lastDeviationTime: Long = 0
    private val deviationCooldownMillis = 60 * 5000 // 1 minute
    private val apiKey = "AIzaSyAlT_fm5x5NLOan7OWKGMG_taLHhaQ01ko"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityActiveTripBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.map_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        helper = PreferenceHelper.getPref(this)
        // Get trip ID from intent or other source
        tripId = intent.getStringExtra("tripId") ?: return
        departure = intent.getStringExtra("departure") ?: return
        destination = intent.getStringExtra("destination") ?: return
        currentUserId = FirebaseAuth.getInstance().uid ?: return
        currentUserName = helper.getCurrentUser()?.name.toString()
        viewModel.tripId = tripId
        // Initialize FusedLocationProviderClient
        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)


        viewModel.tripStatus.observe(this) { status ->
            Toast.makeText(this, "Trip status: $status", Toast.LENGTH_SHORT).show()
        }

        // Start listening for notifications


        viewModel.listenToTrip(tripId)
        viewModel.startLocationUpdates()
        viewModel.listenForNotifications()

        viewModel.tripData.observe(this) { snapshot ->
            val trip = snapshot?.getValue(Trip::class.java)
            if (trip != null && trip.users.isNotEmpty()) {

                // ❌ Don't clear the whole map (don't use googleMap.clear())

                // Just update the user markers
                trip.users.forEach { (userId, user) ->
                    if (user.lat.toDouble() != 0.0 && user.lon.toDouble() != 0.0) {
                        val position = LatLng(user.lat.toDouble(), user.lon.toDouble())
                        val existingMarker = userMarkers[userId]

                        if (existingMarker == null) {
                            // Add new marker if not exist
                            val icon = getColoredCarBitmapDescriptor(userId)
                            val marker = googleMap.addMarker(
                                MarkerOptions()
                                    .position(position)
                                    .title(user.name ?: "Participant")
                                    .icon(icon)
                            )
                            if (marker != null) {
                                userMarkers[userId] = marker
                            }
                        } else {
                            // Update position of existing marker
                            existingMarker.position = position
                        }
                    }
                }

                Log.d("ActiveTripActivity", "Updated ${trip.users.size} participant markers.")

            } else {
                Log.d("ActiveTripActivity", "Trip is null or has no participants.")
            }
        }



        binding.back.setOnClickListener {
           startActivity(Intent(this@ActiveTripActivity,MainActivity::class.java))
            finish()
        }

        viewModel.userLocation.observe(this) { location ->
            if (startLatLng != null && endLatLng != null) {
                checkDeviation(LatLng(location.latitude, location.longitude))
            }
        }
    }

    private fun checkDeviation(currentLatLng: LatLng) {
        val distance = distanceFromLine(startLatLng!!, endLatLng!!, currentLatLng)

        if (distance > 10) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastDeviationTime > deviationCooldownMillis) {
                lastDeviationTime = currentTime
                Toast.makeText(this, "You are deviating from the route!", Toast.LENGTH_LONG).show()
                viewModel.saveNotificationToFirebase(
                    tripId,
                    FirebaseAuth.getInstance().currentUser?.uid.toString(),
                    "$currentUserName is deviating from route"
                )
            }
        }
    }


    private fun distanceFromLine(start: LatLng, end: LatLng, point: LatLng): Double {
        val line = arrayOf(start, end)

        val a = distanceBetween(point, line[0])
        val b = distanceBetween(point, line[1])
        val c = distanceBetween(line[0], line[1])

        val s = (a + b + c) / 2
        val area = sqrt(s * (s - a) * (s - b) * (s - c))
        return (2 * area) / c
    }

    private fun distanceBetween(p1: LatLng, p2: LatLng): Double {
        val result = FloatArray(1)
        Location.distanceBetween(
            p1.latitude, p1.longitude,
            p2.latitude, p2.longitude,
            result
        )
        return result[0].toDouble()
    }


    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, enable location features
                googleMap.isMyLocationEnabled = true
                viewModel.listenToUserLocations()
            } else {
                // Permission denied, show a message or disable location features
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        viewModel.googleMap = googleMap

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
            viewModel.listenToUserLocations()

            // Fetch user current location
            fusedLocationProvider.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    startLatLng = LatLng(location.latitude, location.longitude)
                    fetchStartAndEndPoints(destination) // Only fetch destination now
                } else {
                    Toast.makeText(this, "Unable to fetch current location", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopLocationUpdates()
    }

    private fun getLatLngFromPlaceName(placeName: String, onResult: (LatLng?) -> Unit) {
        val geocoder = Geocoder(this)
        try {
            val addressList = geocoder.getFromLocationName(placeName, 1)
            if (addressList != null && addressList.isNotEmpty()) {
                val address = addressList[0]
                val latLng = LatLng(address.latitude, address.longitude)
                onResult(latLng)
            } else {
                onResult(null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onResult(null)
        }
    }

    private fun fetchStartAndEndPoints(endPlace: String) {
        getLatLngFromPlaceName(endPlace) { endLatLngResult ->
            if (endLatLngResult != null) {
                Log.d("ActiveTripActivity", "End location found: $endPlace")
                Log.d("ActiveTripActivity", "End location: $endLatLngResult")
                endLatLng = endLatLngResult
                drawRoute(startLatLng!!, endLatLng!!)
            } else {
                Toast.makeText(this, "End location not found!", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun drawRoute(origin: LatLng, destination: LatLng) {
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origin.latitude},${origin.longitude}" +
                "&destination=${destination.latitude},${destination.longitude}" +
                "&key=$apiKey"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = URL(url).readText()

                // 👉 Print full JSON response to log
                println("Directions API Response: $result")

                val jsonObject = JSONObject(result)
                val status = jsonObject.getString("status")

                if (status != "OK") {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ActiveTripActivity,
                            "Error from API: $status",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val routes = jsonObject.getJSONArray("routes")
                if (routes.length() > 0) {
                    val points = routes.getJSONObject(0)
                        .getJSONObject("overview_polyline")
                        .getString("points")
                    val decodedPath = PolyUtil.decode(points)

                    withContext(Dispatchers.Main) {
                        googleMap.addPolyline(
                            PolylineOptions()
                                .addAll(decodedPath)
                                .color(
                                    ContextCompat.getColor(
                                        this@ActiveTripActivity,
                                        R.color.primary_blue
                                    )
                                )
                                .width(10f)
                        )

                        val builder = LatLngBounds.builder()
                        for (point in decodedPath) {
                            builder.include(point)
                        }
                        val bounds = builder.build()
                        val padding = 150
                        googleMap.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(
                                bounds,
                                padding
                            )
                        )
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ActiveTripActivity,
                            "No route found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ActiveTripActivity,
                        "Failed to draw route",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun getColoredCarBitmapDescriptor(userId: String): BitmapDescriptor {
        val baseBitmap = vectorToBitmap(R.drawable.ic_car_top_view)


        // Create a blank bitmap to paint on
        val resultBitmap = Bitmap.createBitmap(
            baseBitmap.width,
            baseBitmap.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(resultBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Generate a unique color from userId
        val hash = userId.hashCode()
        val red = (hash shr 16 and 0xFF)
        val green = (hash shr 8 and 0xFF)
        val blue = (hash and 0xFF)

        paint.colorFilter = PorterDuffColorFilter(Color.rgb(red, green, blue), PorterDuff.Mode.SRC_ATOP)

        // Draw the base bitmap onto the canvas using the color filter
        canvas.drawBitmap(baseBitmap, 0f, 0f, paint)

        return BitmapDescriptorFactory.fromBitmap(resultBitmap)
    }

    private fun vectorToBitmap(drawableId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(this, drawableId)!!
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }


}