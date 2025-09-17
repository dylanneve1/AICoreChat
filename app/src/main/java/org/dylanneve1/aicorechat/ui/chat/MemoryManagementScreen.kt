package org.dylanneve1.aicorechat.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dylanneve1.aicorechat.data.MemoryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryManagementScreen(
    memoryEntries: List<MemoryEntry>,
    onAddMemory: (String) -> Unit,
    onUpdateMemory: (MemoryEntry) -> Unit,
    onDeleteMemory: (String) -> Unit,
    onToggleMemory: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingMemory by remember { mutableStateOf<MemoryEntry?>(null) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Memory Management") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
            floatingActionButton = {
                androidx.compose.material3.FloatingActionButton(
                    onClick = {
                        editingMemory = null
                        showDialog = true
                    },
                    modifier = Modifier.padding(16.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add Memory")
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 8.dp),
            ) {
                // Header using SectionHeaderCard for consistency
                org.dylanneve1.aicorechat.ui.components.SectionHeaderCard(
                    icon = Icons.Outlined.Memory,
                    title = "Memory Entries",
                    description = "${memoryEntries.size} ${if (memoryEntries.size == 1) "entry" else "entries"} stored",
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                if (memoryEntries.isEmpty()) {
                    org.dylanneve1.aicorechat.ui.components.EmptyStateView(
                        icon = Icons.Outlined.Memory,
                        title = "No Memories Yet",
                        description = "Add memory entries to help the AI remember important information about you",
                        modifier = Modifier.padding(vertical = 48.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(memoryEntries, key = { it.id }) { memory ->
                            MemoryCard(
                                memory = memory,
                                onToggle = { onToggleMemory(memory.id) },
                                onEdit = {
                                    editingMemory = memory
                                    showDialog = true
                                },
                                onDelete = { onDeleteMemory(memory.id) },
                            )
                        }
                        item {
                            // Padding for FAB
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }

    // Memory Dialog (Add/Edit)
    if (showDialog) {
        MemoryDialog(
            editingMemory = editingMemory,
            onDismiss = {
                showDialog = false
                editingMemory = null
            },
            onConfirm = { content ->
                if (editingMemory != null) {
                    // Update existing memory
                    val updatedMemory = editingMemory!!.copy(
                        content = content,
                        updatedAt = System.currentTimeMillis(),
                    )
                    onUpdateMemory(updatedMemory)
                } else {
                    // Add new memory
                    onAddMemory(content)
                }
                showDialog = false
                editingMemory = null
            },
        )
    }
}

@Composable
private fun MemoryCard(memory: MemoryEntry, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val dateFormat = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (memory.isEnabled) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Memory,
                        contentDescription = null,
                        tint = if (memory.isEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 2.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = memory.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (memory.isEnabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(bottom = 8.dp),
                        )

                        Text(
                            text = dateFormat.format(java.util.Date(memory.updatedAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    androidx.compose.material3.Switch(
                        checked = memory.isEnabled,
                        onCheckedChange = { onToggle() },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Row {
                        androidx.compose.material3.IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            androidx.compose.material3.Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Edit",
                                tint = if (memory.isEnabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        androidx.compose.material3.IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            androidx.compose.material3.Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryDialog(editingMemory: MemoryEntry?, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var memoryContent by remember(editingMemory) {
        mutableStateOf(editingMemory?.content ?: "")
    }
    val isValid = memoryContent.isNotBlank()

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 600.dp),
            ) {
                Text(
                    text = if (editingMemory != null) "Edit Memory" else "Add Memory",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(modifier = Modifier.height(20.dp))

                androidx.compose.material3.OutlinedTextField(
                    value = memoryContent,
                    onValueChange = { memoryContent = it },
                    label = { Text("Memory Content") },
                    placeholder = { Text("What should the AI remember about you?") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(24.dp))

                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    androidx.compose.material3.TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    androidx.compose.material3.Button(
                        onClick = { onConfirm(memoryContent.trim()) },
                        enabled = isValid,
                    ) {
                        Text(if (editingMemory != null) "Update" else "Save")
                    }
                }
            }
        }
    }
}
