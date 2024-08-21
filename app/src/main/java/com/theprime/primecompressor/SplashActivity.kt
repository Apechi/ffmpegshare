package com.theprime.primecompressor

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.adsmedia.adsmodul.AdsHelper
import com.adsmedia.adsmodul.OpenAds
import com.adsmedia.adsmodul.utils.AdsConfig
import com.theprime.primecompressor.configs.AdsData
import com.theprime.primecompressor.utils.Settings

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)



        if (BuildConfig.DEBUG) {
            AdsHelper.debugModePrime(true)
        }
        AdsHelper.initializeAdsPrime(this, BuildConfig.APPLICATION_ID, AdsConfig.Game_App_ID)
        AdsData.getIDAds();
        OpenAds.LoadOpenAds(AdsConfig.Open_App_ID)
        OpenAds.AppOpenAdManager.showAdIfAvailable(this) {
            val intent = Intent(this, MainActivity::class.java)
            object : CountDownTimer(3000, 1000) {
                override fun onFinish() {
                    startActivity(intent)
                    finish()
                }
                override fun onTick(millisUntilFinished: Long) {
                }
            }.start()
        }
    }
}