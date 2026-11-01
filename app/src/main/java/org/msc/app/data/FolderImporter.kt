package org.msc.app.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** SAF folder traversal; reads content URIs without assuming unrestricted storage paths. */
class FolderImporter(private val context: Context, private val repository: LibraryRepository) {
    suspend fun scan(tree: Uri): Pair<Int, Int> {
        val source = "folder:$tree"
        val found = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        val folders = java.util.ArrayDeque<Pair<String, String>>()
        folders.add(DocumentsContract.getTreeDocumentId(tree) to "Imported folder")
        var imported = 0; var failed = 0
        try {
            while (folders.isNotEmpty()) {
                currentCoroutineContext().ensureActive()
                val (id, path) = folders.removeFirst()
                if (!visited.add(id)) continue
                require(visited.size <= 5000) { "This folder is too large. Import a smaller subfolder." }
                val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, id)
                context.contentResolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)?.use { c ->
                    while (c.moveToNext()) {
                        currentCoroutineContext().ensureActive()
                        val childId = c.getString(0); val name = c.getString(1).orEmpty(); val mime = c.getString(2).orEmpty()
                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) folders.add(childId to "$path/$name")
                        else if (mime.startsWith("audio/") || name.substringAfterLast('.').lowercase() in setOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "opus")) {
                            require(found.size < 10000) { "Import up to 10,000 tracks at a time using smaller folders." }
                            val uri = DocumentsContract.buildDocumentUriUsingTree(tree, childId)
                            found += uri.toString()
                            try { repository.upsert(repository.importUri(uri).copy(folder = path, source = source)); imported++ }
                            catch (_: RuntimeException) { failed++ /* Unreadable files are counted, never reported as imported. */ }
                        }
                    }
                } ?: error("The selected folder is unavailable or permission was revoked.")
            }
            repository.tracks.value.filter { it.source == source && it.uri !in found }.forEach { repository.removeFromLibrary(it.uri) }
            return imported to failed
        } finally { repository.refresh() }
    }
}
