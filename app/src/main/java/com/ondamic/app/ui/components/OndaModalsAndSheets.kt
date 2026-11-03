package com.ondamic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ondamic.app.R
import com.ondamic.app.audio.SavedRecordingItem
import com.ondamic.app.data.LanguageCatalog
import com.ondamic.app.ui.theme.AmberGuard
import com.ondamic.app.ui.theme.OnAirCoral
import com.ondamic.app.ui.theme.OndaCyan
import com.ondamic.app.ui.theme.OndaEmerald
import com.ondamic.app.ui.theme.SurfaceCardElevated
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerSheet(
    onDismiss: () -> Unit,
    onSelectLanguage: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val currentTag = remember { LanguageCatalog.getCurrentLanguageTag() }
    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            LanguageCatalog.languages
        } else {
            LanguageCatalog.languages.filter {
                it.nativeName.contains(searchQuery, ignoreCase = true) ||
                    it.regionGroup.contains(searchQuery, ignoreCase = true) ||
                    it.bcp47Tag.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "🌍 ${stringResource(R.string.btn_language)} (36+ Languages)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Português, English, Kreyòl, Kiswahili, Yorùbá…") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered) { lang ->
                    val isSelected = lang.bcp47Tag.equals(currentTag, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) SurfaceCardElevated else MaterialTheme.colorScheme.surface)
                            .clickable {
                                onSelectLanguage(lang.bcp47Tag)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = lang.flagEmoji, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = lang.nativeName,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) OndaCyan else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${lang.regionGroup} ${if (lang.bcp47Tag.isNotEmpty()) "• [${lang.bcp47Tag}]" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = OndaCyan
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun InternalSpeakerWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = AmberGuard
            )
        },
        title = {
            Text(
                text = stringResource(R.string.speaker_warning_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(text = stringResource(R.string.speaker_warning_body))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AmberGuard)
            ) {
                Text(
                    text = stringResource(R.string.speaker_warning_confirm),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.speaker_warning_cancel))
            }
        }
    )
}

@Composable
fun SaveCustomPresetDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.btn_save_preset), fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Ex: Sala de Aula / Caixa JBL / Igreja") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name.ifBlank { "Meu Perfil OndaMic" })
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.btn_save_preset))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.speaker_warning_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedRecordingsSheet(
    recordings: List<SavedRecordingItem>,
    onShare: (File) -> Unit,
    onDelete: (File) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "🎙️ ${stringResource(R.string.btn_recordings)} (.WAV)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (recordings.isEmpty()) {
                Text(
                    text = "Nenhuma gravação local salva ainda. Ative 'Gravação Local Simultânea em WAV' na aba 3 para gravar enquanto amplifica.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recordings) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceCardElevated)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (item.isStereo) OndaCyan else OndaEmerald
                                )
                                val kb = item.sizeBytes / 1024
                                Text(
                                    text = "${if (item.isStereo) "STEREO L+R" else "MONO"} • ${item.durationSec}s • ${kb} KB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row {
                                IconButton(onClick = { onShare(item.file) }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = OndaCyan)
                                }
                                IconButton(onClick = { onDelete(item.file) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = OnAirCoral)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
