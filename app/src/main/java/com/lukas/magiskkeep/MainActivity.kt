package com.lukas.magiskkeep

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        // copy assets/scripts folder to apps directory
        Utils.copyAssetsToPrivateStorage(this)

        val sharedPref = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val editor = sharedPref.edit()

        setSupportActionBar(findViewById(R.id.topAppBar))

        if(!sharedPref.getBoolean("setupDone", false)) {
            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, AppSetup())
                    .commit()
            }
        }

        supportFragmentManager.setFragmentResultListener("setup_complete", this) { _, bundle ->
            if (bundle.getBoolean("success")) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, AppMain())
                    .commit()
            }
            Log.d("Received!!", "Received!!!!!")
        }





    }

    private fun showLanguageDialog() {
        val languages = arrayOf("English", "Español", "Deutsch", "Français", "Русский", "العربية")
        val codes = arrayOf("en", "es", "de", "fr", "ru", "ar")

        AlertDialog.Builder(this)
            .setTitle("Choose Language")
            .setItems(languages) { _, which ->
                val locale = Locale(codes[which])
                Locale.setDefault(locale)

                val config = resources.configuration
                config.setLocale(locale)

                resources.updateConfiguration(config, resources.displayMetrics)
                recreate() // Restart activity to apply changes
            }
            .show()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_language -> {
                // Handle language selection here
                showLanguageDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }




}