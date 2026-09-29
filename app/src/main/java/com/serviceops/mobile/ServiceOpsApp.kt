package com.serviceops.mobile

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

private enum class AppTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Work("Work", Icons.Default.Build),
    Assistant("Chat", Icons.Default.Chat),
    Plan("Plan", Icons.Default.CheckCircle),
    Billing("Billing", Icons.Default.ReceiptLong),
    Reference("Reference", Icons.Default.MenuBook)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceOpsApp(store: ServiceOpsStore) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Home) }
    var addDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SERVICE OPS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(selectedTab.label, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    if (selectedTab != AppTab.Assistant) {
                        IconButton(onClick = { addDialog = when (selectedTab) {
                            AppTab.Home, AppTab.Work -> "log"
                            AppTab.Plan -> "task"
                            AppTab.Billing -> "invoice"
                            AppTab.Reference -> "reference"
                            AppTab.Assistant -> null
                        } }) { Icon(Icons.Default.AddCircle, contentDescription = "Add") }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                AppTab.Home -> DashboardScreen(store, onQuickAdd = { addDialog = it }, onNavigate = { selectedTab = it })
                AppTab.Work -> WorkScreen(store, onAddLocation = { addDialog = "location" }, onAddLog = { addDialog = "log" })
                AppTab.Assistant -> AssistantScreen(store)
                AppTab.Plan -> PlannerScreen(store)
                AppTab.Billing -> BillingScreen(store)
                AppTab.Reference -> ReferenceScreen(store)
            }
        }
    }

    when (addDialog) {
        "location" -> LocationDialog(onDismiss = { addDialog = null }, onSave = { customer, name, address, type ->
            store.addLocation(customer, name, address, type); addDialog = null
        })
        "log" -> LogDialog(store, onDismiss = { addDialog = null }, onSave = { locationId, title, details ->
            store.addLog(locationId, title, details); addDialog = null
        })
        "task" -> TaskDialog(onDismiss = { addDialog = null }, onSave = { title, due, priority ->
            store.addTask(title, due, priority); addDialog = null
        })
        "invoice" -> InvoiceDialog(onDismiss = { addDialog = null }, onSave = { customer, description, amount, due ->
            store.addInvoice(customer, description, amount, due); addDialog = null
        })
        "reference" -> ReferenceDialog(onDismiss = { addDialog = null }, onSave = { title, body, tag ->
            store.addReference(title, body, tag); addDialog = null
        })
    }
}

@Composable
private fun DashboardScreen(store: ServiceOpsStore, onQuickAdd: (String) -> Unit, onNavigate: (AppTab) -> Unit) {
    val openTasks = store.tasks.count { !it.completed }
    val unpaid = store.invoices.filter { !it.paid }.sumOf { it.amount }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Business at a glance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Your field notes, schedule, billing, and references in one place.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Open tasks", openTasks.toString(), Icons.Default.CheckCircle, Modifier.weight(1f)) { onNavigate(AppTab.Plan) }
                SummaryCard("Unpaid", money(unpaid), Icons.Default.ReceiptLong, Modifier.weight(1f)) { onNavigate(AppTab.Billing) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Locations", store.locations.size.toString(), Icons.Default.Place, Modifier.weight(1f)) { onNavigate(AppTab.Work) }
                SummaryCard("Service logs", store.logs.size.toString(), Icons.Default.Build, Modifier.weight(1f)) { onNavigate(AppTab.Work) }
            }
        }
        item { SectionTitle("Quick add") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = { onQuickAdd("log") }, modifier = Modifier.weight(1f)) { Text("Service log") }
                FilledTonalButton(onClick = { onQuickAdd("task") }, modifier = Modifier.weight(1f)) { Text("Task") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = { onQuickAdd("invoice") }, modifier = Modifier.weight(1f)) { Text("Invoice") }
                FilledTonalButton(onClick = { onQuickAdd("reference") }, modifier = Modifier.weight(1f)) { Text("Reference") }
            }
        }
        if (store.logs.isNotEmpty()) {
            item { SectionTitle("Latest activity") }
            items(store.logs.take(3), key = { it.id }) { log ->
                LogCard(log, store.locationName(log.locationId))
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun WorkScreen(store: ServiceOpsStore, onAddLocation: () -> Unit, onAddLog: () -> Unit) {
    var showLocations by rememberSaveable { mutableStateOf(true) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = showLocations, onClick = { showLocations = true }, label = { Text("Locations") })
                FilterChip(selected = !showLocations, onClick = { showLocations = false }, label = { Text("Service logs") })
            }
        }
        item {
            Button(onClick = if (showLocations) onAddLocation else onAddLog, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (showLocations) "Add location" else "Record service visit")
            }
        }
        if (showLocations) {
            items(store.locations, key = { it.id }) { location ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(location.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(location.customer, color = MaterialTheme.colorScheme.primary)
                        Text(location.address.ifBlank { "No address added" })
                        AssistChip(onClick = {}, label = { Text(location.serviceType.ifBlank { "General service" }) })
                    }
                }
            }
        } else {
            items(store.logs, key = { it.id }) { log -> LogCard(log, store.locationName(log.locationId)) }
        }
    }
}

@Composable
private fun LogCard(log: ServiceLog, locationName: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(log.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(locationName, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            if (log.details.isNotBlank()) Text(log.details)
            if (log.photoPaths.isNotEmpty()) PhotoStrip(log.photoPaths)
            Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(log.createdAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AssistantScreen(store: ServiceOpsStore) {
    var locationId by rememberSaveable { mutableStateOf(store.locations.firstOrNull()?.id) }
    var message by rememberSaveable { mutableStateOf("") }
    var locationMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    val selectedPhotos = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val messages = store.chatMessages.filter { it.locationId == locationId }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
        uris.mapNotNull(store::importPhoto).forEach { path ->
            if (path !in selectedPhotos) selectedPhotos += path
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                OutlinedButton(onClick = { locationMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Place, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(store.locationName(locationId), maxLines = 1)
                }
                DropdownMenu(expanded = locationMenu, onDismissRequest = { locationMenu = false }) {
                    DropdownMenuItem(text = { Text("General") }, onClick = { locationId = null; locationMenu = false })
                    store.locations.forEach { location ->
                        DropdownMenuItem(text = { Text(location.name) }, onClick = { locationId = location.id; locationMenu = false })
                    }
                }
            }
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Local model settings")
            }
        }

        if (messages.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                Spacer(Modifier.height(10.dp))
                Text("Describe the visit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Add notes and photos. The local model will prepare a history entry for your approval.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { chat ->
                    ChatMessageCard(chat, store)
                }
                if (sending) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
        }

        if (selectedPhotos.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${selectedPhotos.size} photo${if (selectedPhotos.size == 1) "" else "s"} attached", style = MaterialTheme.typography.labelLarge)
                    PhotoStrip(selectedPhotos)
                    TextButton(onClick = { selectedPhotos.clear() }) { Text("Remove photos") }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(
                onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                enabled = !sending
            ) { Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Attach photos") }
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("What did you find or do?") },
                minLines = 1,
                maxLines = 4
            )
            IconButton(
                enabled = message.isNotBlank() && !sending,
                onClick = {
                    val sentText = message.trim()
                    val sentPhotos = selectedPhotos.toList()
                    val sentLocationId = locationId
                    val location = store.locations.firstOrNull { it.id == sentLocationId }
                    val history = store.logs.filter { it.locationId == sentLocationId }
                    val priorChat = store.chatMessages.filter { it.locationId == sentLocationId }
                    store.addChatMessage("user", sentText, sentLocationId, sentPhotos)
                    message = ""
                    selectedPhotos.clear()
                    sending = true
                    scope.launch {
                        try {
                            val draft = try {
                                LocalLlmClient.createLocationDraft(store.llmBaseUrl, store.llmModel, location, history, priorChat, sentText, sentPhotos)
                            } catch (visionError: Exception) {
                                if (sentPhotos.isEmpty()) throw visionError
                                val fallbackText = "$sentText\n\n${sentPhotos.size} photo(s) are attached to the record but were not analyzed by the text-only model."
                                LocalLlmClient.createLocationDraft(store.llmBaseUrl, store.llmModel, location, history, priorChat, fallbackText, emptyList())
                            }
                            store.addChatMessage("assistant", draft.reply, sentLocationId, sentPhotos, draft.title, draft.details)
                        } catch (error: Exception) {
                            store.addChatMessage(
                                "assistant",
                                "I couldn't reach the local model. Check that the server is running and that the address in Settings is reachable. ${error.message.orEmpty().take(160)}",
                                sentLocationId
                            )
                        } finally {
                            sending = false
                        }
                    }
                }
            ) { Icon(Icons.Default.Send, contentDescription = "Send") }
        }
    }

    if (showSettings) {
        LlmSettingsDialog(store, onDismiss = { showSettings = false })
    }
}

@Composable
private fun ChatMessageCard(message: ChatMessage, store: ServiceOpsStore) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(if (isUser) "You" else "Local assistant", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(message.text)
                if (isUser && message.photoPaths.isNotEmpty()) PhotoStrip(message.photoPaths)
                if (!isUser && message.draftTitle.isNotBlank()) {
                    HorizontalDivider()
                    Text(message.draftTitle, fontWeight = FontWeight.Bold)
                    Text(message.draftDetails)
                    Button(
                        onClick = { store.saveChatDraftToHistory(message.id) },
                        enabled = !message.savedToHistory,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(if (message.savedToHistory) Icons.Default.Check else Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (message.savedToHistory) "Saved to history" else "Save to location history")
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoStrip(paths: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        paths.take(3).forEach { path ->
            val bitmap = remember(path) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Attached service photo",
                    modifier = Modifier.size(width = 86.dp, height = 68.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
        if (paths.size > 3) Text("+${paths.size - 3}", modifier = Modifier.align(Alignment.CenterVertically), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LlmSettingsDialog(store: ServiceOpsStore, onDismiss: () -> Unit) {
    var baseUrl by remember { mutableStateOf(store.llmBaseUrl) }
    var model by remember { mutableStateOf(store.llmModel) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Local model") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("The Android emulator reaches this computer at 10.0.2.2. On your phone, use the computer's local Wi-Fi address.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Field(baseUrl, { baseUrl = it }, "OpenAI-compatible base URL")
                Field(model, { model = it }, "Model name")
            }
        },
        confirmButton = {
            Button(onClick = { store.updateLlmSettings(baseUrl, model); onDismiss() }, enabled = baseUrl.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PlannerScreen(store: ServiceOpsStore) {
    val sorted = store.tasks.sortedWith(compareBy<WorkTask> { it.completed }.thenBy { it.priority != "High" })
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Tap a task to mark it complete.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (sorted.isEmpty()) item { EmptyState("No tasks yet", "Add your next visit, callback, purchase, or deadline.") }
        items(sorted, key = { it.id }) { task ->
            Card(onClick = { store.toggleTask(task.id) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = task.completed, onCheckedChange = { store.toggleTask(task.id) })
                    Column(Modifier.weight(1f)) {
                        Text(task.title, fontWeight = FontWeight.SemiBold, textDecoration = if (task.completed) TextDecoration.LineThrough else null)
                        Text(task.due.ifBlank { "No due date" }, style = MaterialTheme.typography.bodySmall)
                    }
                    SuggestionChip(onClick = {}, label = { Text(task.priority) })
                }
            }
        }
    }
}

@Composable
private fun BillingScreen(store: ServiceOpsStore) {
    val unpaid = store.invoices.filter { !it.paid }.sumOf { it.amount }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Outstanding", style = MaterialTheme.typography.labelLarge)
                    Text(money(unpaid), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (store.invoices.isEmpty()) item { EmptyState("No invoices yet", "Create a record for completed work or recurring service.") }
        items(store.invoices, key = { it.id }) { invoice ->
            Card(onClick = { store.toggleInvoicePaid(invoice.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(invoice.customer, fontWeight = FontWeight.Bold)
                        Text(money(invoice.amount), fontWeight = FontWeight.Bold)
                    }
                    Text(invoice.description)
                    Text("Due: ${invoice.due.ifBlank { "Not set" }}")
                    Text(if (invoice.paid) "PAID" else "UNPAID", color = if (invoice.paid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReferenceScreen(store: ServiceOpsStore) {
    var search by rememberSaveable { mutableStateOf("") }
    val filtered = store.references.filter { search.isBlank() || it.title.contains(search, true) || it.body.contains(search, true) || it.tag.contains(search, true) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Search references") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        if (filtered.isEmpty()) item { EmptyState("Nothing found", "Add pricing notes, part numbers, procedures, or contract details.") }
        items(filtered, key = { it.id }) { note ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (note.tag.isNotBlank()) AssistChip(onClick = {}, label = { Text(note.tag) })
                    }
                    Text(note.body)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) = Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

@Composable
private fun EmptyState(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FormDialog(title: String, saveEnabled: Boolean, onDismiss: () -> Unit, onSave: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content) },
        confirmButton = { Button(onClick = onSave, enabled = saveEnabled) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun LocationDialog(onDismiss: () -> Unit, onSave: (String, String, String, String) -> Unit) {
    var customer by remember { mutableStateOf("") }; var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }; var type by remember { mutableStateOf("") }
    FormDialog("Add location", name.isNotBlank(), onDismiss, { onSave(customer, name, address, type) }) {
        Field(customer, { customer = it }, "Customer")
        Field(name, { name = it }, "Location name")
        Field(address, { address = it }, "Address")
        Field(type, { type = it }, "Services (landscaping, HVAC PM…)")
    }
}

@Composable
private fun LogDialog(store: ServiceOpsStore, onDismiss: () -> Unit, onSave: (Long?, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }; var details by remember { mutableStateOf("") }
    var locationId by remember { mutableStateOf(store.locations.firstOrNull()?.id) }; var expanded by remember { mutableStateOf(false) }
    FormDialog("Record service", title.isNotBlank(), onDismiss, { onSave(locationId, title, details) }) {
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(store.locationName(locationId)) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("General") }, onClick = { locationId = null; expanded = false })
                store.locations.forEach { location -> DropdownMenuItem(text = { Text(location.name) }, onClick = { locationId = location.id; expanded = false }) }
            }
        }
        Field(title, { title = it }, "Work performed")
        Field(details, { details = it }, "Condition, parts, readings, and follow-up", singleLine = false)
    }
}

@Composable
private fun TaskDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }; var due by remember { mutableStateOf("") }; var priority by remember { mutableStateOf("Normal") }
    FormDialog("Add task", title.isNotBlank(), onDismiss, { onSave(title, due, priority) }) {
        Field(title, { title = it }, "Task")
        Field(due, { due = it }, "Due (example: Friday or Oct 3)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Low", "Normal", "High").forEach { value -> FilterChip(selected = priority == value, onClick = { priority = value }, label = { Text(value) }) }
        }
    }
}

@Composable
private fun InvoiceDialog(onDismiss: () -> Unit, onSave: (String, String, Double, String) -> Unit) {
    var customer by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }; var due by remember { mutableStateOf("") }
    FormDialog("Add invoice", customer.isNotBlank() && amount.toDoubleOrNull() != null, onDismiss, { onSave(customer, description, amount.toDouble(), due) }) {
        Field(customer, { customer = it }, "Customer")
        Field(description, { description = it }, "Description")
        Field(amount, { amount = it.filter { char -> char.isDigit() || char == '.' } }, "Amount")
        Field(due, { due = it }, "Due date")
    }
}

@Composable
private fun ReferenceDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }; var body by remember { mutableStateOf("") }; var tag by remember { mutableStateOf("") }
    FormDialog("Add reference", title.isNotBlank(), onDismiss, { onSave(title, body, tag) }) {
        Field(title, { title = it }, "Title")
        Field(tag, { tag = it }, "Tag (Parts, Pricing, Contract…)")
        Field(body, { body = it }, "Details", singleLine = false)
    }
}

@Composable
private fun Field(value: String, onValueChange: (String) -> Unit, label: String, singleLine: Boolean = true) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = singleLine, minLines = if (singleLine) 1 else 3)
}

private fun money(value: Double): String = NumberFormat.getCurrencyInstance().format(value)
