package com.example.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.example.AppContainer
import com.example.util.currentTimeMillis
import com.example.util.toKey
import com.example.util.todayDate
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Implemented by the Application so background receivers can reach the app's single container. */
interface AppContainerHolder {
    val container: AppContainer
}

/**
 * Daily backup into a folder the user picks once through the system folder picker (Google Drive, Downloads, ...).
 * The folder permission is kept across restarts, and an inexact daily alarm writes the file even when the app
 * is closed; opening the app also catches up when a day was missed.
 */
class AndroidAutoBackup(private val context: Context) : AutoBackup {
    /** Set by the Activity, which owns the folder-picker launcher. */
    var folderPicker: () -> Unit = {}

    private var exporter: (suspend () -> String)? = null
    private var enableAfterPick = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs get() = prefs(context)

    private val _state = MutableStateFlow(readState())
    override val state: StateFlow<AutoBackupState> = _state.asStateFlow()

    private fun readState(error: String? = null) = AutoBackupState(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        location = prefs.getString(KEY_FOLDER_NAME, null),
        lastBackupMillis = prefs.getLong(KEY_LAST, 0L).takeIf { it > 0 },
        canChangeFolder = true,
        error = error
    )

    override fun attach(exporter: suspend () -> String) {
        this.exporter = exporter
    }

    override fun setEnabled(enabled: Boolean) {
        if (enabled && prefs.getString(KEY_FOLDER, null) == null) {
            enableAfterPick = true
            folderPicker()
            return
        }
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        schedule(context)
        _state.value = readState()
        if (enabled) backupIfDue()
    }

    override fun changeFolder() = folderPicker()

    /** Result of the folder picker; null when the user backed out. */
    fun onFolderPicked(uri: Uri?) {
        if (uri == null) {
            enableAfterPick = false
            return
        }
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
        prefs.edit()
            .putString(KEY_FOLDER, uri.toString())
            .putString(KEY_FOLDER_NAME, folderName(uri))
            .apply {
                if (enableAfterPick) putBoolean(KEY_ENABLED, true)
            }
            .apply()
        enableAfterPick = false
        schedule(context)
        _state.value = readState()
        backupNow()
    }

    override fun backupNow() {
        scope.launch { runBackup() }
    }

    override fun backupIfDue() {
        val last = prefs.getLong(KEY_LAST, 0L)
        if (prefs.getBoolean(KEY_ENABLED, false) && currentTimeMillis() - last > DUE_AFTER_MILLIS) backupNow()
    }

    /** Writes today's file and trims old ones. Returns false (and shows why) when it could not. */
    suspend fun runBackup(): Boolean {
        val folder = prefs.getString(KEY_FOLDER, null)?.let(Uri::parse)
        val export = exporter
        if (folder == null || export == null) return false
        return try {
            val content = export()
            val resolver = context.contentResolver
            val treeId = DocumentsContract.getTreeDocumentId(folder)
            val parent = DocumentsContract.buildDocumentUriUsingTree(folder, treeId)
            val name = "${AutoBackup.FILE_PREFIX}${todayDate().toKey()}.json"
            val existing = children(folder).firstOrNull { it.second == name }?.first
            val target = existing
                ?: DocumentsContract.createDocument(resolver, parent, "application/json", name)
                ?: error("Could not create the backup file")
            resolver.openOutputStream(target, "wt")?.use { it.write(content.encodeToByteArray()) }
                ?: error("Could not open the backup file")

            children(folder)
                .filter { it.second.startsWith(AutoBackup.FILE_PREFIX) }
                .sortedByDescending { it.second }
                .drop(AutoBackup.KEEP)
                .forEach { runCatching { DocumentsContract.deleteDocument(resolver, it.first) } }

            prefs.edit().putLong(KEY_LAST, currentTimeMillis()).apply()
            _state.value = readState()
            true
        } catch (e: Exception) {
            _state.value = readState(error = "Backup failed. Choose the folder again with \"Change folder\".")
            false
        }
    }

    private fun children(tree: Uri): List<Pair<Uri, String>> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val result = mutableListOf<Pair<Uri, String>>()
        context.contentResolver.query(childrenUri, columns, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0) ?: continue
                val name = cursor.getString(1) ?: continue
                result += DocumentsContract.buildDocumentUriUsingTree(tree, id) to name
            }
        }
        return result
    }

    private fun folderName(tree: Uri): String = runCatching {
        val doc = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        context.contentResolver.query(doc, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
    }.getOrNull() ?: "Chosen folder"

    companion object {
        private const val PREFS = "auto_backup"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_FOLDER = "folder"
        private const val KEY_FOLDER_NAME = "folder_name"
        private const val KEY_LAST = "last"
        private const val DUE_AFTER_MILLIS = 20 * 60 * 60 * 1000L
        private const val REQUEST_CODE = 201

        private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        /** (Re)creates or cancels the daily alarm; also called after a reboot. */
        fun schedule(context: Context) {
            val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                Intent(context, AutoBackupReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarms.cancel(intent)
            if (!prefs(context).getBoolean(KEY_ENABLED, false)) return
            val first = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 3)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
            alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, first.timeInMillis, AlarmManager.INTERVAL_DAY, intent)
        }
    }
}

/** Fired by the daily alarm; writes the backup in the background. */
class AutoBackupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val holder = context.applicationContext as? AppContainerHolder ?: return
        val backup = holder.container.platform.autoBackup as? AndroidAutoBackup ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                backup.runBackup()
            } finally {
                pending.finish()
            }
        }
    }
}
