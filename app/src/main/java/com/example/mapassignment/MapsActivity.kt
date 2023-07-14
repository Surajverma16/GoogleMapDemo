package com.example.mapassignment

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.mapassignment.databinding.ActivityMapsBinding
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var mapsViewModel: MapsViewModel
    private lateinit var mapView: Fragment
    private lateinit var locationManager: LocationManager
    val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = supportFragmentManager.findFragmentById(R.id.map)!!
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        mapsViewModel = ViewModelProvider(this)[MapsViewModel::class.java]
        mapsViewModel.locationManager = locationManager

        val mapFragments = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragments.getMapAsync(this@MapsActivity)

        if (mapsViewModel.checkLocationPermission(this) && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
                mapsViewModel.checkLocationPermission(this)
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0F, locationListener)
        }

        binding.btnMapOptions.setOnClickListener {
            mapsViewModel.mapOptions(this)
        }

        binding.btnCenterLocation.setOnClickListener {
            if (mapsViewModel.checkLocationPermission(this) && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    0L,
                    0F,
                    locationListener
                )
            } else {
                mapsViewModel.enableLocation(this)
                mapsViewModel.checkLocationEnabled(locationManager, this, this)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "notification_channel",
                "channelName",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {

            val runnable = object : Runnable {
                override fun run() {
                    mapsViewModel.getCoordinates(LatLng(location.latitude, location.longitude))
                    handler.postDelayed(this, 1000)
                    Log.d("Current Location", " ${location.longitude} + ${location.latitude}")
                }
            }
            handler.post(runnable)
            locationManager.removeUpdates(this)

            val mapFragment =
                supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this@MapsActivity)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mapsViewModel.mapReady(mMap, this, mapView)
    }


    override fun onResume() {
        super.onResume()
        if (mapsViewModel.checkLocationPermission(this) && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                0L,
                0F,
                locationListener
            )
        }
    }
}