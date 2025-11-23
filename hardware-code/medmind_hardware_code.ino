
#include <ESP8266WiFi.h>
#include <Firebase_ESP_Client.h>
#include <ArduinoJson.h>
#include <Servo.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <NTPClient.h> 
#include <WiFiUdp.h>    
#include "addons/TokenHelper.h"
#include "addons/RTDBHelper.h"
#define WIFI_SSID  "ssid here"
#define WIFI_PASSWORD  "password here"
#define FIREBASE_DB_URL "firebase url here"
#define FIREBASE_DB_SECRET "database secret code"
#define PATIENT_USER_ID "jPajGiGS8ZSkfudJnVylEY3whgz1"
Servo servo1, servo2;
const int servo1Pin = D5;
const int servo2Pin = D6;
const int buzzerPin = D8;
const int irSensorPin = D7;
LiquidCrystal_I2C lcd(0x27, 16, 2);
const String quotes[] = {"Stay positive!", "Keep going!", "You can do it!", "Never give up!"}; 
uint8_t quoteIndex = 0;
const int min_pulse = 500;
const int max_pulse = 2500;

FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;
bool firebaseReady = false;
String schedulePath = "/schedules/" + String(PATIENT_USER_ID);
String adherenceLogPath = "/adherence_logs";
const long UTC_OFFSET_IN_SECONDS = 19800; 
WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP, "pool.ntp.org", UTC_OFFSET_IN_SECONDS);
JsonDocument scheduleJson; 
unsigned long lastScheduleFetch = 0;
const long fetchInterval = 5 * 1000;
unsigned long lastTimeCheck = 0;
void displayPillTime() {
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("Pill Time Now");
}
void displayMotivationalQuote() {
  static unsigned long previousMillis = 0;
  if (millis() - previousMillis >= 5000) {
    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print(quotes[quoteIndex]);
    quoteIndex = (quoteIndex + 1) % 4;
    previousMillis = millis();
  }
}
void activateBuzzer() {
  const int tonesArr[] = {1000, 1200, 800};
  for (int i = 0; i < 3; i++) {
    tone(buzzerPin, tonesArr[i]);
    delay(500);
  }
  noTone(buzzerPin);
}


bool dispensePills(int chamber, int rotations, String pillName) {
  Servo &servo = (chamber == 1) ? servo1 : servo2;
  for (int i = 0; i < rotations; i++) {
    for (int angle = 0; angle <= 270; angle += 10) {
      if (angle <= 180) {
        servo.write(angle);
        delay(50);
        servo.write(angle - 10);
        delay(50);
      } else {
        int zigzagAngle = 270 - angle;
        servo.write(zigzagAngle);
        delay(50);
        servo.write(zigzagAngle + 30);
        delay(50);
      }
    }
    servo.write(0);
    delay(500);
  }

  Serial.printf("Dispensed %s from Chamber %d\n", pillName.c_str(), chamber);
  activateBuzzer();
  displayPillTime(); 

  bool pillTaken = false;
  unsigned long startTime = millis();
  Serial.println("Log: Monitoring IR sensor for 15 seconds.");

  while (millis() - startTime < 15000) {
    if (digitalRead(irSensorPin) == LOW) { 
      pillTaken = true;
      Serial.println("Object Detected!");
      break; 
    }
    delay(100);
  }
  
  Serial.println(pillTaken ? "Pill Taken (recorded)" : "Pill Not Taken (recorded)");
  lcd.clear();
  return pillTaken;
}


void writeAdherenceLog(String pillName, int servo, int pills, bool taken) {
  if (!firebaseReady) return;
  FirebaseJson json;
  json.set("patientId", PATIENT_USER_ID);
  json.set("pillName", pillName); 
  json.set("servo", servo);
  json.set("pills", pills);
  json.set("taken", taken);
  json.set("timestamp/.sv", "timestamp");
  Serial.println("Writing adherence log to Database");
  if (Firebase.RTDB.pushJSON(&fbdo, adherenceLogPath.c_str(), &json)) {
    Serial.println("Log write success");
  } else {
    Serial.println("logging: Log write failed: " + fbdo.errorReason());
  }
}


void fetchSchedules() {
  if (!firebaseReady) return;
  Serial.println("Fetching schedules from RTDB...");
  if (Firebase.RTDB.getJSON(&fbdo, schedulePath.c_str())) {
    deserializeJson(scheduleJson, fbdo.payload());
    if (scheduleJson.isNull()) {
        Serial.println("No schedules found or parse failed.");
        scheduleJson.clear(); 
    } else {
        Serial.println("Schedules parsed and stored.");
    }
  } else {
    Serial.println("loging: Failed to fetch schedules: " + fbdo.errorReason());
  }
  lastScheduleFetch = millis();
}

int getDayOfWeek(long timestamp) {
  time_t raw_time = (time_t)timestamp;
  struct tm * ti;
  ti = localtime (&raw_time);
  return ti->tm_wday; 
}
String getDayString(int day) {
  switch (day) {
    case 0: return "SUN"; case 1: return "MON"; case 2: return "TUE";
    case 3: return "WED"; case 4: return "THU"; case 5: return "FRI";
    case 6: return "SAT"; default: return "";
  }
}


void checkSchedules() {
  if (scheduleJson.isNull() || !timeClient.isTimeSet()) {
    return; 
  }
  
  timeClient.update();
  unsigned long now = timeClient.getEpochTime();
  int currentDay = timeClient.getDay();
  int currentHour = timeClient.getHours();
  int currentMinute = timeClient.getMinutes();
  String currentDayStr = getDayString(currentDay);

  JsonObject root = scheduleJson.as<JsonObject>();
  for (JsonPair schedulePair : root) {
    JsonObject schedule = schedulePair.value().as<JsonObject>();
    if (schedule["type"] == "NOW") {
      Serial.println("Found 'Dispense Now' command.");
      int servo = schedule["servo"];
      int pills = schedule["pills"];
      String pillName = schedule["pillName"];
      bool taken = dispensePills(servo, pills, pillName);
      writeAdherenceLog(pillName, servo, pills, taken);
      String nodeToDelete = schedulePath + "/" + schedulePair.key().c_str();
      Firebase.RTDB.deleteNode(&fbdo, nodeToDelete.c_str());
      continue; 
    }
    long startDate = schedule["startDate"];
    long endDate = schedule["endDate"] | 0; 
    long lastDispense = schedule["lastDispenseTimestamp"] | 0;
    long nowSec = now; 
    long startSec = startDate / 1000;
    long endSec = endDate / 1000;
    if (nowSec < startSec) continue; 
    if (endDate != 0 && nowSec > endSec) continue;
    JsonArray days = schedule["daysOfWeek"].as<JsonArray>();
    bool dayMatch = false;
    for (JsonVariant day : days) {
      if (day.as<String>() == currentDayStr) {
        dayMatch = true;
        break;
      }
    }
    if (!dayMatch) continue;
    time_t raw_now = (time_t)now;
    struct tm * now_utc = gmtime(&raw_now);
    int dayNow = now_utc->tm_yday;
    time_t raw_last = (time_t)(lastDispense / 1000); 
    struct tm * last_utc = gmtime(&raw_last);
    int dayLast = last_utc->tm_yday;
    JsonArray times = schedule["times"].as<JsonArray>();
    for (JsonVariant t : times) {
      int schedHour = t["hour"];
      int schedMin = t["minute"];
      
      if (schedHour == currentHour && schedMin == currentMinute) {
        if (dayNow != dayLast) { 
          Serial.printf("Time match! Dispensing schedule: %s\n", schedule["pillName"].as<String>().c_str());
          int servo = schedule["servo"];
          int pills = schedule["pills"];
          String pillName = schedule["pillName"];
          bool taken = dispensePills(servo, pills, pillName);
          writeAdherenceLog(pillName, servo, pills, taken);
          String nodeToUpdate = schedulePath + "/" + schedulePair.key().c_str() + "/lastDispenseTimestamp";
          Firebase.RTDB.setDouble(&fbdo, nodeToUpdate.c_str(), (double)now * 1000.0);
          break; 
        }
      }
    }
  }
}


void setup() {
  Serial.begin(115200);
  lcd.init();
  lcd.backlight();
  servo1.attach(servo1Pin, min_pulse, max_pulse);
  servo2.attach(servo2Pin, min_pulse, max_pulse);
  pinMode(buzzerPin, OUTPUT);
  digitalWrite(buzzerPin, LOW);
  pinMode(irSensorPin, INPUT);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("WiFi Connection");
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(1000);
  }
  Serial.println("\nConnected");
  timeClient.begin();
  Serial.println("Syncing time.");
  while(!timeClient.update()) {
    timeClient.forceUpdate();
    delay(500);
  }
  Serial.println("Time synchronized!");
  config.database_url = FIREBASE_DB_URL;
  config.signer.tokens.legacy_token = FIREBASE_DB_SECRET;
  config.time_zone = 5.5; 
  Firebase.reconnectWiFi(true);
  fbdo.setResponseSize(4096);
  fbdo.setBSSLBufferSize(4096, 1024); 
  Firebase.begin(&config, &auth);
  firebaseReady = true;
  fetchSchedules(); 
}


void loop() {
  if (firebaseReady) {
    if (millis() - lastTimeCheck >= 5000) { 
      lastTimeCheck = millis();
      Serial.printf("Checking schedules... Time: %s\n", timeClient.getFormattedTime());
      checkSchedules();
    }
    if (millis() - lastScheduleFetch >= 2000) {
      fetchSchedules();
    }
  } else {
    Serial.println("Firebase not ready.");
    delay(5000);
  }
  
  displayMotivationalQuote();
  delay(10);
}