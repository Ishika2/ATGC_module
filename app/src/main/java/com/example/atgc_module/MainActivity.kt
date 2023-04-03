package com.example.atgc_module

import android.R.attr.password
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.atgc_module.databinding.ActivityMainBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    var host: String? = null
    var username: String? = null
    var password: String? = null
    var port: Int? = null
    fun authenticate(view: View?) {
        // Create an intent for sshActivity
        //val intent = Intent(this, ::class.kt)
        intent.putExtra("host", host)
        intent.putExtra("port", port)
        intent.putExtra("username", username)
        intent.putExtra("password", password)
        startActivity(intent)
        finish()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button3.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(intent, FILE_PICK_REQUEST_CODE)
        }

        binding.button4.setOnClickListener{
            val url = "http://10.209.96.201/file.txt"
            val destinationPath = this.filesDir.path + "/file.txt"
            downloadFile(url, destinationPath)
        }
        ActivityCompat.requestPermissions(
            this, arrayOf<String>(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            PackageManager.PERMISSION_GRANTED
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun buttonCreateFile(view: View?) {
        val intent =
            Intent(Intent.ACTION_CREATE_DOCUMENT, MediaStore.Downloads.EXTERNAL_CONTENT_URI)
        //        intent.setType("application/pdf");
        intent.type = "*/*"
        this.startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun buttonOpenFile(view: View?) {
        val intent = Intent(Intent.ACTION_VIEW, MediaStore.Downloads.EXTERNAL_CONTENT_URI)
        //        intent.setType("application/pdf");
        intent.type = "*/*"
        this.startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICK_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            val filePath = uri?.let { uri ->
                when (uri.scheme) {
                    ContentResolver.SCHEME_FILE -> uri.path
                    ContentResolver.SCHEME_CONTENT -> {
                        val cursor = contentResolver.query(uri, null, null, null, null)
                        cursor?.use {
                            if (it.moveToFirst()) {
                                val pathIndex = it.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                                if (pathIndex != -1) {
                                    it.getString(pathIndex)
                                } else {
                                    null
                                }
                            } else {
                                null
                            }
                        }
                    }
                    else -> null
                }
            }
            if (filePath != null) {
                uploadFile(filePath)
            }
        }
    }



    fun uploadFile(filePath: String?) {
        if (filePath == null) {
            return
        }
        val file = File(filePath)
        val request = Request.Builder()
            .url("/home/sciverse/data/")
            .post(
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        file.name,
                        RequestBody.create("application/octet-stream".toMediaTypeOrNull(), file)
                    )
                    .build()
            )
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
            }

            override fun onResponse(call: Call, response: Response) {
                // Handle response
            }
        })
    }


    companion object {
        private const val FILE_PICK_REQUEST_CODE = 1
    }

    fun downloadFile(url: String, destinationPath: String) {
        val request = Request.Builder()
            .url(url)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val inputStream = response.body?.byteStream()
                    val outputStream = FileOutputStream(destinationPath)

                    inputStream?.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }

                    // Handle success
                } else {
                    // Handle non-successful response
                }
            }
        })
    }

}



























//Attempt 1
/*private fun filePicker() {
        //.Now Permission Working
        Toast.makeText(this@MainActivity, "File Picker Call", Toast.LENGTH_SHORT).show()
        //Let's Pick File
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = ""
        this.startActivity(intent)
    }
    fun UploadFile() {
        val uploadTask = UploadTask()
        uploadTask.execute(arrayOf<String>(file_path))
    }
    class UploadTask : AsyncTask<String?, String?, String>() {
        override fun onPostExecute(s: String) {
            super.onPostExecute(s)
            progressBar.setVisibility(View.GONE)
            if (s.equals("true", ignoreCase = true)) {
                Toast.makeText(this, "File uploaded", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed Upload", Toast.LENGTH_SHORT).show()
            }
        }

        override fun doInBackground(vararg p0: String?): String {
            return if (uploadFile(strings[0])) {
                "true"
            } else {
                "failed"
            }
        }

        override fun onPreExecute() {
            super.onPreExecute()
            progressBar.setVisibility(View.VISIBLE)
        }

        private fun uploadFile(path: String): Boolean {
            val file = File(path)
            return try {
                val requestBody: RequestBody = OkHttpClient.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "files",
                        file.getName(),
                        create(MediaType.parse("image/*"), file)
                    )
                    .addFormDataPart("some_key", "some_value")
                    .addFormDataPart("submit", "submit")
                    .build()
                val request: Request = OkHttpClient.Builder()
                    .url("http://192.168.0.2/project/upload.php")
                    .post(requestBody)
                    .build()
                val client = OkHttpClient()
                client.newCall(request).enqueue(object : Callback() {
                    fun onFailure(call: Call?, e: IOException) {
                        e.printStackTrace()
                    }

                    @Throws(IOException::class)
                    fun onResponse(call: Call?, response: Response?) {
                    }
                })
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }*/