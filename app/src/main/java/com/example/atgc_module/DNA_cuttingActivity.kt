package com.example.atgc_module

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.atgc_module.databinding.ActivityDnaCuttingBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.Exception

class DNA_cuttingActivity : AppCompatActivity() {

    lateinit var binding: ActivityDnaCuttingBinding
    var JobId: String = System.currentTimeMillis().toString() //"1234"
    val JobName: String = "DNA_cutting"
    val sshTask2 = sshTask()

    var host: String? =
        "111.91.225.19"            //out: 111.91.225.19 port: 22   #iit: 10.209.96.201
    var username: String? = "sciverse"
    var password: String? = "Access@App"
    var port: Int? = 22
    var filename: String? = JobId


    private val textATGC: EditText by lazy { findViewById(R.id.editATGC) }
    private val textStart: EditText by lazy { findViewById(R.id.editStart) }
    private val textORF: EditText by lazy { findViewById(R.id.editORF) }
    private val textEnd: EditText by lazy { findViewById(R.id.editEnd) }

    @OptIn(DelicateCoroutinesApi::class)
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDnaCuttingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.uploadFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "text/plain"
            startActivityForResult(intent, FILE_PICK_REQUEST_CODE)
        }

        binding.SubmitButton.setOnClickListener {

            val start: String = textStart.text.toString()
            val orf: String = textORF.text.toString()
            val end: String = textEnd.text.toString()

            val command1 = "sh /home/sciverse/Main.sh $filename $JobName $start $orf $end"

            if (textATGC.text.toString().isNotEmpty())
            {
                TextToFile(textATGC.text.toString())

                val toast = Toast.makeText(
                    applicationContext,
                    "Response Submitted Successfully",
                    Toast.LENGTH_SHORT
                )
                toast.show()
            }

            else
            {
                GlobalScope.launch {
                    sshTask2.ExecuteCommand(host!!,username!!,password!!,command1)
                }
                val toast2 =
                    Toast.makeText(applicationContext, "Your Job ID is $JobId", Toast.LENGTH_LONG)
                toast2.show()
            }
        }

        binding.buttonDownload.setOnClickListener {
            GlobalScope.launch {
                sshTask2.DownloadFileViaSSH(host!!,username!!,password!!,"$JobId.txt")
            }

            val command2 = "mv /home/sciverse/$JobId /home/sciverse/JOBS/$JobId"

            GlobalScope.launch {
                sshTask2.ExecuteCommand(host!!,username!!,password!!,command2)
            }

            val toast = Toast.makeText(
                applicationContext,
                "File Downloaded Successfully, check downloads folder!",
                Toast.LENGTH_SHORT
            )
            toast.show()
        }

        ActivityCompat.requestPermissions(
            this, arrayOf(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
            ),
            PackageManager.PERMISSION_GRANTED
        )

    }




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
                } else {
                    // Handle null file path case
                    Log.e(ContentValues.TAG, "Failed to get file path from URI: $fileUri")
                }
            }

        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun TextToFile(InputText: String) {
        val file = File(externalCacheDir, JobId)    //externalCacheDir
        file.writeText(InputText)

        val start: String = textStart.text.toString()
        val orf: String = textORF.text.toString()
        val end: String = textEnd.text.toString()
        val command1 = "sh /home/sciverse/Main.sh $filename $JobName $start $orf $end"

        GlobalScope.launch {
            sshTask2.uploadFileViaSSH(
                host!!,
                username!!,
                password!!,
                file
            )

            sshTask2.ExecuteCommand(host!!,username!!,password!!,command1)
        }
        val toast2 =
            Toast.makeText(applicationContext, "Your Job ID is $JobId", Toast.LENGTH_LONG)
        toast2.show()
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