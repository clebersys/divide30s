package com.cleber.videosplit30

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            AppTheme {
                val vm: SplitViewModel = viewModel()
                val state by vm.state.collectAsState()
                val context = LocalContext.current

                // Voltar (inclusive o gesto preditivo) retorna ao início em vez de
                // fechar o app quando já existe um resultado ou um erro na tela.
                BackHandler(
                    enabled = state is SplitViewModel.State.Done ||
                        state is SplitViewModel.State.Error,
                ) {
                    vm.reset()
                }

                val picker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { uri -> uri?.let(vm::onVideoPicked) }

                fun pick() = picker.launch(arrayOf("video/*"))

                Scaffold(containerColor = Color.Transparent) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(BgTop, BgMiddle, BgBottom),
                                ),
                            )
                            .padding(padding),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                // O espaço extra embaixo reserva lugar para o crédito.
                                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 36.dp),
                        ) {
                            Header()
                            Spacer(Modifier.height(18.dp))

                            when (val current = state) {
                                SplitViewModel.State.Idle -> IdleContent(onPick = ::pick)

                                is SplitViewModel.State.Ready -> ReadyContent(
                                    ready = current,
                                    onPick = ::pick,
                                    onSplit = { vm.start(30) },
                                )

                                is SplitViewModel.State.Splitting -> SplittingContent(
                                    splitting = current,
                                    onCancel = vm::cancel,
                                )

                                is SplitViewModel.State.Done -> DoneContent(
                                    done = current,
                                    onWhatsApp = {
                                        runCatching {
                                            ShareUtils.shareToWhatsApp(
                                                context,
                                                current.files,
                                                current.mimeType,
                                            )
                                        }.onFailure {
                                            Toast.makeText(
                                                context,
                                                "Não foi possível abrir o compartilhamento",
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                    },
                                    onOtherApps = {
                                        runCatching {
                                            ShareUtils.shareToAnyApp(
                                                context,
                                                current.files,
                                                current.mimeType,
                                            )
                                        }.onFailure {
                                            Toast.makeText(
                                                context,
                                                "Não foi possível compartilhar",
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                    },
                                    onWhatsAppDocument = {
                                        runCatching {
                                            ShareUtils.shareToWhatsApp(
                                                context,
                                                current.files,
                                                current.mimeType,
                                                asDocument = true,
                                            )
                                        }.onFailure {
                                            Toast.makeText(
                                                context,
                                                "Não foi possível abrir o compartilhamento",
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                    },
                                    onReset = vm::reset,
                                )

                                is SplitViewModel.State.Error -> ErrorContent(
                                    message = current.message,
                                    onRetry = vm::reset,
                                )
                            }
                        }

                        CreditFooter(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreditFooter(modifier: Modifier = Modifier) {
    Text(
        text = "Desenvolvido por Cleber Reis",
        color = TextMuted.copy(alpha = 0.8f),
        fontSize = 11.sp,
        modifier = modifier,
    )
}

@Composable
private fun Header() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Accent.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "Divide 30s",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Divide sem perder qualidade",
                color = TextMuted,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun IdleContent(onPick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .background(Accent.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(64.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = "Escolha um vídeo para dividir",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Cada parte terá cerca de 30 segundos. O vídeo não é reencodado e não perde qualidade.",
            color = TextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(32.dp))
        PrimaryButton(text = "Escolher vídeo", onClick = onPick)
    }
}

@Composable
private fun ReadyContent(
    ready: SplitViewModel.State.Ready,
    onPick: () -> Unit,
    onSplit: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        InfoCard(
            title = "Vídeo selecionado",
            lines = listOf(
                ready.name,
                "${formatDuration(ready.durationMs)}  •  ${formatBytes(ready.sizeBytes)}",
            ),
        )
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Serão geradas aproximadamente ${estimateParts(ready.durationMs)} partes de 30s.",
            color = TextMuted,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(24.dp))

        PrimaryButton(text = "Dividir em partes de 30s", onClick = onSplit)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onPick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("Escolher outro vídeo", color = TextPrimary)
        }
    }
}

@Composable
private fun SplittingContent(
    splitting: SplitViewModel.State.Splitting,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Dividindo…",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = splitting.name,
            color = TextMuted,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(28.dp))
        LinearProgressIndicator(
            progress = { splitting.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            color = Accent,
            trackColor = Accent.copy(alpha = 0.15f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "${(splitting.progress * 100).toInt()}%",
            color = Accent,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "A cópia é feita sem reencodar, então é rápida e mantém a qualidade original.",
            color = TextMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        TextButton(onClick = onCancel) {
            Text("Cancelar", color = TextMuted)
        }
    }
}

@Composable
private fun DoneContent(
    done: SplitViewModel.State.Done,
    onWhatsApp: () -> Unit,
    onOtherApps: () -> Unit,
    onWhatsAppDocument: () -> Unit,
    onReset: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Success,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "${done.files.size} partes geradas",
                    color = TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Total: ${formatDuration(done.totalDurationMs)}  •  ${done.sourceName}",
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(done.files, key = { it.absolutePath }) { file ->
                    FileRow(file)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        WhatsAppButton(onClick = onWhatsApp)
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onWhatsAppDocument,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = TextPrimary)
            Spacer(Modifier.width(8.dp))
            Text("Enviar como documento (sem compressão)", color = TextPrimary, fontSize = 14.sp)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onOtherApps,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = TextPrimary)
            Spacer(Modifier.width(8.dp))
            Text("Compartilhar em outro app", color = TextPrimary)
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = TextMuted)
            Spacer(Modifier.width(6.dp))
            Text("Nova divisão", color = TextMuted)
        }
    }
}

@Composable
private fun FileRow(file: File) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(Accent.copy(alpha = 0.12f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = formatBytes(file.length()),
                color = TextMuted,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = Danger,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Não foi possível dividir",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            color = TextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(26.dp))
        PrimaryButton(text = "Escolher outro vídeo", onClick = onRetry)
    }
}

@Composable
private fun InfoCard(title: String, lines: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = lines.first(),
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (lines.size > 1) {
                Spacer(Modifier.height(4.dp))
                Text(text = lines[1], color = TextMuted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent,
            contentColor = BgTop,
        ),
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WhatsAppButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = WhatsAppGreen,
            contentColor = Color(0xFF062014),
        ),
    ) {
        Icon(Icons.Default.Share, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Compartilhar no WhatsApp", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = BgTop,
            background = BgTop,
            onBackground = TextPrimary,
            surface = Surface,
            onSurface = TextPrimary,
        ),
        content = content,
    )
}

private val BgTop = Color(0xFF0B1220)
private val BgMiddle = Color(0xFF101B2E)
private val BgBottom = Color(0xFF0B1220)
private val Surface = Color(0xFF16213A)
private val Accent = Color(0xFF38BDF8)
private val TextPrimary = Color(0xFFE8EEF8)
private val TextMuted = Color(0xFF94A3B8)
private val Success = Color(0xFF34D399)
private val Danger = Color(0xFFF87171)
private val WhatsAppGreen = Color(0xFF25D366)

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "duração desconhecida"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "tamanho desconhecido"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return String.format(Locale.getDefault(), "%.1f %s", value, units[index])
}

private fun estimateParts(durationMs: Long): Int {
    if (durationMs <= 0) return 1
    return ((durationMs + 29_999) / 30_000).toInt().coerceAtLeast(1)
}
