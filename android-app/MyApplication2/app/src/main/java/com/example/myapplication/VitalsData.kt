package com.example.myapplication
data class VitalsData(
    val lastUpdated: Long = System.currentTimeMillis(),
    val heartRate: Long? = null,
    val oxygenSaturation: Double? = null,
    val steps: Long? = null
) {
    constructor() : this(0L, null, null, null)
}