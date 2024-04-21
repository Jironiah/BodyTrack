package com.example.cuerpo_peso.api

import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.coroutines.CoroutineContext

class CRUD():CoroutineScope {
    private val job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    val urlapi = "https://www.googleapis.com/drive/v3/"

    private fun getClient(): OkHttpClient {
        val login = HttpLoggingInterceptor()
        login.level = HttpLoggingInterceptor.Level.BODY

        return OkHttpClient.Builder().addInterceptor(login).build()
    }
    private fun getRetrofit(): Retrofit {
        val gson = GsonBuilder().setLenient().create()

        return Retrofit.Builder().baseUrl(urlapi).client(getClient()).addConverterFactory(
            GsonConverterFactory.create(gson)).build()

    }

//    fun getRutaCotxe(inici: String, final: String): List<List<Double>>?{
//        var resposta: Response<Resposta>? = null
//
//        runBlocking {
//            val corrutina = launch {
//                resposta = getRetrofit().create(ApiLocationService::class.java)
//                    .getRutaCotxe(apikey, inici, final)
//            }
//            corrutina.join()
//        }
//        if (resposta!!.isSuccessful) {
//            return resposta!!.body()!!.features[0].geometry.coordinates
//        }else {
//            return null
//        }
//    }
}