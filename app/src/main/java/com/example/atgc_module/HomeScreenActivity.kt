package com.example.atgc_module

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.atgc_module.databinding.ActivityHomeScreenBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class HomeScreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeScreenBinding

    var host: String? =
        "111.91.225.19"            //out: 111.91.225.19 port: 22   #iit: 10.209.96.201
    var username: String? = "sciverse"
    var password: String? = "Access@App"
    var command: String? = "ls"
    var port: Int? = 22

    val sshTask2 = sshTask()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GlobalScope.launch {
            sshTask2.executeSSHCommand(host!!, username!!, password!!, command!!, port!!)
        }
        val toast =
            Toast.makeText(applicationContext, "Connected to the Server", Toast.LENGTH_SHORT)
        toast.show()

        binding = ActivityHomeScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.atgcButton.setOnClickListener {
            startActivity( Intent(this,ATGCActivity::class.java))
        }

        binding.NormalityButton.setOnClickListener {
            startActivity( Intent(this,NormalityActivity::class.java))
        }

        binding.MolarityButton.setOnClickListener {
            startActivity( Intent(this,MolarityActivity::class.java))
        }
    }
}