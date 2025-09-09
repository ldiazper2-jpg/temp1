package com.example.multiurl.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

object InstallUtils {
    fun requestInstallPermissionIfNeeded(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canInstall = activity.packageManager.canRequestPackageInstalls()
            if (!canInstall) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
                return false
            }
        }
        return true
    }

    fun installApk(context: Context, uri: Uri) {
        try {
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(uri, "application/vnd.android.package-archive")
            }
            context.startActivity(installIntent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    fun fileToContentUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    }
}
