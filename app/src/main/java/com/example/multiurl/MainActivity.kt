package com.example.multiurl

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.multiurl.data.AppDatabase
import com.example.multiurl.data.UrlEntity
import com.example.multiurl.databinding.ActivityMainBinding
import com.example.multiurl.ui.UrlAdapter
import com.example.multiurl.util.BackupUtils
import com.example.multiurl.util.InstallUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private fun openSelectedInBrowser() {
    val sel = adapter.getSelectedOrDefault()
    if (sel == null) {
        Toast.makeText(this, "Agrega una URL primero", Toast.LENGTH_SHORT).show()
        return
    }
    startActivity(Intent(this, BrowserActivity::class.java).putExtra(BrowserActivity.EXTRA_URL, sel.url))
}

    private lateinit var binding: ActivityMainBinding
    private val db by lazy { AppDatabase.get(this) }
    private val adapter by lazy { UrlAdapter(::editUrl, ::deleteUrl) { setDefault(it) } }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* no-op */ }

    private val createBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) lifecycleScope.launch {
            val list = db.urlDao().getAll()
            BackupUtils.export(this@MainActivity, list, uri)
            Toast.makeText(this@MainActivity, "Backup exportado", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) lifecycleScope.launch {
            val list = BackupUtils.import(this@MainActivity, uri)
            val dao = db.urlDao()
            val existing = dao.getAll()
            existing.forEach { dao.delete(it) }
            var defaultSet = false
            list.forEach { item ->
                val id = dao.insert(item.copy(id = 0))
                if (item.isDefault && !defaultSet) {
                    dao.setDefault(id)
                    defaultSet = true
                }
            }
            Toast.makeText(this@MainActivity, "Backup restaurado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestLegacyStorageIfNeeded()

        binding.rvUrls.layoutManager = LinearLayoutManager(this)
        binding.rvUrls.adapter = adapter

        lifecycleScope.launch {
            db.urlDao().observeAll().collectLatest { list ->
                adapter.submit(list)
            }
        }

        binding.btnAddUrl.setOnClickListener {
            val text = binding.inputUrl.text?.toString()?.trim().orEmpty()
            if (text.isNotEmpty()) addUrl(text) else promptAddUrl()
        }
        binding.btnDownload.setOnClickListener { startDownloadFromSelected() }
        binding.btnBackup.setOnClickListener { createBackupLauncher.launch("multiurl-backup.json") }
        binding.btnRestore.setOnClickListener { pickBackupLauncher.launch(arrayOf("application/json")) }

        lifecycleScope.launch {
            val dao = db.urlDao()
            if (dao.getAll().isEmpty()) {
                val u1 = dao.insert(UrlEntity(url = "https://tvbox.mitienda.site/", isDefault = true))
                dao.setDefault(u1)
                dao.insert(UrlEntity(url = "https://tvbox.recuperat.mx/"))
            }
        }
    }

    private fun requestLegacyStorageIfNeeded() {
        val toAsk = mutableListOf<String>()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                toAsk += Manifest.permission.WRITE_EXTERNAL_STORAGE
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                toAsk += Manifest.permission.READ_EXTERNAL_STORAGE
            }
        } else if (Build.VERSION.SDK_INT in 30..32) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                toAsk += Manifest.permission.READ_EXTERNAL_STORAGE
            }
        }
        if (toAsk.isNotEmpty()) requestPermissionLauncher.launch(toAsk.toTypedArray())
    }

    private fun promptAddUrl() {
        val edit = EditText(this).apply { hint = "https://..." }
        AlertDialog.Builder(this)
            .setTitle("Agregar URL")
            .setView(edit)
            .setPositiveButton("Guardar") { d, _ ->
                val u = edit.text.toString().trim()
                if (URLUtil.isValidUrl(u)) lifecycleScope.launch { db.urlDao().insert(UrlEntity(url = u)) }
                else Toast.makeText(this, "URL no válida", Toast.LENGTH_SHORT).show()
                d.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addUrl(url: String) {
        if (!URLUtil.isValidUrl(url)) {
            Toast.makeText(this, "URL no válida", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            db.urlDao().insert(UrlEntity(url = url))
            binding.inputUrl.setText("")
        }
    }

    private fun editUrl(item: UrlEntity) {
        val edit = EditText(this).apply { setText(item.url) }
        AlertDialog.Builder(this)
            .setTitle("Editar URL")
            .setView(edit)
            .setPositiveButton("Guardar") { d, _ ->
                val u = edit.text.toString().trim()
                if (URLUtil.isValidUrl(u)) lifecycleScope.launch { db.urlDao().update(item.copy(url = u)) }
                else Toast.makeText(this, "URL no válida", Toast.LENGTH_SHORT).show()
                d.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteUrl(item: UrlEntity) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar")
            .setMessage("¿Eliminar esta URL?")
            .setPositiveButton("Sí") { _, _ -> lifecycleScope.launch { db.urlDao().delete(item) } }
            .setNegativeButton("No", null)
            .show()
    }

    private fun setDefault(item: UrlEntity) {
        lifecycleScope.launch { db.urlDao().setDefault(item.id) }
    }

    private fun startDownloadFromSelected() {
        val sel = adapter.getSelectedOrDefault()
        if (sel == null) {
            Toast.makeText(this, "Agrega una URL primero", Toast.LENGTH_SHORT).show()
            return
        }
        if (!InstallUtils.requestInstallPermissionIfNeeded(this)) {
            Toast.makeText(this, "Habilita la instalación desde orígenes desconocidos", Toast.LENGTH_LONG).show()
            return
        }
        enqueueDownload(sel.url)
    }

    private fun enqueueDownload(url: String) {
        if (!URLUtil.isValidUrl(url)) {
            Toast.makeText(this, "URL inválida", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = Uri.parse(url)
        val fileName = URLUtil.guessFileName(url, null, null)
        val request = DownloadManager.Request(uri).apply {
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setTitle(fileName)
            setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
            val ext = fileName.substringAfterLast('.', "").lowercase()
            if (ext == "apk") setMimeType("application/vnd.android.package-archive")
        }
        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = dm.enqueue(request)
        Toast.makeText(this, "Descarga iniciada (#$id)", Toast.LENGTH_SHORT).show()
    }
}
