package com.example.mapassignment

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.example.mapassignment.databinding.BottomSheetMapOptionsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.maps.android.SphericalUtil
import java.util.*

class MapsViewModel : ViewModel() {
    private var marker: Marker? = null
    private var isTraffic: Boolean = true
    private lateinit var map: GoogleMap
    private var selectedDefault: Boolean = true
    private var selectedSatellite: Boolean = false
    private var selectedTerrain: Boolean = false
    private var longitude: Double = 0.0
    private var latitude: Double = 0.0
    private var markerLatLng: LatLng? = null
    var locationManager: LocationManager? = null
    val handler = Handler()
    lateinit var runnable: Runnable


    fun checkLocationPermission(activity: Activity): Boolean {

        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return true
    }

    fun enableLocation(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.ACCESS_FINE_LOCATION"
            ), 1
        )
    }

    fun mapReady(mMap: GoogleMap, activity: Activity, mapView: Fragment) {
        map = mMap
        if (checkLocationPermission(activity) && locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                mMap.isMyLocationEnabled = true
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 15f))
                mMap.uiSettings.isMyLocationButtonEnabled = false
        } else {
                mMap.animateCamera(CameraUpdateFactory.zoomBy(0f))
        }
        val locationButton =
            (mapView.view?.findViewById<View>(Integer.parseInt("1"))?.parent as View).findViewById<View>(
                Integer.parseInt("2")
            )
        val rlp = locationButton.layoutParams as (RelativeLayout.LayoutParams)
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
        rlp.setMargins(0, 0, 30, 30)

        val compassButton =
            (mapView.view?.findViewById<View>(Integer.parseInt("1"))?.parent as View).findViewById<View>(
                Integer.parseInt("5")
            )
        val clp = compassButton.layoutParams as (RelativeLayout.LayoutParams)
        clp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 1)
        clp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0)
        clp.setMargins(1000, 450, 1000, 0)
        markLocation(mMap, activity)

    }

    fun markLocation(mMap: GoogleMap, activity: Activity) {
        mMap.setOnMapClickListener { latlog ->
                Handler().run {
                    marker?.remove()
                    markerLatLng = latlog
                    val snippet = String.format(
                        Locale.getDefault(),
                        "Lat: %1\$.5f, Long: %2\$.5f",
                        latlog.latitude,
                        latlog.longitude
                    )
                    marker = mMap.addMarker(
                        MarkerOptions().position(latlog).title(getAddress(latlog, activity))
                            .snippet(snippet)
                    )
                    findDifferenceBetweenPoints(activity)

            }
        }
    }

    private fun getAddress(latLng: LatLng, context: Context): String? {
        return try {
            val list = Geocoder(context).getFromLocation(latLng.latitude, latLng.longitude, 1)
            try {
                list!![0].getAddressLine(0)
            } catch (e: IndexOutOfBoundsException) {
                null
            }
        } catch (e: Exception) {
            e.cause?.message.toString()
        }
    }

    fun getCoordinates(latLng: LatLng) {
        latitude = latLng.latitude
        longitude = latLng.longitude
    }

    fun mapOptions(activity: Activity) {
        val dialog = BottomSheetDialog(activity)
        val bottomBinding = BottomSheetMapOptionsBinding.inflate(activity.layoutInflater)
        dialog.setContentView(bottomBinding.root)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

        if (selectedDefault) {
            bottomBinding.imgNormal.setBackgroundResource(R.drawable.map_options_border)
            bottomBinding.txtNormal.setTextColor(ContextCompat.getColor(activity, R.color.blue))
        } else if (selectedSatellite) {
            bottomBinding.imgSatellite.setBackgroundResource(R.drawable.map_options_border)
            bottomBinding.txtSatellite.setTextColor(ContextCompat.getColor(activity, R.color.blue))
        } else {
            bottomBinding.imgTerrain.setBackgroundResource(R.drawable.map_options_border)
            bottomBinding.txtTerrain.setTextColor(ContextCompat.getColor(activity, R.color.blue))
        }
        if (!isTraffic) {
            bottomBinding.imgTraffic.setBackgroundResource(R.drawable.map_options_border)
            bottomBinding.txtTraffic.setTextColor(ContextCompat.getColor(activity, R.color.blue))
        }

        bottomBinding.txtClose.setOnClickListener {
            dialog.dismiss()
        }
        bottomBinding.imgSatellite.setOnClickListener {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            selectedDefault = false
            selectedSatellite = true
            selectedTerrain = false

            bottomBinding.imgSatellite.setBackgroundResource(R.drawable.map_options_border)
            bottomBinding.txtSatellite.setTextColor(ContextCompat.getColor(activity, R.color.blue))
            bottomBinding.imgTerrain.setBackgroundResource(0)
            bottomBinding.txtTerrain.setTextColor(ContextCompat.getColor(activity, R.color.black))
            bottomBinding.imgNormal.setBackgroundResource(0)
            bottomBinding.txtNormal.setTextColor(ContextCompat.getColor(activity, R.color.black))
        }

        bottomBinding.imgTerrain.setOnClickListener {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN

            selectedDefault = false
            selectedSatellite = false
            selectedTerrain = true

            bottomBinding.imgTerrain.setBackgroundResource(R.drawable.map_options_border)
            bottomBinding.txtTerrain.setTextColor(ContextCompat.getColor(activity, R.color.blue))
            bottomBinding.imgSatellite.setBackgroundResource(0)
            bottomBinding.txtSatellite.setTextColor(ContextCompat.getColor(activity, R.color.black))
            bottomBinding.imgNormal.setBackgroundResource(0)
            bottomBinding.txtNormal.setTextColor(ContextCompat.getColor(activity, R.color.black))

        }
        bottomBinding.imgNormal.setOnClickListener {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            selectedDefault = true
            selectedSatellite = false
            selectedTerrain = false

            bottomBinding.imgNormal.setBackgroundResource(R.drawable.map_options_border)
            bottomBinding.txtNormal.setTextColor(ContextCompat.getColor(activity, R.color.blue))
            bottomBinding.imgTerrain.setBackgroundResource(0)
            bottomBinding.txtTerrain.setTextColor(ContextCompat.getColor(activity, R.color.black))
            bottomBinding.imgSatellite.setBackgroundResource(0)
            bottomBinding.txtSatellite.setTextColor(ContextCompat.getColor(activity, R.color.black))
        }

        bottomBinding.imgTraffic.setOnClickListener {
            if (isTraffic) {
                map.isTrafficEnabled = true
                isTraffic = false
                it.setBackgroundResource(R.drawable.map_options_border)
            } else {
                map.isTrafficEnabled = false
                isTraffic = true
                it.setBackgroundResource(0)
            }
        }
        dialog.show()
    }

    private fun findDifferenceBetweenPoints(context: Activity) {
        runnable = object : Runnable {
            override fun run() {
                val distanceInMeter =
                    SphericalUtil.computeDistanceBetween(LatLng(latitude, longitude), markerLatLng)
                Log.d("Distance", "findDifferenceBetweenPoints: $distanceInMeter")
                handler.postDelayed(this, 2000)

                if (distanceInMeter < 500) {
                    val notificationManager = NotificationManagerCompat.from(context)

                    val notification = NotificationCompat.Builder(context, "notification_channel")
                        .setContentTitle("Near by")
                        .setContentText("You are in 500 meter radius of Dropped Pin")
                        .setSmallIcon(R.drawable.baseline_location_on_24)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .build()

                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            context,
                            arrayOf("android.permission.POST_NOTIFICATIONS"),
                            1
                        )
                        return
                    }
                    notificationManager.notify(911, notification)

                    handler.removeCallbacks(this)
                }
            }
        }
        handler.post(runnable)
    }

    fun checkLocationEnabled(
        locationManager: LocationManager,
        context: Context,
        activity: Activity,

        ) {
        var network_enabled = false
        var gpsEnabled = false

        try {
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: java.lang.Exception) {
            Log.e("checkLocationEnabled: ", ex.message.toString())
        }

        try {
            network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (ex: Exception) {
            Log.e("checkLocationEnabled: ", ex.message.toString())
        }

        if (!network_enabled && !gpsEnabled) {
            val dialog = Dialog(context)
            val dialogView = activity.layoutInflater.inflate(R.layout.location_permission, null)
            dialog.setCancelable(false)
            dialog.setContentView(dialogView)
            dialogView.findViewById<TextView>(R.id.txtCancel).setOnClickListener {
                dialog.dismiss()
            }
            dialogView.findViewById<TextView>(R.id.txtOpenSettings).setOnClickListener {
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                dialog.dismiss()
            }
            dialog.show()
        }
    }
}