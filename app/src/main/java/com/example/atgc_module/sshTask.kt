package com.example.atgc_module

import android.content.ContentResolver
import android.os.AsyncTask
import android.os.Environment
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.internal.ContextUtils.getActivity
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.*

open class sshTask {
    suspend fun executeSSHCommand(
        host: String,
        user: String,
        password: String,
        command: String,
        port: Int
    ): String {
        return withContext(Dispatchers.IO) {
            val jsch = JSch()
            val session: Session = jsch.getSession(user, host, port)
            session.setPassword(password)
            session.setConfig("StrictHostKeyChecking", "no")
            session.setConfig("PreferredAuthentications", "password")

            session.connect()

            val channel = session.openChannel("exec") as ChannelExec
            channel.setCommand(command)

            val inputStream = channel.inputStream
            val errorStream = channel.errStream
            channel.connect()

            val output = inputStream.bufferedReader().use { it.readText() }
            val error = errorStream.bufferedReader().use { it.readText() }

            Log.d("Output", output)

            channel.disconnect()
            session.disconnect()

            if (output.isNotEmpty()) {
                output
            } else {
                error
            }
        }
    }

    suspend fun uploadFileViaSSH(
        host: String,
        user: String,
        password: String,
        filePath: File
    ) {
        withContext(Dispatchers.IO) {
            val jsch = JSch()
            val session: Session = jsch.getSession(user, host, 22)
            session.setPassword(password)
            session.setConfig("StrictHostKeyChecking", "no")
            session.setConfig("PreferredAuthentications", "password")
            session.connect()

            val channel1 = session.openChannel("sftp") as ChannelSftp

            val fis = filePath.inputStream()

            channel1.connect()
            channel1.put(fis, filePath.name)
            channel1.disconnect()
            session.disconnect()
        }
    }

    suspend fun DownloadFileViaSSH(
        host: String,
        user: String,
        password: String,
        fileName: String
//        filePath: File
    ) {
        withContext(Dispatchers.IO) {
            val jsch = JSch()
            val session: Session = jsch.getSession(user, host, 22)
            session.setPassword(password)
            session.setConfig("StrictHostKeyChecking", "no")
            session.setConfig("PreferredAuthentications", "password")
            session.connect()

//            val channel1 = session.openChannel("exec") as ChannelExec
//            channel1.setCommand(command)

//            val inputStream2 = channel1.inputStream
//            val outputStream2 = channel1.outputStream
//            val errorStream2 = channel1.errStream
//            channel1.connect()

            val channel2 = session.openChannel("sftp") as ChannelSftp
            channel2.connect()

            val inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null

            try {

                val outputFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),fileName)
                outputStream = FileOutputStream(outputFile)

                val inputFile = channel2.get("/home/sciverse/$fileName", outputStream)

            }
            catch (e: Exception) {
                Log.d("DownloadFile","Error downloading file: $e")
            }
            finally {
                inputStream?.close()
                outputStream?.close()
            }

//            channel1.disconnect()
            channel2.disconnect()
            session.disconnect()


        }
    }

    suspend fun ExecuteCommand(
        host: String,
        user: String,
        password: String,
        command: String
    ) {
        withContext(Dispatchers.IO) {
            val jsch = JSch()
            val session: Session = jsch.getSession(user, host, 22)
            session.setPassword(password)
            session.setConfig("StrictHostKeyChecking", "no")
            session.setConfig("PreferredAuthentications", "password")
            session.connect()


            val channel1 = session.openChannel("exec") as ChannelExec
            channel1.setCommand(command)

            channel1.connect()

//            val output = channel1.inputStream
//
//            Log.d("Output ls", output.bufferedReader().use { it.readText() })

            channel1.disconnect()
            session.disconnect()
        }
    }

}



//            var output: OutputStream? = null
//            var input: FileInputStream? = null
//            var finalFile: File? = null
//
//            finalFile = File(Environment.getExternalStorageDirectory().path)
//
//            input = FileInputStream(finalFile.path)
//            input.write(fileContents)

//            val file: String = fileContents

//            val file = File( Environment.getExternalStorageDirectory().path, "/home/sciverse/Output_Seq.txt".substringAfterLast("/"))

//            val outputStream = channel1.inputStream

//            val fos = file.path


//            channel1.get("/home/sciverse/JOB/$jobid/Output_Seq.txt",fos.toString())

//downloadFileFromServer("/home/sciverse/JOB/$jobid/Output_Seq.txt",Environment.getExternalStorageDirectory().path)

//channel1.put(fis, filePath.name)



//                val fileContents = inputFile.bufferedReader().use { it.readText() }

//                inputStream = ByteArrayInputStream(inputFile.readBytes())
//                val bufferSize = 1024
//                val buffer = ByteArray(bufferSize)
//                val inputBytes = inputStream.readBytes()

//                val outputFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"Output_Seq.txt")

//                outputStream = FileOutputStream(outputFile)
//                var bytesRead = inputStream.read(buffer)
//
//                while(bytesRead != -1)
//                {
//                    outputStream.write(buffer,0,bytesRead)
//                    bytesRead = inputStream.read(buffer)
//                }

//                outputStream.write(inputBytes)
