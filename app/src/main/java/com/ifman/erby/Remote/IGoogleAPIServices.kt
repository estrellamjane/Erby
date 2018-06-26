package com.ifman.erby.Remote

import com.ifman.erby.Places.MyPlaces
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

interface IGoogleAPIServices {
    @GET
    fun getNearbyPlaces(@Url url: String):Call<MyPlaces>
}