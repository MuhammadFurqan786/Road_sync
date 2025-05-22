package com.roadsync

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import com.roadsync.admin.presentation.DashboardActivity
import com.roadsync.auth.presentation.AuthActivity
import com.roadsync.databinding.ActivitySplashBinding
import com.roadsync.home.presentation.MainActivity
import com.roadsync.pref.PreferenceHelper


class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private lateinit var helper: PreferenceHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        helper = PreferenceHelper.getPref(this)
        val user = helper.getCurrentUser()
        val isLoggedIn = helper.isUserLogin()
        // Start fade-in animationa
        binding.splashLogo.visibility = AppCompatImageView.VISIBLE
        binding.splashLogo.startAnimation(fadeIn)

        // Delay for 2 seconds, then move to MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            if (isLoggedIn) {
                if (user?.userType == "user"){
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }else{
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                }

            } else {
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
            }
        }, 2000)
    }
}
