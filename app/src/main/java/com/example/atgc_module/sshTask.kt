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
//        remotePath: String,
        command: String,
        command2: String,
        command3: String,
        command4: String,
        command5: String

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
            val channel3 = session.openChannel("exec") as ChannelExec
            val channel4 = session.openChannel("exec") as ChannelExec
            val channel5 = session.openChannel("exec") as ChannelExec
            val channel6 = session.openChannel("exec") as ChannelExec

            val fis = filePath.inputStream()

            channel1.connect()
            channel1.put(fis, filePath.name)
            channel2.setCommand(command)
            channel3.setCommand(command2)
            channel4.setCommand(command3)
            channel5.setCommand(command4)
            channel6.setCommand(command5)

            val inputStream = channel2.inputStream
            val errorStream = channel2.errStream
            channel2.connect()

//            val inputStream2 = channel3.inputStream
            val errorStream2 = channel3.errStream
            channel3.connect()

            val errorStream3 = channel4.errStream
            channel4.connect()

            val errorStream4 = channel5.errStream
            channel5.connect()

            val inputStream5 = channel6.inputStream
            val errorStream5 = channel6.errStream
            channel6.connect()

            val output = inputStream.bufferedReader().use { it.readText() }
            val error = errorStream.bufferedReader().use { it.readText() }

//            val output2 = inputStream2.bufferedReader().use { it.readText() }
            val error2 = errorStream2.bufferedReader().use { it.readText() }

            val output5 = inputStream5.bufferedReader().use { it.readText() }

            Log.d("Output", output)

            Log.d("Output 5", output5)

//            Log.d("Output", output2)



            if (output.isNotEmpty()) {
                output
            } else {
                error
            }

//            if (output2.isNotEmpty()) {
//                output2
//            } else {
//                error2
//            }

//            val fis = filePath.inputStream()

//            channel1.cd(remotePath)
//            channel1.put(fis, filePath.name)

            channel1.disconnect()
            channel2.disconnect()
            channel3.disconnect()
            channel4.disconnect()
            channel5.disconnect()
            channel6.disconnect()

            session.disconnect()
        }
    }
}