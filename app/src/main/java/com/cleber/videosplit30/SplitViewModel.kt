package com.cleber.videosplit30

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class SplitViewModel(application: Application) : AndroidViewModel(application) {

    sealed interface State {
        data object Idle : State

        data class Ready(
            val uri: Uri,
            val name: String,
            val durationMs: Long,
            val sizeBytes: Long,
        ) : State

        data class Splitting(
            val name: String,
            val progress: Float,
        ) : State

        data class Done(
            val directory: File,
            val files: List<File>,
            val mimeType: String,
            val totalDurationMs: Long,
            val sourceName: String,
        ) : State

        data class Error(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private var job: Job? = null

    fun onVideoPicked(uri: Uri) {
        viewModelScope.launch {
            _state.value = State.Idle
            val info = withContext(Dispatchers.IO) {
                clearPreviousSplits()
                readMetadata(uri)
            }
            _state.value = State.Ready(
                uri = uri,
                name = info.name,
                durationMs = info.durationMs,
                sizeBytes = info.sizeBytes,
            )
        }
    }

    fun start(segmentSeconds: Int = 30) {
        val ready = _state.value as? State.Ready ?: return
        if (job?.isActive == true) return

        _state.value = State.Splitting(ready.name, 0f)
        job = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val app = getApplication<Application>()
                    val root = app.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                        ?: File(app.filesDir, "videos")
                    val base = File(root, "Divide30s")

                    if (ready.sizeBytes > 0) {
                        val available = StatFs(root.absolutePath).availableBytes
                        val needed = ready.sizeBytes + 100L * 1024 * 1024
                        if (available < needed) {
                            error(
                                "Espaço insuficiente: libere cerca de " +
                                    formatGigabytes(needed) + " no aparelho.",
                            )
                        }
                    }

                    clearPreviousSplits()

                    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val dir = File(base, stamp)

                    VideoSplitter.split(
                        context = app,
                        uri = ready.uri,
                        outputDir = dir,
                        segmentUs = segmentSeconds * 1_000_000L,
                        onProgress = { progress ->
                            _state.value = State.Splitting(ready.name, progress)
                        },
                        checkActive = { coroutineContext.ensureActive() },
                    )
                }

                val mime = if (result.files.first().extension.equals("webm", true)) {
                    "video/webm"
                } else {
                    "video/mp4"
                }
                _state.value = State.Done(
                    directory = result.directory,
                    files = result.files,
                    mimeType = mime,
                    totalDurationMs = result.segmentDurationsUs.sum() / 1000L,
                    sourceName = ready.name,
                )
            } catch (cancelled: CancellationException) {
                _state.value = State.Idle
                throw cancelled
            } catch (error: Exception) {
                _state.value = State.Error(
                    error.message ?: "Falha ao dividir o vídeo",
                )
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _state.value = State.Idle
    }

    fun reset() {
        job?.cancel()
        job = null
        _state.value = State.Idle
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
    }

    private data class VideoInfo(
        val name: String,
        val durationMs: Long,
        val sizeBytes: Long,
    )

    /** Apaga as partes de divisões anteriores; elas não são mais necessárias. */
    private fun clearPreviousSplits() {
        val app = getApplication<Application>()
        val root = app.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: File(app.filesDir, "videos")
        File(root, "Divide30s").listFiles()?.forEach { old ->
            runCatching { old.deleteRecursively() }
        }
    }

    private fun formatGigabytes(bytes: Long): String =
        String.format(Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))

    private fun readMetadata(uri: Uri): VideoInfo {
        val app = getApplication<Application>()
        var name = "vídeo"
        var size = 0L

        runCatching {
            app.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) cursor.getString(nameIndex)?.let { name = it }
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                }
            }
        }

        var duration = 0L
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(app, uri)
            duration = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            // alguns provedores não expõem metadados; segue com duração desconhecida
        } finally {
            runCatching { retriever.release() }
        }

        return VideoInfo(name, duration, size)
    }
}
