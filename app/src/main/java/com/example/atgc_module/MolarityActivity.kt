package com.example.atgc_module

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.atgc_module.databinding.ActivityMolarityBinding
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class MolarityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMolarityBinding

    var JobId: String = System.currentTimeMillis().toString()
    val JobName: String = "Molarity"
    val sshTask2 = sshTask()

    var host: String? = "111.91.225.19"

    var username: String? = "sciverse"
    var password: String? = "Access@App"
    var filename: String? = JobId
    var command: String? = "ls"
    var command1: String? = "sh /home/sciverse/Main.sh $filename $JobName"
    var port: Int? = 22

    val Vol: EditText by lazy { findViewById(R.id.editVolume) }
    val moles: EditText by lazy { findViewById(R.id.editMoles) }
    val MolWt: EditText by lazy { findViewById(R.id.editMolecularWt) }

    val result: TextView by lazy { findViewById(R.id.textResult) }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMolarityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.SubmitButton.setOnClickListener {
            GlobalScope.launch {
                sshTask2.executeSSHCommand(host!!, username!!, password!!, command!!, port!!)
            }

            val toast =
                Toast.makeText(applicationContext, "Connected to the Server", Toast.LENGTH_SHORT)
            toast.show()

            GlobalScope.launch {
                TextToFile(
                    Vol.text.toString(),
                    moles.text.toString(),
                    MolWt.text.toString()
                )
            }

            val toast1 = Toast.makeText(
                applicationContext,
                "Response Submitted Successfully",
                Toast.LENGTH_SHORT
            )
            toast1.show()
            val toast2 =
                Toast.makeText(applicationContext, "Your Job ID is $JobId", Toast.LENGTH_LONG)
            toast2.show()

        }

    }

    @OptIn(DelicateCoroutinesApi::class)
    fun TextToFile(Vol: String, moles: String, MolWt: String) {
        val file = File(externalCacheDir, JobId)
        file.writeText(Vol)
        file.appendText("\n")
        file.appendText(moles)
        file.appendText("\n")
        file.appendText(MolWt)

        GlobalScope.launch {
            sshTask2.uploadFileViaSSH(
                host!!,
                username!!,
                password!!,
                file
            )
            ResultViaSSH(host!!, username!!, password!!, command1!!, result)
        }
    }

    suspend fun ResultViaSSH(
        host: String,
        user: String,
        password: String,
        command1: String,
        result: TextView
    ) {
        withContext(Dispatchers.IO) {
            val jsch = JSch()
            val session: Session = jsch.getSession(user, host, 22)
            session.setPassword(password)
            session.setConfig("StrictHostKeyChecking", "no")
            session.setConfig("PreferredAuthentications", "password")
            session.connect()

            val channel2 = session.openChannel("exec") as ChannelExec

            channel2.setCommand(command1)

            val inputStream2 = channel2.inputStream
            val errorStream2 = channel2.errStream
            channel2.connect()

            val output2 = inputStream2.bufferedReader().use { it.readText() }
            //val error2 = errorStream2.bufferedReader().use { it.readText() }

            Log.d("Output 2 ", output2)

            val jsonOutput = JSONObject(output2)

            val res = jsonOutput.getJSONObject("0")

            withContext(Dispatchers.Main)
            {
                result.text = res.getDouble("Molarity").toString()
            }

            channel2.disconnect()
            session.disconnect()
        }
    }
}