package com.example.atgc_module

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.atgc_module.databinding.ActivityGcPercentBinding
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
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.Exception

class GC_percentActivity : AppCompatActivity() {
    lateinit var binding: ActivityGcPercentBinding
    var JobId: String = System.currentTimeMillis().toString() //"1234"
    val JobName: String = "GC_percent"
    val sshTask2 = sshTask()

    var host: String? =
        "111.91.225.19"            //out: 111.91.225.19 port: 22   #iit: 10.209.96.201
    var username: String? = "sciverse"
    var password: String? = "Access@App"
    var filename: String? = JobId
    var command: String? = "ls"
    var command1: String? = "sh /home/sciverse/Main.sh $filename $JobName"
    var port: Int? = 22

    val Result: TextView by lazy { findViewById(R.id.result) }

    private val textATGC: EditText by lazy { findViewById(R.id.editATGC) }

    val error: TextView by lazy { findViewById(R.id.errorView) }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGcPercentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.uploadFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "text/plain"
            startActivityForResult(intent, FILE_PICK_REQUEST_CODE)
        }

        binding.SubmitButtonFile.setOnClickListener {
            GlobalScope.launch {
                ResultViaSSH(
                    host!!, username!!, password!!, command1!!,
                    Result,
                    error
                )
                // do something with the result
            }
        }

        binding.SubmitButton.setOnClickListener {
            TextToFile(textATGC.text.toString())
            val toast = Toast.makeText(
                applicationContext,
                "Response Submitted Successfully",
                Toast.LENGTH_SHORT
            )
            toast.show()
            val toast2 =
                Toast.makeText(applicationContext, "Your Job ID is $JobId", Toast.LENGTH_LONG)
            toast2.show()
        }

        ActivityCompat.requestPermissions(
            this, arrayOf(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            PackageManager.PERMISSION_GRANTED
        )
    }


    //Get the selected file's URI in onActivityResult method
    @OptIn(DelicateCoroutinesApi::class)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_PICK_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val fileUri = data.data // Get the URI of the selected file
            if (fileUri != null) { // Add null check here

                val filePath = getFileFromContentUri(fileUri)
                Log.d("filepath", filePath?.path.toString())

                // Get the file path from URI
                if (filePath != null) { // Add null check for file path
                    GlobalScope.launch {
                        sshTask2.uploadFileViaSSH(
                            host!!,
                            username!!,
                            password!!,
                            filePath
                        )
                    }
                    val toast = Toast.makeText(
                        applicationContext,
                        "File Uploaded Successfully",
                        Toast.LENGTH_SHORT
                    )
                    toast.show()
                    val toast2 = Toast.makeText(
                        applicationContext,
                        "Your Job ID is $JobId",
                        Toast.LENGTH_SHORT
                    )
                    toast2.show()
                } else {
                    // Handle null file path case
                    Log.e(ContentValues.TAG, "Failed to get file path from URI: $fileUri")
                }
            }

        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun TextToFile(InputText: String) {
        val file = File(externalCacheDir, JobId)
        file.writeText(InputText)

        GlobalScope.launch {
            sshTask2.uploadFileViaSSH(
                host!!,
                username!!,
                password!!,
                file
            )

            ResultViaSSH(
                host!!, username!!, password!!, command1!!,
                Result,
                error
            )
        }
    }

    suspend fun ResultViaSSH(
        host: String,
        user: String,
        password: String,
        command1: String,
        Result: TextView,
        error: TextView
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

            val ResJSON = jsonOutput.getJSONObject("0")

            //val FreqJSON = jsonOutput.getJSONObject("Frequency")

            withContext(Dispatchers.Main)
            {

                val others = ResJSON.getInt("others")

                if (others > 0) {
                    error.text = "Invalid DNA Sequence"
                    Result.text = "0"
                } else {
                    error.text = ""
                    Result.text = ResJSON.getDouble("GC_percent").toString()
                }
            }
            channel2.disconnect()
            session.disconnect()
        }
    }

    private fun getFileFromContentUri(contentUri: Uri): File? {
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        var finalFile: File? = null

        try {
            inputStream = contentResolver?.openInputStream(contentUri)
            val inputBytes = inputStream?.readBytes() ?: byteArrayOf()
            //JobId = "12345"
            finalFile = File(externalCacheDir, JobId)  //job sequencing

            if (finalFile.exists()) {
                finalFile.delete()
            }

            outputStream = FileOutputStream(finalFile.path)
            outputStream.write(inputBytes)

        } catch (e: Exception) {
            Log.d("UploadError", "on creating file: $e")
        } finally {
            inputStream?.close()
            outputStream?.close()
        }

        return finalFile

    }

    companion object {
        private const val FILE_PICK_REQUEST_CODE = 1
    }

}