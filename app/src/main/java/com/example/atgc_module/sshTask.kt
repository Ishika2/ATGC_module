package com.example.atgc_module

import android.os.AsyncTask
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
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

class sshTask {
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


    suspend fun ResultViaSSH(
        host: String,
        user: String,
        password: String,
        command1: String,
        NumA: TextView,
        NumT: TextView,
        NumG: TextView,
        NumC: TextView,
        FreqA: TextView,
        FreqT: TextView,
        FreqG: TextView,
        FreqC: TextView,
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

            val NumJSON = jsonOutput.getJSONObject("Numbers")

            val FreqJSON = jsonOutput.getJSONObject("Frequency")

            withContext(Dispatchers.Main)
            {

                val others = NumJSON.getInt("others")

                if (others > 0) {
                    error.text = "Invalid DNA Sequence"
                    NumA.text = "A"
                    NumT.text = "T"
                    NumG.text = "G"
                    NumC.text = "C"

                    FreqA.text = "A"
                    FreqT.text = "T"
                    FreqG.text = "G"
                    FreqC.text = "C"
                } else {
                    error.text = ""
                    NumA.text = NumJSON.getInt("A").toString()
                    NumT.text = NumJSON.getInt("T").toString()
                    NumG.text = NumJSON.getInt("G").toString()
                    NumC.text = NumJSON.getInt("C").toString()

                    FreqA.text = FreqJSON.getDouble("A").toString()
                    FreqT.text = FreqJSON.getDouble("T").toString()
                    FreqG.text = FreqJSON.getDouble("G").toString()
                    FreqC.text = FreqJSON.getDouble("C").toString()

                }

            }
            channel2.disconnect()
            session.disconnect()
        }
    }
}