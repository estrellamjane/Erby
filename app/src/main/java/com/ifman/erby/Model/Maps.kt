package com.ifman.erby.Model

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
import com.ifman.erby.Remote.IGoogleAPIServices
import kotlinx.android.synthetic.main.activity_maps.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class Maps{

    //Map Variable
    private lateinit var mMap: GoogleMap

    //Google API Services
    lateinit var mService: IGoogleAPIServices

    //Current Place
    internal lateinit var currentPlace: MyPlaces

    //Get nearby places
    private fun nearbyPlace(typePlace: String, latLng: LatLng):GoogleMap{
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
                                        //.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_hotel))
                                markerOptions.snippet(i.toString())//Assign index for Marker

                                //Add Marker to Map
                                mMap.addMarker(markerOptions)
                            }
                        }
                    }

                    //Request Failure
                    override fun onFailure(call: Call<MyPlaces>?, t: Throwable?) {
                        //Toast.makeText(baseContext,""+t!!.message, Toast.LENGTH_SHORT).show()
                    }
                })

        //Move Camera
        mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
        mMap!!.animateCamera(CameraUpdateFactory.zoomTo(15f))
        return mMap
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
    private fun searchMap(geocoder:Geocoder,location:String):GoogleMap{

        //Init Search Location and Address Result List
        var addressList : List<Address>? = null

        //Init Marker
        val mOptions = MarkerOptions()

        if(location != ""){
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
            return mMap
        }else{
            return mMap
        }
    }
}