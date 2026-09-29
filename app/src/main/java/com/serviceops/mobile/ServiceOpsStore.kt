package com.serviceops.mobile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class ServiceOpsStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("service_ops_data", Context.MODE_PRIVATE)

    val locations = mutableStateListOf<Location>()
    val logs = mutableStateListOf<ServiceLog>()
    val tasks = mutableStateListOf<WorkTask>()
    val invoices = mutableStateListOf<Invoice>()
    val references = mutableStateListOf<ReferenceNote>()
    val chatMessages = mutableStateListOf<ChatMessage>()

    var llmBaseUrl by mutableStateOf(preferences.getString("llm_base_url", "http://10.0.2.2:8080/v1") ?: "http://10.0.2.2:8080/v1")
        private set
    var llmModel by mutableStateOf(preferences.getString("llm_model", "cyber-tiel") ?: "cyber-tiel")
        private set

    init {
        load()
        if (locations.isEmpty() && logs.isEmpty() && tasks.isEmpty() && invoices.isEmpty() && references.isEmpty()) {
            addStarterData()
        }
    }

    fun addLocation(customer: String, name: String, address: String, serviceType: String) {
        locations += Location(nextId(), customer.trim(), name.trim(), address.trim(), serviceType.trim())
        save()
    }

    fun addLog(locationId: Long?, title: String, details: String, photoPaths: List<String> = emptyList()) {
        logs.add(0, ServiceLog(nextId(), locationId, title.trim(), details.trim(), System.currentTimeMillis(), photoPaths))
        save()
    }

    fun addChatMessage(
        role: String,
        text: String,
        locationId: Long?,
        photoPaths: List<String> = emptyList(),
        draftTitle: String = "",
        draftDetails: String = ""
    ): Long {
        val id = nextId()
        chatMessages += ChatMessage(
            id = id,
            role = role,
            text = text.trim(),
            locationId = locationId,
            createdAt = System.currentTimeMillis(),
            photoPaths = photoPaths,
            draftTitle = draftTitle.trim(),
            draftDetails = draftDetails.trim()
        )
        save()
        return id
    }

    fun saveChatDraftToHistory(messageId: Long) {
        val index = chatMessages.indexOfFirst { it.id == messageId }
        if (index < 0) return
        val message = chatMessages[index]
        if (message.savedToHistory || message.draftTitle.isBlank()) return
        logs.add(0, ServiceLog(
            id = nextId(),
            locationId = message.locationId,
            title = message.draftTitle,
            details = message.draftDetails,
            createdAt = System.currentTimeMillis(),
            photoPaths = message.photoPaths
        ))
        chatMessages[index] = message.copy(savedToHistory = true)
        save()
    }

    fun updateLlmSettings(baseUrl: String, model: String) {
        llmBaseUrl = baseUrl.trim().trimEnd('/')
        llmModel = model.trim().ifBlank { "cyber-tiel" }
        preferences.edit {
            putString("llm_base_url", llmBaseUrl)
            putString("llm_model", llmModel)
        }
    }

    fun importPhoto(uri: Uri): String? = runCatching {
        val directory = File(appContext.filesDir, "service_photos").apply { mkdirs() }
        val destination = File(directory, "photo_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        appContext.contentResolver.openInputStream(uri).use { input -> BitmapFactory.decodeStream(input, null, bounds) }
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > 1800) sampleSize *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = appContext.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(BitmapFactory.decodeStream(input, null, options)) { "Unable to decode selected photo" }
        }
        destination.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
        }
        bitmap.recycle()
        destination.absolutePath
    }.getOrNull()

    fun addTask(title: String, due: String, priority: String) {
        tasks += WorkTask(nextId(), title.trim(), due.trim(), priority)
        save()
    }

    fun toggleTask(id: Long) {
        val index = tasks.indexOfFirst { it.id == id }
        if (index >= 0) tasks[index] = tasks[index].copy(completed = !tasks[index].completed)
        save()
    }

    fun addInvoice(customer: String, description: String, amount: Double, due: String) {
        invoices.add(0, Invoice(nextId(), customer.trim(), description.trim(), amount, due.trim()))
        save()
    }

    fun toggleInvoicePaid(id: Long) {
        val index = invoices.indexOfFirst { it.id == id }
        if (index >= 0) invoices[index] = invoices[index].copy(paid = !invoices[index].paid)
        save()
    }

    fun addReference(title: String, body: String, tag: String) {
        references.add(0, ReferenceNote(nextId(), title.trim(), body.trim(), tag.trim()))
        save()
    }

    fun locationName(id: Long?): String = locations.firstOrNull { it.id == id }?.name ?: "General"

    private fun nextId(): Long {
        val largestStoredId = listOfNotNull(
            locations.maxOfOrNull { it.id },
            logs.maxOfOrNull { it.id },
            tasks.maxOfOrNull { it.id },
            invoices.maxOfOrNull { it.id },
            references.maxOfOrNull { it.id },
            chatMessages.maxOfOrNull { it.id }
        ).maxOrNull() ?: 0L
        return maxOf(System.currentTimeMillis(), largestStoredId + 1L)
    }

    private fun addStarterData() {
        val locationId = nextId()
        locations += Location(locationId, "Example customer", "Restaurant location", "Add the service address", "Landscaping + HVAC PM")
        tasks += WorkTask(nextId(), "Confirm next preventive-maintenance visit", "This week", "High")
        logs += ServiceLog(nextId(), locationId, "Example service log", "Record work completed, equipment condition, parts used, and follow-up items here.", System.currentTimeMillis())
        references += ReferenceNote(nextId(), "A20 belt replacement", "Record supplier, cost, customer price, tension notes, and compatible equipment here.", "Parts")
        save()
    }

    private fun save() {
        val root = JSONObject()
        root.put("locations", JSONArray().apply { locations.forEach { item -> put(JSONObject().apply {
            put("id", item.id); put("customer", item.customer); put("name", item.name)
            put("address", item.address); put("serviceType", item.serviceType)
        }) } })
        root.put("logs", JSONArray().apply { logs.forEach { item -> put(JSONObject().apply {
            put("id", item.id); put("locationId", item.locationId ?: JSONObject.NULL); put("title", item.title)
            put("details", item.details); put("createdAt", item.createdAt)
            put("photoPaths", JSONArray(item.photoPaths))
        }) } })
        root.put("tasks", JSONArray().apply { tasks.forEach { item -> put(JSONObject().apply {
            put("id", item.id); put("title", item.title); put("due", item.due)
            put("priority", item.priority); put("completed", item.completed)
        }) } })
        root.put("invoices", JSONArray().apply { invoices.forEach { item -> put(JSONObject().apply {
            put("id", item.id); put("customer", item.customer); put("description", item.description)
            put("amount", item.amount); put("due", item.due); put("paid", item.paid)
        }) } })
        root.put("references", JSONArray().apply { references.forEach { item -> put(JSONObject().apply {
            put("id", item.id); put("title", item.title); put("body", item.body); put("tag", item.tag)
        }) } })
        root.put("chatMessages", JSONArray().apply { chatMessages.forEach { item -> put(JSONObject().apply {
            put("id", item.id); put("role", item.role); put("text", item.text)
            put("locationId", item.locationId ?: JSONObject.NULL); put("createdAt", item.createdAt)
            put("photoPaths", JSONArray(item.photoPaths)); put("draftTitle", item.draftTitle)
            put("draftDetails", item.draftDetails); put("savedToHistory", item.savedToHistory)
        }) } })
        preferences.edit { putString("data", root.toString()) }
    }

    private fun load() {
        val raw = preferences.getString("data", null) ?: return
        runCatching {
            val root = JSONObject(raw)
            root.optJSONArray("locations")?.forEachObject { item -> locations += Location(
                item.getLong("id"), item.optString("customer"), item.optString("name"),
                item.optString("address"), item.optString("serviceType")
            ) }
            root.optJSONArray("logs")?.forEachObject { item -> logs += ServiceLog(
                item.getLong("id"), if (item.isNull("locationId")) null else item.getLong("locationId"),
                item.optString("title"), item.optString("details"), item.optLong("createdAt"),
                item.optJSONArray("photoPaths").toStringList()
            ) }
            root.optJSONArray("tasks")?.forEachObject { item -> tasks += WorkTask(
                item.getLong("id"), item.optString("title"), item.optString("due"),
                item.optString("priority"), item.optBoolean("completed")
            ) }
            root.optJSONArray("invoices")?.forEachObject { item -> invoices += Invoice(
                item.getLong("id"), item.optString("customer"), item.optString("description"),
                item.optDouble("amount"), item.optString("due"), item.optBoolean("paid")
            ) }
            root.optJSONArray("references")?.forEachObject { item -> references += ReferenceNote(
                item.getLong("id"), item.optString("title"), item.optString("body"), item.optString("tag")
            ) }
            root.optJSONArray("chatMessages")?.forEachObject { item -> chatMessages += ChatMessage(
                item.getLong("id"), item.optString("role"), item.optString("text"),
                if (item.isNull("locationId")) null else item.getLong("locationId"), item.optLong("createdAt"),
                item.optJSONArray("photoPaths").toStringList(), item.optString("draftTitle"),
                item.optString("draftDetails"), item.optBoolean("savedToHistory")
            ) }
        }
    }
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).map { index -> optString(index) }.filter { it.isNotBlank() }
}

private inline fun JSONArray.forEachObject(block: (JSONObject) -> Unit) {
    for (index in 0 until length()) block(getJSONObject(index))
}
