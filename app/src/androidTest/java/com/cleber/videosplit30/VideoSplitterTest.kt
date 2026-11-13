package com.cleber.videosplit30

import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoSplitterTest {

    @Test
    fun dividesLongVideoIntoLosslessParts() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        val input = File(context.cacheDir, "sample.mp4")
        instrumentation.context.assets.open("sample.mp4").use { source ->
            input.outputStream().use { target -> source.copyTo(target) }
        }
        assertTrue("vídeo de teste ausente", input.length() > 0)

        val outputDir = File(context.cacheDir, "split_out")
        val result = VideoSplitter.split(
            context = context,
            uri = Uri.fromFile(input),
            outputDir = outputDir,
            segmentUs = 30_000_000L,
            checkActive = {},
        )

        // 65s com keyframe a cada 2s -> 3 partes (0-30, 30-60, 60-65)
        assertTrue("esperava 3 partes, veio ${result.files.size}", result.files.size == 3)
        assertTrue("nomes fora de ordem", result.files.map { it.name }.sorted() == result.files.map { it.name })

        val total = result.files.sumOf { it.length() }
        assertTrue("partes vazias", result.files.all { it.length() > 0 })
        assertTrue(
            "tamanho total ($total) muito diferente do original (${input.length()})",
            total > input.length() * 0.85 && total < input.length() * 1.3,
        )

        // Todas as partes precisam ser arquivos de vídeo válidos.
        val retriever = MediaMetadataRetriever()
        try {
            result.files.forEach { file ->
                retriever.setDataSource(file.absolutePath)
                val duration = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
                val width = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                assertTrue("${file.name} sem duração", duration > 500)
                assertTrue("${file.name} com largura errada ($width)", width == "320")
            }
        } finally {
            retriever.release()
        }

        // As duas primeiras partes devem ter ~30s; a última leva o resto.
        assertTrue(
            "primeira parte com duração inesperada: ${result.segmentDurationsUs[0]}",
            result.segmentDurationsUs[0] in 28_000_000L..32_000_000L,
        )
        assertTrue(
            "segunda parte com duração inesperada: ${result.segmentDurationsUs[1]}",
            result.segmentDurationsUs[1] in 28_000_000L..32_000_000L,
        )
    }
}
