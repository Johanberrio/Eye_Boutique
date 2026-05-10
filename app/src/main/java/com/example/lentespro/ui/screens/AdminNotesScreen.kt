package com.example.lentespro.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lentespro.ui.viewmodel.AdminNotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNotesScreen(
    viewModel: AdminNotesViewModel,
    isSuperAdmin: Boolean,
    onBack: () -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    var editMode by remember { mutableStateOf(false) }
    var textState by remember(notes) { mutableStateOf(notes) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Información / Notas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (isSuperAdmin && !editMode) {
                        IconButton(onClick = { editMode = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (editMode && isSuperAdmin) {
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    label = { Text("Escribe la información aquí") },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    minLines = 10
                )
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.updateNotes(textState)
                            editMode = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar y Publicar")
                    }
                    
                    OutlinedButton(
                        onClick = { editMode = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = if (notes.isBlank()) "No hay información registrada." else notes,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
