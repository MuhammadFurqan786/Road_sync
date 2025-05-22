package com.roadsync.admin.presentation

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.roadsync.R
import com.roadsync.auth.model.User
import com.roadsync.auth.presentation.AuthActivity
import com.roadsync.databinding.ActivityDashboardBinding
import com.roadsync.pref.PreferenceHelper


class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    var drawerLayout: DrawerLayout? = null
    var contentView: CoordinatorLayout? = null
    private lateinit var helper: PreferenceHelper
    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Setup toolbar and custom navigation icon

        helper = PreferenceHelper.getPref(this)
        user = helper.getCurrentUser()


        // DrawerLayout and NavigationView setup
        drawerLayout = binding.drawerLayout
        contentView = binding.appBarMain.main

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val halfWidth = (screenWidth * 0.6).toInt()

        val navView: NavigationView = binding.navView

        val params = binding.navView.layoutParams
        params.width = halfWidth
        binding.navView.layoutParams = params
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Setup AppBarConfiguration
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_users,
                R.id.nav_trips,
                R.id.nav_logout
            ), drawerLayout
        )

        setSupportActionBar(binding.appBarMain.toolbar)


        // Set title text color
        binding.appBarMain.toolbar.setNavigationIcon(R.drawable.ic_burger)

        binding.appBarMain.toolbar.setupWithNavController(navController, appBarConfiguration)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Setup the rest of your navigation
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        drawerLayout!!.setScrimColor(Color.TRANSPARENT)
        drawerLayout!!.addDrawerListener(object : SimpleDrawerListener() {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                // Scale the View based on current slide offset

                val diffScaledOffset = slideOffset * (1 - END_SCALE)
                val offsetScale = 1 - diffScaledOffset
                contentView!!.scaleX = offsetScale
                contentView!!.scaleY = offsetScale

                // Translate the View, accounting for the scaled width
                val xOffset = drawerView.width * slideOffset
                val xOffsetDiff = contentView!!.width * diffScaledOffset / 2
                val xTranslation = xOffset - xOffsetDiff
                contentView!!.translationX = xTranslation
            }

            override fun onDrawerClosed(drawerView: View) {
            }
        }
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (appBarConfiguration.topLevelDestinations.contains(destination.id)) {
                binding.appBarMain.toolbar.setNavigationIcon(R.drawable.ic_burger)
            } else {
                binding.appBarMain.toolbar.setNavigationIcon(R.drawable.line_md_arrow_left)
            }
        }

        val navMenuView = binding.navView


        val headerView = navMenuView.getHeaderView(0)
        headerView.findViewById<AppCompatTextView>(R.id.userName).text = user?.name
        headerView.findViewById<AppCompatTextView>(R.id.userEmail).text = user?.email
        headerView.findViewById<AppCompatTextView>(R.id.userPhone).text =
            user?.phone
        Glide.with(this).load(user?.profileImage).error(R.drawable.user_image).fallback(R.drawable.user_image)
            .into(headerView.findViewById(R.id.userImage))


        val logoutItem = navMenuView.findViewById<AppCompatTextView>(R.id.nav_logout)

        logoutItem.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            helper.setUserLogin(false)
            helper.saveCurrentUser(null)
            helper.clearPreferences()
            Toast.makeText(this, "Logout", Toast.LENGTH_SHORT).show()
            drawerLayout!!.closeDrawers()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }


    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    companion object {
        const val END_SCALE: Float = 0.9f
    }

}
