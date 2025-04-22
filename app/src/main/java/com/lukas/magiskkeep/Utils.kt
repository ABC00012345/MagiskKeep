package com.lukas.magiskkeep

import android.app.ProgressDialog
import android.content.Context
import android.util.Log
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object Utils {

    var logViewText = ""


    // asset copy related stuff *****
    fun copyAssetsToPrivateStorage(context: Context) {
        try {
            // Get the app's private files directory
            val privateDir = context.filesDir

            // List all files/directories in assets
            val assetList = context.assets.list("") ?: return

            assetList.forEach { assetName ->
                try {
                    if (isAssetDirectory(context, assetName)) {
                        // Handle directory
                        copyAssetDirectory(context, assetName, File(privateDir, assetName))
                    } else {
                        // Handle single file
                        context.assets.open(assetName).use { inputStream ->
                            File(privateDir, assetName).outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isAssetDirectory(context: Context, path: String): Boolean {
        return try {
            // Try to list contents - if it works, it's a directory
            context.assets.list(path)?.isNotEmpty() ?: false
        } catch (e: IOException) {
            false
        }
    }

    private fun copyAssetDirectory(context: Context, assetPath: String, targetDir: File) {
        try {
            targetDir.mkdirs()
            val files = context.assets.list(assetPath) ?: return

            files.forEach { fileName ->
                val fullAssetPath = if (assetPath.isEmpty()) fileName else "$assetPath/$fileName"

                if (isAssetDirectory(context, fullAssetPath)) {
                    // Recursively copy subdirectories
                    copyAssetDirectory(context, fullAssetPath, File(targetDir, fileName))
                } else {
                    // Copy file
                    context.assets.open(fullAssetPath).use { inputStream ->
                        File(targetDir, fileName).outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    // asset copy related stuff *****



    fun updateLogView(logView: TextView){
        logView.setText(logViewText)
    }

    fun startMagiskInstallProcess(
        logView: TextView,
        bootPartPath: String,
        context: Context,
        onResult: (Boolean) -> Unit
    ) {
        val dialog = ProgressDialog(context).apply {
            setMessage("Installing...")
            setCancelable(false)
            show()
        }

        CoroutineScope(Dispatchers.Main).launch {
            val result = withContext(Dispatchers.IO) {
                installMagisk(logView, bootPartPath, context)
            }
            dialog.dismiss()
            onResult(result)
        }
    }


    suspend fun installMagisk(logView: TextView, bootPartPath: String, context: Context?): Boolean {
        context ?: run {
            withContext(Dispatchers.Main) {
                logViewText += "Context is null\n"
                updateLogView(logView)
            }
            return false
        }

        // clear logs
        withContext(Dispatchers.Main) {
            logViewText = ""
            logView.text = ""
        }

        withContext(Dispatchers.Main) {
            logViewText += "Extracting Image from boot${
                bootPartPath.substring(
                    bootPartPath.length - 2,
                    bootPartPath.length
                )
            } partition ...\n"
            updateLogView(logView)
        }

        // extract boot.img
        if (!extractBootPart(context, bootPartPath)) {
            withContext(Dispatchers.Main) {
                logViewText += context.getString(R.string.failed_to_extract_boot_image)
                updateLogView(logView)
            }
            return false
        } else {
            withContext(Dispatchers.Main) {
                logViewText += context.getString(R.string.boot_image_extracted_successfully)
                updateLogView(logView)
            }
        }

        withContext(Dispatchers.Main) {
            logViewText += context.getString(R.string.patching_boot_img)
            updateLogView(logView)
        }

        // patch the boot.img
        if (!patchBootImg(context)) {
            withContext(Dispatchers.Main) {
                logViewText += context.getString(R.string.failed_to_patch_boot_image)
                updateLogView(logView)
            }
            return false
        } else {
            withContext(Dispatchers.Main) {
                logViewText += context.getString(R.string.boot_image_patched_successfully)
                updateLogView(logView)
            }
        }

        withContext(Dispatchers.Main) {
            logViewText += context.getString(R.string.flashing_new_boot_img)
            updateLogView(logView)
        }

        // patch the boot.img
        if (!flashBootPart(context, bootPartPath)) {
            withContext(Dispatchers.Main) {
                logViewText += context.getString(R.string.failed_to_flash_boot_image)
                updateLogView(logView)
            }
            return false
        } else {
            withContext(Dispatchers.Main) {
                logViewText += context.getString(R.string.boot_image_flashed_successfully)
                updateLogView(logView)
            }
        }

        withContext(Dispatchers.Main) {
            logViewText += context.getString(R.string.done)
            updateLogView(logView)
        }


        return true
    }

    private fun patchBootImg(context: Context): Boolean {
        makeAllFilesExecutable(context.filesDir)
        if(!executeBootPatch(context)) return false

        return true
    }

    fun executeBootPatch(context: Context): Boolean {
        val privateDir = context.filesDir
        val bootPatchScript = File(privateDir, "boot_patch.sh")
        val bootImage = File(privateDir, "boot.img")

        return try {
            // Make script executable (this should be done as root too!)
            RootManager.execCmd("chmod +x ${bootPatchScript.absolutePath}")

            // Build the command to run
            val cmd = "sh ${bootPatchScript.absolutePath} ${bootImage.absolutePath}"

            // Execute as root using your own root method
            val output = RootManager.execCmd(cmd)

            Log.d("BootPatch", "Output:\n$output")
            logViewText += "BootPatch Output:\n$output\n"

            // Optional: check if output contains success indicators
            !output.contains("error", ignoreCase = true)
        } catch (e: Exception) {
            Log.e("BootPatch", "Execution failed", e)
            logViewText += "Exception: ${e.message}\n"
            false
        }
    }


    fun makeAllFilesExecutable(dir: File) {
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    makeAllFilesExecutable(file) // recurse into subdir
                } else {
                    file.setExecutable(true, false)
                }
            }
        } else {
            dir.setExecutable(true, false)
        }
    }

    private fun extractBootPart(context: Context, bootPartPath: String): Boolean {
        return runCatching {
            // 1. Validate source
            /*if (!RootManager.execCmdSilent("[ -f $bootPartPath ]")) {
                throw IOException("Boot partition not found at $bootPartPath")
            }*/

            // 2. Prepare output
            val outputDir = context.filesDir.apply { mkdirs() }
            val outputFile = File(outputDir, "boot.img")

            // 3. Execute dd with error checking
            val cmd = "dd if=$bootPartPath of=${outputFile.absolutePath} bs=4096"
            /*if (!RootManager.execCmdSilent(cmd)) {
                throw IOException("dd command failed")
            }*/if (!RootManager.execCmdSilent(cmd)) {
                throw IOException("dd command failed")
            }

            // 4. Verify extraction
            /*if (!outputFile.exists() || outputFile.length() == 0L) {
                throw IOException("Extracted file is empty/missing")
            }*/

            Log.d("", "Boot image extracted to ${outputFile.absolutePath}")

            // set owner+group of the boot.img to the app:
            val uid = RootManager.execCmd("cat /data/system/packages.list | grep com.lukas.magiskkeep").trim().split(" ")[1] // output example: com.lukas.magiskkeep 10123 0 /data/user/0/com.lukas.magiskkeep ...
            RootManager.execCmd("chown $uid:$uid ${outputFile.absolutePath}")

            true
        }.getOrElse { e ->
            Log.e("", "Extraction failed", e)
            false
        }
    }

    private fun flashBootPart(context: Context, bootPartPath: String): Boolean {
        return runCatching {
            // 1. Validate source
            /*if (!RootManager.execCmdSilent("[ -f $bootPartPath ]")) {
                throw IOException("Boot partition not found at $bootPartPath")
            }*/

            // very important, fix the flash issue on most devices:
            if(!RootManager.execCmdSilent("blockdev --setrw $bootPartPath")){
                throw IOException("setrw on target partition failed!")
            }

            // 2. Prepare input
            val inputDir = context.filesDir.apply { mkdirs() }
            val inputFile = File(inputDir, "new-boot.img")
            
            // 3. Execute dd with error checking  // flash newly created boot.img with magisk
            val cmd = "dd if=${inputFile.absolutePath} of=$bootPartPath bs=4096"
            /*if (!RootManager.execCmdSilent(cmd)) {
                throw IOException("dd command failed")
            }*/if (!RootManager.execCmdSilent(cmd)) {
                throw IOException("dd command failed")
            }

            // 4. Verify extraction
            /*if (!outputFile.exists() || outputFile.length() == 0L) {
                throw IOException("Extracted file is empty/missing")
            }*/

            Log.d("", "Boot image flashed to $bootPartPath")


            true
        }.getOrElse { e ->
            Log.e("", "Extraction failed", e)
            false
        }
    }
}