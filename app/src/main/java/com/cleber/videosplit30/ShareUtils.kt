package com.cleber.videosplit30

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ShareUtils {

    private val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")

    fun shareToWhatsApp(
        context: Context,
        files: List<File>,
        mimeType: String,
        asDocument: Boolean = false,
    ) {
        // Como documento (application/octet-stream) o WhatsApp não recomprime o vídeo.
        val shareMime = if (asDocument) "application/octet-stream" else mimeType
        val base = buildIntent(context, files, shareMime)
        val installed = WHATSAPP_PACKAGES.firstOrNull { pkg ->
            runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
        }
        if (installed != null) {
            val direct = Intent(base).setPackage(installed)
            try {
                context.startActivity(direct)
                return
            } catch (_: ActivityNotFoundException) {
                // segue para o seletor padrão
            } catch (_: SecurityException) {
                // alguns aparelhos bloqueiam o envio direto; usa o seletor
            }
        }
        shareToAnyApp(context, files, shareMime)
    }

    fun shareToAnyApp(context: Context, files: List<File>, mimeType: String) {
        val intent = buildIntent(context, files, mimeType)
        context.startActivity(Intent.createChooser(intent, "Compartilhar vídeos"))
    }

    private fun buildIntent(context: Context, files: List<File>, mimeType: String): Intent {
        val uris = files.map { file ->
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        }
        val clip = ClipData.newRawUri("vídeos", uris.first())
        uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }

        return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(uris))
            clipData = clip
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
