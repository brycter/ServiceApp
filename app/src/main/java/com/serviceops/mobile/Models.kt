package com.serviceops.mobile

data class Location(
    val id: Long,
    val customer: String,
    val name: String,
    val address: String,
    val serviceType: String
)

data class ServiceLog(
    val id: Long,
    val locationId: Long?,
    val title: String,
    val details: String,
    val createdAt: Long
)

data class WorkTask(
    val id: Long,
    val title: String,
    val due: String,
    val priority: String,
    val completed: Boolean = false
)

data class Invoice(
    val id: Long,
    val customer: String,
    val description: String,
    val amount: Double,
    val due: String,
    val paid: Boolean = false
)

data class ReferenceNote(
    val id: Long,
    val title: String,
    val body: String,
    val tag: String
)
