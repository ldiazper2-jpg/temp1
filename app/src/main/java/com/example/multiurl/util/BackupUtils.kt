package com.example.multiurl.util

import android.content.Context
import android.net.Uri
import com.example.multiurl.data.UrlEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object BackupUtils {
    suspend fun export(context: Context, list: List<UrlEntity>, target: Uri) = withContext(Dispatchers.IO) {
        val json = JSONArray().apply {
            list.forEach { put(JSONObject().apply {
                put("url", it.url)
                put("isDefault", it.isDefault)
            }) }
        }
        context.contentResolver.openOutputStream(target)?.use { os ->
            os.write(json.toString(2).toByteArray(StandardCharsets.UTF_8))
            os.flush()
        }
    }

    suspend fun import(context: Context, src: Uri): List<UrlEntity> = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(src)?.use { input ->
            val reader = BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8))
            val text = reader.readText()
            val arr = JSONArray(text)
            val out = mutableListOf<UrlEntity>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(UrlEntity(
                    url = o.getString("url"),
                    isDefault = o.optBoolean("isDefault", false)
                ))
            }
            out
        } ?: emptyList()
    }
}
