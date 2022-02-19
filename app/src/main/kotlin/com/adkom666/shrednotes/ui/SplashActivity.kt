package com.adkom666.shrednotes.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber
import kotlin.time.ExperimentalTime

/**
 * Splash screen.
 */
@ExperimentalCoroutinesApi
@ExperimentalTime
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate: savedInstanceState=$savedInstanceState")
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
