package com.example.myapplication
import com.google.firebase.firestore.PropertyName
data class User(
    var uid: String = "",
    var email: String = "",
    @get:PropertyName("fullName") @set:PropertyName("fullName") var fullName: String = "",
    var role: String = "",
    @get:PropertyName("pillsChamber1") @set:PropertyName("pillsChamber1") var pillsChamber1: Int = 0,
    @get:PropertyName("pillsChamber2") @set:PropertyName("pillsChamber2") var pillsChamber2: Int = 0,
    @get:PropertyName("patientIds") @set:PropertyName("patientIds") var patientIds: List<String> = emptyList(),
    var fcmToken: String = "",
    var caregiverId: String = "",
    @get:PropertyName("isEmergencyActive") @set:PropertyName("isEmergencyActive")
    var isEmergencyActive: Boolean = false
) {
    constructor() : this("", "", "", "", 0, 0, emptyList(), "", "", false)
}