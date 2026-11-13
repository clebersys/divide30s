package com.cleber.videosplit30

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max

/**
 * Divide um vídeo em partes de N segundos SEM reencodar.
 *
 * A divisão é feita em nível de container: os samples comprimidos são copiados
 * do arquivo original para novos arquivos. Os cortes acontecem no primeiro
 * keyframe (I-frame) a partir do tempo alvo, então cada parte tem ~30s e a
 * qualidade/velocidade do vídeo permanece exatamente a mesma.
 */
object VideoSplitter {

    const val DEFAULT_SEGMENT_US: Long = 30_000_000L
    private const val MAX_SEGMENTS = 5_000
    private const val BUFFER_SIZE = 16 * 1024 * 1024

    data class Output(
        val directory: File,
        val files: List<File>,
        val segmentDurationsUs: List<Long>,
    )

    fun split(
        context: Context,
        uri: Uri,
        outputDir: File,
        segmentUs: Long = DEFAULT_SEGMENT_US,
        onProgress: (Float) -> Unit = {},
        checkActive: () -> Unit = {},
    ): Output {
        require(segmentUs > 0) { "Duração da parte inválida" }
        outputDir.mkdirs()
        outputDir.listFiles()?.forEach { it.delete() }

        val extractor = MediaExtractor()
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: error("Não foi possível abrir o vídeo selecionado")

        try {
            pfd.use { descriptor ->
                extractor.setDataSource(descriptor.fileDescriptor)

                val selected = LinkedHashMap<Int, MediaFormat>()
                var videoTrack = -1
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    when {
                        mime.startsWith("video/") && videoTrack == -1 -> {
                            videoTrack = i
                            selected[i] = format
                        }
                        mime.startsWith("audio/") -> selected[i] = format
                    }
                }
                check(videoTrack >= 0) { "O arquivo selecionado não tem faixa de vídeo" }

                selected.keys.forEach { track ->
                    runCatching { extractor.selectTrack(track) }
                }
                extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                val videoFormat = selected.getValue(videoTrack)
                val durationUs = if (videoFormat.containsKey(MediaFormat.KEY_DURATION)) {
                    videoFormat.getLong(MediaFormat.KEY_DURATION)
                } else {
                    -1L
                }
                val rotation = if (videoFormat.containsKey(MediaFormat.KEY_ROTATION)) {
                    videoFormat.getInteger(MediaFormat.KEY_ROTATION)
                } else {
                    0
                }
                val container = chooseContainer(videoFormat)

                val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE)
                val info = MediaCodec.BufferInfo()

                val durations = ArrayList<Long>()
                var writer: SegmentWriter? = null
                var segmentIndex = 0
                var segmentStartUs = 0L
                var samples = 0L

                fun openNext(startUs: Long) {
                    writer?.let { previous ->
                        val duration = previous.close()
                        if (duration > 0) durations.add(duration)
                    }
                    segmentIndex++
                    writer = SegmentWriter(
                        directory = outputDir,
                        index = segmentIndex,
                        extension = container.second,
                        outputFormat = container.first,
                        rotation = rotation,
                        tracks = selected,
                        videoTrack = videoTrack,
                        startUs = startUs,
                    )
                }

                openNext(0L)

                while (true) {
                    checkActive()
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) break

                    val track = extractor.sampleTrackIndex
                    val timeUs = extractor.sampleTime
                    val flags = extractor.sampleFlags

                    val current = writer!!
                    val isVideoKeyframe = track == videoTrack &&
                        (flags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0

                    if (isVideoKeyframe &&
                        timeUs - segmentStartUs >= segmentUs &&
                        current.hasVideoSample &&
                        segmentIndex < MAX_SEGMENTS
                    ) {
                        // Fecha a parte atual e começa a próxima exatamente neste keyframe.
                        segmentStartUs = timeUs
                        openNext(timeUs)
                    }

                    var muxerFlags = 0
                    if (flags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                        muxerFlags = muxerFlags or MediaCodec.BUFFER_FLAG_KEY_FRAME
                    }
                    if (flags and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME != 0) {
                        muxerFlags = muxerFlags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
                    }
                    info.set(0, size, timeUs, muxerFlags)
                    writer!!.write(track, buffer, info)

                    samples++
                    if (samples % 64L == 0L && durationUs > 0) {
                        onProgress((timeUs.toFloat() / durationUs).coerceIn(0f, 0.999f))
                    }

                    extractor.advance()
                }

                writer?.let { last ->
                    val duration = last.close()
                    if (duration > 0) durations.add(duration)
                }

                val files = outputDir.listFiles { file -> file.isFile }
                    ?.sortedBy { it.name }
                    ?: emptyList()
                check(files.isNotEmpty()) { "Nenhuma parte foi gerada" }
                onProgress(1f)
                return Output(outputDir, files, durations)
            }
        } finally {
            extractor.release()
        }
    }

    private fun chooseContainer(videoFormat: MediaFormat): Pair<Int, String> {
        val mime = videoFormat.getString(MediaFormat.KEY_MIME) ?: ""
        val isWebm = mime == "video/webm" ||
            mime.startsWith("video/x-vnd.on2.vp") ||
            mime == "video/av01"
        return if (isWebm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM to "webm"
        } else {
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4 to "mp4"
        }
    }

    private class SegmentWriter(
        private val directory: File,
        index: Int,
        extension: String,
        outputFormat: Int,
        private val rotation: Int,
        tracks: Map<Int, MediaFormat>,
        private val videoTrack: Int,
        val startUs: Long,
    ) {
        private val file = File(directory, "parte_${"%04d".format(index)}.$extension")
        private val muxer = MediaMuxer(file.absolutePath, outputFormat)
        private val trackMap = HashMap<Int, Int>()
        private var started = false
        private var wroteAny = false
        private var lastPtsUs = 0L
        var hasVideoSample = false
            private set

        init {
            tracks.forEach { (sourceTrack, format) ->
                trackMap[sourceTrack] = muxer.addTrack(format)
            }
        }

        fun write(sourceTrack: Int, buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
            val muxerTrack = trackMap[sourceTrack] ?: return
            if (!started) {
                if (rotation != 0) muxer.setOrientationHint(rotation)
                muxer.start()
                started = true
            }
            val adjusted = MediaCodec.BufferInfo().apply {
                set(
                    info.offset,
                    info.size,
                    max(0L, info.presentationTimeUs - startUs),
                    info.flags,
                )
            }
            buffer.position(info.offset)
            buffer.limit(info.offset + info.size)
            muxer.writeSampleData(muxerTrack, buffer, adjusted)
            buffer.clear()

            wroteAny = true
            if (sourceTrack == videoTrack) hasVideoSample = true
            if (adjusted.presentationTimeUs > lastPtsUs) lastPtsUs = adjusted.presentationTimeUs
        }

        fun close(): Long {
            val duration = if (wroteAny) lastPtsUs else 0L
            if (started) {
                runCatching { muxer.stop() }
            }
            runCatching { muxer.release() }
            started = false
            return duration
        }
    }
}
