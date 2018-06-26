package com.ifman.erby.Common

import com.ifman.erby.Remote.IGoogleAPIServices
import com.ifman.erby.Remote.RetrofitClient

object Common {
    private val GOOGLE_API_URL="https://maps.googleapis.com/"

    val googleApiService:IGoogleAPIServices
        get() = RetrofitClient.getClient(GOOGLE_API_URL).create(IGoogleAPIServices::class.java)
}