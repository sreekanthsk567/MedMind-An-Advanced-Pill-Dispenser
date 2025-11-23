package com.example.myapplication
data class Schedule(
    val id: String = "",
    val userId: String = "",
    val pillName: String = "Pill",
    val servo: Int = 1,
    val pills: Int = 1,
    val type: String = "DAILY",
    val daysOfWeek: List<String> = emptyList(),

    val startDate: Long = 0L,
    val endDate: Long? = null,
    val times: List<SimpleTime> = emptyList(),
    val lastDispenseTimestamp: Long = 0L
) {
    constructor() : this("", "", "Pill", 1, 1, "DAILY", emptyList(), 0L, null, emptyList(), 0L)
}
data class SimpleTime(
    val hour: Int = 0,
    val minute: Int = 0
) {
    constructor() : this(0, 0)
}