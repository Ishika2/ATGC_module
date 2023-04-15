package com.example.atgc_module

import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.atgc_module.databinding.ActivityMainBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    val sshTask2 = sshTask()

    var host: String? = "111.91.225.19"            //out: 111.91.225.19 port: 44   #iit: 10.209.96.204
    var username: String? = "sciverse"
    var password: String? = "Access@App"
    var command: String? = "ls"
    var port: Int? = 22
    var remotePath: String? = "sciverse@10.209.96.201:~/sciverse/data"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button3.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "text/plain"
            startActivityForResult(intent, FILE_PICK_REQUEST_CODE)
        }

        binding.login.setOnClickListener{
            GlobalScope.launch {
                sshTask2.executeSSHCommand(host!!, username!!, password!!, command!!, port!!)
                // do something with the result
            }

        }
        ActivityCompat.requestPermissions(
            this, arrayOf<String>(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            PackageManager.PERMISSION_GRANTED
        )
    }

    // Get the selected file's URI in onActivityResult method
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_PICK_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val fileUri = data.data // Get the URI of the selected file
            if (fileUri != null) { // Add null check here
                val filePath = getFilePathFromUri(this, fileUri) // Get the file path from URI
                if (filePath != null) { // Add null check for file path
                    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        MimeTypeMap.getFileExtensionFromUrl(filePath)
                    )
                    if (mimeType?.startsWith("text/") == true) { // Check if MIME type is a text file
                        GlobalScope.launch {
                            sshTask2.uploadFileViaSSH(
                                host!!,
                                username!!,
                                password!!,
                                filePath,
                                remotePath!!,
                                command!!
                            )
                        } // Call uploadFileViaSSH method with file path
                    } else {
                        // Handle non-text file types here
                        Log.e(TAG, "Selected file is not a text file")
                    }
                } else {
                    // Handle null file path case
                    Log.e(TAG, "Failed to get file path from URI: $fileUri")
                }
            }
        }
    }

    fun getFilePathFromUri(context: Context, uri: Uri?): String? {
        if (uri == null) {
            return null
        }
        var filePath: String? = null

        // For Android API level 19 and above, use the getContentResolver method to get the file path from the URI
        if (Build.VERSION.SDK_INT >= 19 && DocumentsContract.isDocumentUri(context, uri)) {
            // Handle ExternalStorageProvider
            if ("com.android.externalstorage.documents" == uri.authority) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    filePath = Environment.getExternalStorageDirectory() + "/" + split[1]
                }
            } else if ("com.android.providers.downloads.documents" == uri.authority) {
                val id = DocumentsContract.getDocumentId(uri).toLong()
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    id
                )
                filePath = getDataColumn(context, contentUri, null, null)
            } else if ("com.android.providers.media.documents" == uri.authority) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])
                filePath = getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor: Cursor? =
                context.getContentResolver().query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                filePath = cursor.getString(column_index)
                cursor.close()
            }
        }
        return filePath
    }

    private fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        var cursor: Cursor? = null
        val column = "_data"
        val projectionForColumn = arrayOf(column)
        try {
            cursor = uri?.let {
                context.getContentResolver()
                    .query(it, projectionForColumn, selection, selectionArgs, null)
            }
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return null
    }


//third attempt filepath
//    fun getFilePathFromUri(uri: Uri): String? {
//        var filePath: String? = null
//        if (DocumentsContract.isDocumentUri(applicationContext, uri)) {
//            val documentId = DocumentsContract.getDocumentId(uri)
//            val split = documentId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
//            val type = split[0]
//            var contentUri: Uri? = null
//            contentUri = if ("image" == type) {
//                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//            } else if ("video" == type) {
//                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
//            } else if ("audio" == type) {
//                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
//            } else {
//                MediaStore.Files.getContentUri("external")
//            }
//            val selection = "_id=?"
//            val selectionArgs = arrayOf(split[1])
//            val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
//            var cursor: Cursor? = null
//            try {
//                cursor = contentResolver.query(contentUri, projection, selection, selectionArgs, null)
//                if (cursor != null && cursor.moveToFirst()) {
//                    val column_index: Int = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
//                    filePath = cursor.getString(column_index)
//                }
//            } catch (e: Exception) {
//                Log.e("TAG", "Failed to get file path from uri", e)
//            } finally {
//                if (cursor != null) cursor.close()
//            }
//        }
//        return filePath
//    }


//second attempt
//    fun getFilePathFromUri(uri: Uri): String? {
//        var filePath: String? = null
//        if (Build.VERSION.SDK_INT >= 19 && DocumentsContract.isDocumentUri(applicationContext, uri)) {
//            val documentId = DocumentsContract.getDocumentId(uri)
//            val split = documentId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
//            val type = split[0]
//            var contentUri: Uri? = null
//            contentUri = if ("image" == type) {
//                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//            } else if ("video" == type) {
//                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
//            } else if ("audio" == type) {
//                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
//            } else {
//                MediaStore.Files.getContentUri("external")
//            }
//            val selection = "_id=?"
//            val selectionArgs = arrayOf(split[1])
//            val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
//            var cursor: Cursor? = null
//            try {
//                cursor = contentResolver.query(contentUri, projection, selection, selectionArgs, null)
//                if (cursor != null && cursor.moveToFirst()) {
//                    val column_index: Int = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
//                    filePath = cursor.getString(column_index)
//                }
//            } catch (e: Exception) {
//                Log.e("TAG", "Failed to get file path from uri", e)
//            } finally {
//                if (cursor != null) cursor.close()
//            }
//        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
//            val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
//            var cursor: Cursor? = null
//            try {
//                cursor = contentResolver.query(uri, projection, null, null, null)
//                if (cursor != null && cursor.moveToFirst()) {
//                    val column_index: Int = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
//                    filePath = cursor.getString(column_index)
//                }
//            } catch (e: Exception) {
//                Log.e("TAG", "Failed to get file path from uri", e)
//            } finally {
//                if (cursor != null) cursor.close()
//            }
//        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
//            filePath = uri.path
//        }
//        return filePath
//    }

//first attempt for filepath
//    private fun getFilePathFromUri(context: Context, uri: Uri): String? {
//        var filePath: String? = null
//        try {
//            if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
//                val cursor = context.contentResolver.query(uri, null, null, null, null)
//                cursor?.let {
//                    it.moveToFirst()
//                    val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
//                    filePath = it.getString(columnIndex)
//                    it.close()
//                }
//            } else if (uri.scheme == ContentResolver.SCHEME_FILE) {
//                filePath = uri.path
//            }
//        } catch (e: Exception) {
//            Log.e("Error", "Exception while getting file path from uri: ${e.message}")
//        }
//        return filePath
//    }

    companion object {
        private const val FILE_PICK_REQUEST_CODE = 1
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


    /*fun authenticate(view: View?) {
        // Create an intent for sshActivity
        //val intent = Intent(this, ::class.kt)
        intent.putExtra("host", host)
        intent.putExtra("port", port)
        intent.putExtra("username", username)
        intent.putExtra("password", password)
        startActivity(intent)
        finish()
    }*/
 */


//second attempt
/*
    @RequiresApi(Build.VERSION_CODES.Q)
    fun buttonCreateFile(view: View?) {
        val intent =
            Intent(Intent.ACTION_CREATE_DOCUMENT, MediaStore.Downloads.EXTERNAL_CONTENT_URI)
        //        intent.setType("application/pdf");
        intent.type = ""
        this.startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun buttonOpenFile(view: View?) {
        val intent = Intent(Intent.ACTION_VIEW, MediaStore.Downloads.EXTERNAL_CONTENT_URI)
        //        intent.setType("application/pdf");
        intent.type = ""
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
    }*/

/*
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
    }*/