# MedMind – AI-Powered Smart Pill Dispenser & Health Ecosystem
---
<img src="media/media/image51.jpg" width="220">

MedMind is an IoT-enabled medication management system designed to help elderly patients take their medicines on time while keeping caregivers and doctors in the loop. It combines a smart pill-dispensing hardware unit with an Android application that provides reminders, AI-powered coaching, vitals monitoring, and real-time alerts.

---
## The Problem
---
Medication non-adherence is one of the biggest hidden challenges in elderly care. Many older adults forget doses, double-dose by accident, or skip medicines entirely. Family members living away often have no way to track if their loved ones are following their medication schedule. Most existing solutions like pill boxes or alarms are passive—they cannot verify whether a pill was actually taken.
---
## The MedMind Solution
---
MedMind creates a complete medication ecosystem:

1. **Smart Hardware Dispenser** that physically dispenses pills and verifies if the patient picked them up.
2. **Patient Mobile App** providing reminders, schedules, health insights, and automatic inventory tracking.
3. **Caregiver/Doctor Interface** for remote monitoring of adherence, vitals, and alerts.
4. **AI Medical Companion** for supportive, non-medical conversational guidance.

---

# Key Features

## Android App
- Reliable pill reminders with both local alarms & cloud-triggered alerts.
- AI Health Coach powered by Google Gemini for motivational suggestions.
- Syncs vitals like heart rate, steps, and oxygen from WearOS/Samsung watches through Health Connect.
- Emergency SOS that triggers a loud alarm on the caregiver’s phone.
- Automatic pill count tracking based on hardware dispensing.
- Multi-language support (English, Malayalam, Hindi).

## Smart Hardware (ESP8266)
- IR break-beam sensor checks if the pill was actually picked up.
- If not picked up within 15 seconds, the system flags a missed dose.
- Zig-zag servo motion prevents double dispensing or pill jams.
- NTP-based time synchronization for accurate offline dispensing.
- Speaker + display provide clear alerts at the device.

# Tech Stack

| Component | Technology | Purpose |
|----------|------------|---------|
| Mobile App | Kotlin (MVVM) | Patient/Caregiver/Doctor interfaces |
| Backend | Firebase (Auth, Firestore, RTDB) | User management, logs, vitals |
| Hardware | ESP8266 NodeMCU | Controls servos, sensors, alerts |
| Sensors | Servo motors, IR sensor | Dispensing + verification |
| AI Engine | Gemini API | AI medical companion |
| Health Data | Google Health Connect | Sync vitals from wearables |


# Getting Started

## 1. Prerequisites
Make sure you have:

- Android Studio (Koala or newer)
- Arduino IDE
- Firebase project with Auth, Firestore & Realtime DB enabled
- Google Health Connect installed on the Android device



## 2. Hardware Setup

1. Go to the `hardware-code/` folder.
2. Open the `.ino` file in Arduino IDE.
3. Install required libraries:  
   - Firebase ESP Client  
   - ArduinoJson  
   - NTPClient  
4. Update credentials at the top of the code:

```cpp
#define WIFI_SSID "YOUR_WIFI"
#define WIFI_PASSWORD "YOUR_PASSWORD"
#define FIREBASE_DB_URL "YOUR_RTDB_URL"
#define FIREBASE_DB_SECRET "YOUR_DATABASE_SECRET"
#define PATIENT_USER_ID "UID_FROM_APP"
````

5. Upload the code to the NodeMCU.

---

## 3. Android App Setup

1. Clone the repo:

```bash
git clone https://github.com/YOUR_USERNAME/MedMind.git
```

2. Open the `android-app/` folder in Android Studio.
3. Add your Firebase `google-services.json` to:

```
android-app/app/
```

4. Add your Gemini API Key inside `AiCompanionActivity.kt`.
5. Build and run on a physical device (Health Connect doesn’t work on standard emulators).

---

# Hardware Wiring

## MedMind Pill Dispenser – Hardware Wiring Reference

*(ESP8266 NodeMCU + Servos + IR Sensor + Buzzer + LCD I2C)*

### 1. ESP8266 NodeMCU Pins Used

| Component | NodeMCU Pin | GPIO |
| --------- | ----------- | ---- |
| Servo 1   | D5          | 14   |
| Servo 2   | D6          | 12   |
| IR Sensor | D7          | 13   |
| Buzzer    | D8          | 15   |
| LCD SDA   | D2          | 4    |
| LCD SCL   | D1          | 5    |

### 2. Servo Wiring (Both Servos)

| Servo Wire             | Connect To                  |
| ---------------------- | --------------------------- |
| Brown/Black (GND)      | NodeMCU GND                 |
| Red (VCC)              | External 5V                 |
| Orange/Yellow (Signal) | Servo 1 → D5 / Servo 2 → D6 |

⚠ **Important:** NodeMCU GND and external 5V GND must be connected together.

### 3. LCD 16×2 I2C

| LCD Pin | Connect To |
| ------- | ---------- |
| VCC     | 5V         |
| GND     | GND        |
| SDA     | D2         |
| SCL     | D1         |

### 4. IR Sensor

| IR Sensor Pin | Connect To |
| ------------- | ---------- |
| VCC           | 5V         |
| GND           | GND        |
| OUT           | D7         |

### 5. Speaker/Buzzer via LM386

| LM386 Pin | Connect To |
| --------- | ---------- |
| VCC       | 5V         |
| GND       | GND        |
| IN/SIG    | D8         |
| OUT       | Speaker    |

---

#Circuit Diagram

<img src="media/media/image50.jpg" width="700">

---

#Application Screenshots



---

## Login & Registration

MedMind ecosystem allows to login as patient/caregiver/doctor providing intelink between these users seamlessly for remote monitoring in the case of doctor or a caregiver.

<img src="media/media/image1.png" width="450">
New Registration window
<img src="media/media/image2.png" width="450">
<img src="media/media/image3.png" width="450">
<img src="media/media/image4.png" width="450">

### Multi-Lingual Screens

MedMind supports multi-languages as well. Here is a sneak peak of how every language implemented looks like.

<img src="media/media/image5.png" width="450">
<img src="media/media/image6.png" width="450">
<img src="media/media/image7.png" width="450">


### Patient, Caregiver, or Doctor UI sneakpeak

---

## Patient Interface

<img src="media/media/imagea.jpg" width="500">
<img src="media/media/imageb.jpg" width="500">

---

Both dotor and caregiver can add multiple patients for remote monitoring and can use the MedMind features to improve the adherence of the patients as well.

## Caregiver UI

<img src="media/media/image9.jpg" width="500">

---

## Doctor UI

<img src="media/media/image8.jpg" width="500">

---

## Emergency SOS

Emergency SOS allows to alarm the caregiver/doctor in case of emergency of patient.

<img src="media/media/image10.png" width="500">

The emergency alarm will be ring on the connected devices.

<img src="media/media/image11.png" width="500">
<img src="media/media/image12.jpg" width="600">

---

## Smartwatch Vitals Linking

Patients can link the smartwatch/smart health devices(since we are using health connect- health connect compatible devices can be used to link the vitals) with vital monitoring to link with the MedMind so that the caregiver/ doctor can remotely monitor the status.

<img src="media/media/image13.png" width="450">
<img src="media/media/image14.png" width="450">
<img src="media/media/image15.png" width="450">
<img src="media/media/image16.png" width="500">
<img src="media/media/imagex.png" width="600">
<img src="media/media/imagey.png" width="600">

<img src="media/media/image18.jpg" width="500">

---

## MedMind Ecosystem

Now Lets see how MedMind ecosystem works.Patients can link with doctor and caregiver by entering their ID

<img src="media/media/image19.png" width="400">

### Linking Patients in Caregivers/Doctors UI

At the top we have implemented the add ID to link with the patients and mulitple patients can be added and monitored. 

<img src="media/media/image20.png" width="450">


### Chat & Call Features

So Patients can chat with caregiver/ doctor also viseversa using the chat with caregiver/ chat with patient 

<img src="media/media/image21.png" width="450">

<img src="media/media/image20.png" width="450">

Chatting 

<img src="media/media/image22.png" width="450">
<img src="media/media/image23.jpg" width="500">

Users can request call with each other in case of emergency as well

<img src="media/media/imagep.png" width="450">

---

## AI Medical Companion

Its a companion that helps to reduce the loneliness as well help in medical related questions. We implemented using a custom Gemini API.

<img src="media/media/image26.png" width="600">
<img src="media/media/image27.png" width="450">
<img src="media/media/image28.png" width="450">

---

## Scheduling Pills

We have used two chambers which can dispense two different sized pills for the prototyping of the hardware.Patient or caregiver can schedule the pill dispensing from the app using the patient UI. Add Schedule button is used for scheduling.

<img src="media/media/image29.png" width="450">

<img src="media/media/image30.png" width="450">
<img src="media/media/image31.png" width="600">

<img src="media/media/image32.png" width="600">
<img src="media/media/image33.png" width="600">
<img src="media/media/image34.png" width="450">

<img src="media/media/image35.png" width="450">
<img src="media/media/image36.png" width="450">

### Notification 

After dispensing auditory alert via speaker as well as notification via smartphone take place to alarm the user to take the medicine.
<img src="media/media/image37.png" width="450">
<img src="media/media/image38.jpg" width="450">

---

## Missed Dose Detection

If the pill hasn't been taken on time then IR logic will log it as not taken and again the user is prompted to take the medicine. 

<img src="media/media/image39.png" width="600">
<img src="media/media/image40.png" width="600">

---

## Adherence History & AI Coach

AI coach motivates and give suggestions based on the adherence history and vitals to encourage pill intake on time. Adherence history shows the successfull intake of pill with logging at the bottom.

<img src="media/media/image41.png" width="400">
<img src="media/media/image42.png" width="500">
<img src="media/media/image43.png" width="450">

### Caregiver/doctor View

Caregiver/ doctor can access this adherence history of patient from their ui
<img src="media/media/image17.png" width="500">

---

## Pill Count Logic & Hardware

The pill count updated will be reducing automatically after every successfull intake of pills from the tray of hardware. If the pill count falls below 5 then the user is notified to refill the chamber.

MedMind side view
<img src="media/media/image44.png" width="500">

Pill Dispensing Mechanism
<img src="media/media/image45.jpg" width="500">
<img src="media/media/imageq.jpg" width="500">
<img src="media/media/image46.jpg" width="500">
<img src="media/media/image47.jpg" width="500">
<img src="media/media/image48.jpg" width="600">

Pill refill notification

<img src="media/media/image49.png" width="350">

---

# Team

* **Sreekanth S K** – Team Lead
* **Pranav J P**
*  **Sandra P S** 
* **Sooraj Subhash** 


---

# 📄 License

This project is licensed under the MIT License.

