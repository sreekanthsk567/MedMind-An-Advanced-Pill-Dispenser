package com.example.myapplication
data class AdherenceLog(
    val patientId: String = "",
    val pillName: String? = "Pill",
    val servo: Int = 0,
    val pills: Int = 0,
    val taken: Boolean = false,
    val timestamp: Long = 0L
) {
    constructor() : this("", "Pill", 0, 0, false, 0L)
}