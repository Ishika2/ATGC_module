package com.example.atgc_module

import android.os.AsyncTask
import android.util.Log
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*

class sshTask  {
    suspend fun executeSSHCommand(host: String, user: String, password: String, command: String, port: Int): String {
        return withContext(Dispatchers.IO) {
            val jsch = JSch()
            val session: Session = jsch.getSession(user, host, port)
            session.setPassword(password)
            session.setConfig("StrictHostKeyChecking", "no")
            session.setConfig("PreferredAuthentications", "password")
//            java.util.Properties config = new java.util.Properties()
//            config.put

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
        filePath: File,
        remotePath: String,
        command: String
    ) {
        withContext(Dispatchers.IO) {
            val jsch = JSch()
            val session: Session = jsch.getSession(user, host, 22)
            session.setPassword(password)
            session.setConfig("StrictHostKeyChecking", "no")
            session.setConfig("PreferredAuthentications", "password")
            session.connect()

            val channel1 = session.openChannel("sftp") as ChannelSftp
            val channel2 = session.openChannel("exec") as ChannelExec

            channel1.connect()
            channel2.setCommand(command)

            val inputStream = channel2.inputStream
            val errorStream = channel2.errStream
            channel2.connect()

            val output = inputStream.bufferedReader().use { it.readText() }
            val error = errorStream.bufferedReader().use { it.readText() }

            Log.d("Output", output)



            if (output.isNotEmpty()) {
                output
            } else {
                error
            }

            val fis = filePath.inputStream()

            channel1.cd(remotePath)
            channel1.put(fis, filePath.name)

            channel1.disconnect()
            channel2.disconnect()
            session.disconnect()
        }
    }
}