package com.example.atgc_module

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.atgc_module.databinding.ActivityMainBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import java.io.*
import java.lang.Exception


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    var JobId: String = System.currentTimeMillis().toString() //"1234"
    val sshTask2 = sshTask()

    var host: String? =
        "111.91.225.19"            //out: 111.91.225.19 port: 22   #iit: 10.209.96.201
    var username: String? = "sciverse"
    var password: String? = "Access@App"
    var filename: String? = JobId
    var command: String? = "ls"
    var command1: String? = "sh /home/sciverse/Main.sh $filename"
    var port: Int? = 22

    val NumA: TextView by lazy { findViewById(R.id.NumA) }
    val NumT: TextView by lazy { findViewById(R.id.NumT) }
    val NumG: TextView by lazy { findViewById(R.id.NumG) }
    val NumC: TextView by lazy { findViewById(R.id.NumC) }

    val FreqA: TextView by lazy { findViewById(R.id.FreqA) }
    val FreqT: TextView by lazy { findViewById(R.id.FreqT) }
    val FreqG: TextView by lazy { findViewById(R.id.FreqG) }
    val FreqC: TextView by lazy { findViewById(R.id.FreqC) }

    val error: TextView by lazy { findViewById(R.id.errorView) }

    private val textATGC: EditText by lazy { findViewById(R.id.editATGC) }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.uploadFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "text/plain"
            startActivityForResult(intent, FILE_PICK_REQUEST_CODE)
        }

        binding.SubmitButtonFile.setOnClickListener {
            GlobalScope.launch {
                sshTask2.ResultViaSSH(
                    host!!, username!!, password!!, command1!!,
                    NumA,
                    NumT,
                    NumG,
                    NumC,
                    FreqA,
                    FreqT,
                    FreqG,
                    FreqC,
                    error
                )
                // do something with the result
            }
        }

        binding.connect.setOnClickListener {
            GlobalScope.launch {
                sshTask2.executeSSHCommand(host!!, username!!, password!!, command!!, port!!)
                // do something with the result
            }
            val toast =
                Toast.makeText(applicationContext, "Connected to the Server", Toast.LENGTH_SHORT)
            toast.show()
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
                    Log.e(TAG, "Failed to get file path from URI: $fileUri")
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

            sshTask2.ResultViaSSH(
                host!!, username!!, password!!, command1!!,
                NumA,
                NumT,
                NumG,
                NumC,
                FreqA,
                FreqT,
                FreqG,
                FreqC,
                error
            )
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