package com.javaide.mobile.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.javaide.mobile.BuildConfig

abstract class BaseActivity : AppCompatActivity() {

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        supportActionBar?.subtitle = "v${BuildConfig.VERSION_NAME}"
    }
}
