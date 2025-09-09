package com.example.multiurl.receiver

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import com.example.multiurl.util.InstallUtils
import java.io.File

class DownloadCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == intent.action) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val c: Cursor = dm.query(DownloadManager.Query().setFilterById(id))
            if (c.moveToFirst()) {
                val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    val uriString = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                    val mime = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE))
                    val uri = uriString?.let { Uri.parse(it) }

                    val isApk = when {
                        mime == "application/vnd.android.package-archive" -> true
                        uri?.lastPathSegment?.endsWith(".apk", ignoreCase = true) == true -> true
                        else -> false
                    }

                    if (isApk && uri != null) {
                        val apkUri = if ("file" == uri.scheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            val file = File(uri.path!!)
                            InstallUtils.fileToContentUri(context, file)
                        } else uri
                        InstallUtils.installApk(context, apkUri)
                    }
                }
            }
            c.close()
        }
    }
}
