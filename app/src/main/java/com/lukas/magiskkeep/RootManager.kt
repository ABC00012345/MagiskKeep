package com.lukas.magiskkeep

import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object RootManager {
    // Method to execute a command and return the output
    fun execCmd(cmd: String): String {
        val output = StringBuilder()

        try {
            // Run the command as root
            val process = Runtime.getRuntime().exec(arrayOf("su","-mm","-c",cmd))

            // Read the output of the command
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String?
            while ((reader.readLine().also { line = it }) != null) {
                output.append(line).append("\n") // Append the output to StringBuilder
                Log.d("CMD", line!!) // Log the output
            }

            reader.close()
            process.waitFor()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        Log.d("test",output.toString())

        return output.toString() // Return the command output
    }
    fun execCmdSilent(cmd: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-mm", "-c", cmd))
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: IOException) {
            false
        } catch (e: InterruptedException) {
            false
        }
    }
}
