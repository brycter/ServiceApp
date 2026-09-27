package com.serviceops.mobile

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject

class ServiceOpsStore(context: Context) {
    private val preferences = context.getSharedPreferences("service_ops_data", Context.MODE_PRIVATE)

    val locations = mutableStateListOf<Location>()
    val logs = mutableStateListOf<ServiceLog>()
    val tasks = mutableStateListOf<WorkTask>()
    val invoices = mutableStateListOf<Invoice>()
    val references = mutableStateListOf<ReferenceNote>()

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

    fun addLog(locationId: Long?, title: String, details: String) {
        logs.add(0, ServiceLog(nextId(), locationId, title.trim(), details.trim(), System.currentTimeMillis()))
        save()
    }

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

    private fun nextId(): Long = System.currentTimeMillis() + (0..999).random()

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
        preferences.edit().putString("data", root.toString()).apply()
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
                item.optString("title"), item.optString("details"), item.optLong("createdAt")
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
        }
    }
}

private inline fun JSONArray.forEachObject(block: (JSONObject) -> Unit) {
    for (index in 0 until length()) block(getJSONObject(index))
}
