package com.ifman.erby.Controller

import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.SearchView
import android.widget.Toast
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.ifman.erby.Common.Common
import com.ifman.erby.Places.MyPlaces
import com.ifman.erby.R
import com.ifman.erby.Remote.IGoogleAPIServices
import kotlinx.android.synthetic.main.activity_maps.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    //Map Variable
    private lateinit var mMap: GoogleMap
    //Latitude and Longitude
    private var latitude:Double=0.toDouble()
    private var longitude:Double=0.toDouble()
    //Last Location and Marker Variable
    private lateinit var mLastLocation: Location
    private var mMarker: Marker?=null

    //Location Variables
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest
    lateinit var locationCallback: LocationCallback

    companion object {
        private const val MY_PERMISSION_CODE: Int = 1000
    }

    //Google API Services
    lateinit var mService: IGoogleAPIServices

    //Current Place
    internal lateinit var currentPlace: MyPlaces

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //Init Google API Service
        mService= Common.googleApiService

        //Request runtime permission
        runtimePermission()

        //Set Search Listener
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{

            //On Search Submit
            override fun onQueryTextSubmit(query: String): Boolean {
                //Call Search Function
                searchMap()
                return false
            }

            //On Typing Search
            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })
    }

    //Request runtime permission
    private fun runtimePermission(){
        //Location Request for Marshmallow and Above
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Checks if the location permission is granted
            if(checkLocationPermission()) {
                //Call Location Request and Callback
                buildLocationRequest()
                buildLocationCallback()

                //Init Location Provider Client
                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
            }
        }
        //Location Request for Lollipop and Below
        else{
            //Call Location Request and Callback
            buildLocationRequest()
            buildLocationCallback()

            //Init Location Provider Client
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
        }
    }

    //Check for Location Permission
    private fun checkLocationPermission():Boolean {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION))
                ActivityCompat.requestPermissions(this, arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                ), MY_PERMISSION_CODE)
            else
                ActivityCompat.requestPermissions(this, arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                ), MY_PERMISSION_CODE)
            return false
        }
        else
            return true
    }

    //Request Location Permission
    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 3000
        locationRequest.smallestDisplacement = 10f
    }

    //Get Current Location
    private fun buildLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?){
                mLastLocation = p0!!.locations.get(p0!!.locations.size-1) //Get Last Location

                if(mMarker != null){
                    mMarker!!.remove()
                }

                latitude = mLastLocation.latitude
                longitude = mLastLocation.longitude

                val latLng = LatLng(latitude,longitude)

                //Move Camera
                mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                mMap!!.animateCamera(CameraUpdateFactory.zoomTo(15f))
            }
        }
    }

    //On Request Permission Result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode)
        {
            MY_PERMISSION_CODE ->{
                if(grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                        if(checkLocationPermission()) {
                            buildLocationRequest()
                            buildLocationCallback()

                            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
                            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())

                            mMap!!.isMyLocationEnabled=true
                        }
                }
                else
                {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //Stops the Location Updates
    override fun onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onStop()
    }

    //Get nearby places
    private fun nearbyPlace(typePlace: String, latLng: LatLng){
        //Clear All Marker on Map
        mMap.clear()

        //Init Marker
        val markerOptions = MarkerOptions()
                .position(latLng)
                .title("Place of Interest")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))

        //Add Marker to Map
        mMap!!.addMarker(markerOptions)

        //Build URL Request base on location
        val url=getUrl(latLng.latitude,latLng.longitude,typePlace)

        //get Nearby Places using Google API Services
        mService.getNearbyPlaces(url)
                .enqueue(object: Callback<MyPlaces> {

                    //Request Success
                    override fun onResponse(call: Call<MyPlaces>?, response: Response<MyPlaces>?) {
                        currentPlace = response!!.body()!!

                        if(response!!.isSuccessful){
                            var latLng1: LatLng
                            //Add all nearby places to the Map
                            for(i in 0 until response!!.body()!!.results!!.size){
                                val googlePlace = response.body()!!.results!![i]
                                val lat = googlePlace.geometry!!.location!!.lat
                                val lng = googlePlace.geometry!!.location!!.lng
                                val placeName = googlePlace.name
                                latLng1 = LatLng(lat, lng)
                                val markerOptions = MarkerOptions()
                                        .position(latLng1)
                                        .title(placeName)
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_hotel))
                                markerOptions.snippet(i.toString())//Assign index for Marker

                                //Add Marker to Map
                                mMap.addMarker(markerOptions)
                            }
                        }
                    }

                    //Request Failure
                    override fun onFailure(call: Call<MyPlaces>?, t: Throwable?) {
                        Toast.makeText(baseContext,""+t!!.message, Toast.LENGTH_SHORT).show()
                    }
                })

        //Move Camera
        mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
        mMap!!.animateCamera(CameraUpdateFactory.zoomTo(15f))
    }

    //Get result for nearby places
    private fun getUrl(latitude: Double, longitude: Double, typePlace: String?): String {
        val googlePlaceUrl = StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json")
        googlePlaceUrl.append("?location=$latitude,$longitude")
        googlePlaceUrl.append("&radius=500") //500 Meters
        googlePlaceUrl.append("&type=$typePlace")
        googlePlaceUrl.append("&key=AIzaSyBDJ36C01RYXn416H71DRTwC-3oBlD3rGI")

        Log.d("URL_DEBUG", googlePlaceUrl.toString())
        return googlePlaceUrl.toString()
    }

    //Search Location based on Address
    private fun searchMap(){
        //Clear All Marker on Map
        mMap.clear()

        //Init Search Location and Address Result List
        val location = searchView.query.toString()
        var addressList : List<Address>? = null

        //Init Marker
        val mOptions = MarkerOptions()

        if(location != ""){
            //Init Geocoder
            val geocoder = Geocoder(this)

            try {
                //Retrieve Address Results
                addressList = geocoder.getFromLocationName(location, 5)
            }catch (e : IOException){
                e.printStackTrace()
            }

            //Adds Markers for Address Result List
            for(i in addressList!!.indices){
                val address = addressList[i]
                val latLng = LatLng(address.latitude, address.longitude)
                mOptions.position(latLng)
                mMap!!.addMarker(mOptions)
                //Move Camera
                mMap!!.animateCamera(CameraUpdateFactory.newLatLng(latLng))
            }
        }
    }

    //On Map Startup
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.setPadding(0,170,0,0)

        //Map Long Click Listener
        mMap!!.setOnMapLongClickListener {
            //Call Nearby Place Request
            nearbyPlace("rent", it)
        }

        //Map Click Listener
        mMap!!.setOnMapClickListener {
            //Clear Markers
            mMap.clear()
        }

        //Check if MyLocation is Enabled
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap!!.isMyLocationEnabled = true
            }
        }
        else
            mMap!!.isMyLocationEnabled = true


        //Enable Zoom Control and Compass on Map
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true

    }
}
