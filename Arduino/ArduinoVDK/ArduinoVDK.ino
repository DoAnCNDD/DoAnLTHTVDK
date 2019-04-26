#include <ESP8266WiFi.h>
#include "WiFiManager.h"
#include "pitches.h"
#include <ESP8266HTTPClient.h>

#define CAM_BIEN_RUNG         D8
#define LED                   D5
#define LOA                   D1
#define TRIG                  D7
#define ECHO                  D6
#define DINH_MUC_RUNG         700
#define DINH_MUC_KHOANG_CACH  5
#define SSID                  "DoAnVDK_PHH"
#define PASSWORD              "12345678"

const String& BASE_URL = "http://node-auth-081098.herokuapp.com/do_an_ltht_vdk";

// notes in the melody:
int melody[] = {
  NOTE_C4, NOTE_G3, NOTE_G3, NOTE_A3, NOTE_G3, 0, NOTE_B3, NOTE_C4
};

// note durations: 4 = quarter note, 8 = eighth note, etc.:
int noteDurations[] = {
  4, 8, 8, 4, 4, 4, 4, 4
};

void configModeCallback (WiFiManager *myWiFiManager) {
  Serial.println("Entered config mode");
  Serial.println(WiFi.softAPIP());
  Serial.println(myWiFiManager->getConfigPortalSSID());
  Serial.println("------------------------------------");
}

void play() {
  Serial.println("[PLAY]");
  // iterate over the notes of the melody:
  for (int thisNote = 0; thisNote < 8; thisNote++) {

    // to calculate the note duration, take one second divided by the note type.
    //e.g. quarter note = 1000 / 4, eighth note = 1000/8, etc.
    int noteDuration = 1000 / noteDurations[thisNote];
    tone(LOA, melody[thisNote], noteDuration);

    // to distinguish the notes, set a minimum time between them.
    // the note's duration + 30% seems to work well:
    int pauseBetweenNotes = noteDuration * 1.30;
    delay(pauseBetweenNotes);
    // stop the tone playing:
    noTone(LOA);
  }
  Serial.println("[END_PLAY]");
}

void setup() {
  Serial.begin(115200);
  pinMode(LED, OUTPUT);
  pinMode(ECHO, INPUT);
  pinMode(TRIG, OUTPUT);
  digitalWrite(LED, LOW);

  WiFiManager wifiManager;
//  wifiManager.resetSettings();
  wifiManager.setAPCallback(configModeCallback);

  if (!wifiManager.autoConnect(SSID, PASSWORD)) {
    Serial.println("[ERROR] failed to connect and hit timeout");
    ESP.reset();
    delay(1000);
  }

  Serial.println("[WIFI_CONNECTED]");
}

void check(int khoang_cach, int value_rung) {
  if (khoang_cach > DINH_MUC_KHOANG_CACH) {
    play();
    postHTTP();
  } else if (value_rung > DINH_MUC_RUNG) {
    play();
  }
}
int dokhoangcach() {
  unsigned long duration; // biến đo thời gian
  int distance;           // biến lưu khoảng cách

  /* Phát xung từ chân trig */
  digitalWrite(TRIG, 0);  // tắt chân trig
  delayMicroseconds(2);
  digitalWrite(TRIG, 1);  // phát xung từ chân trig
  delayMicroseconds(5);   // xung có độ dài 5 microSeconds
  digitalWrite(TRIG, 0);  // tắt chân trig

  /* Tính toán thời gian */
  // Đo độ rộng xung HIGH ở chân echo.
  duration = pulseIn(ECHO, HIGH);
  // Tính khoảng cách đến vật.
  distance = int(duration / 2 / 29.412);
  return distance;
}
void postHTTP() {
  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    http.setTimeout(10000);
    http.begin(BASE_URL + "/notification");

    int httpCode = http.POST("");
    if (httpCode > 0) {
      String payload = http.getString();
      Serial.print("[POST_NOTIFICATION_SUCCESS] result = ");
      Serial.println(payload);
    } else {
      Serial.print("[POST_NOTIFICATION_ERROR] httpCode = ");
      Serial.println(httpCode);
    }
    http.end();
  } else {
    Serial.println("[POST_NOTIFICATION_ERROR] no connected wifi");
  }
}

int getHTTP() {
  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    http.setTimeout(10000);
    http.begin(BASE_URL + "/on_off");

    int httpCode = http.GET();
    if (httpCode > 0) {
      String payload = http.getString();
      Serial.print("[GET_on_off_SUCCESS] = ");
      Serial.println(payload);
      return payload.toInt();
    } else {
      Serial.print("[GET_on_off_ERROR] httpCode = ");
      Serial.println(httpCode);
      return -1;
    }
    http.end();
  } else {
    Serial.print("[GET_on_off_ERROR] no connected wifi");
    return -1;
  }
}

void loop() {
  int on_off = getHTTP();
  Serial.print("[on_off]=");
  Serial.println(on_off);
  if (on_off == 1) {
    digitalWrite(LED, HIGH);

    int khoang_cach = dokhoangcach();
    int rung = analogRead(CAM_BIEN_RUNG);

    Serial.print("[khoang_cach]=");
    Serial.println(khoang_cach);
    Serial.print("[rung]=");
    Serial.println(rung);

    check(khoang_cach, rung);
  } else if (on_off == 0) {
    noTone(LOA);
    digitalWrite(LED, LOW);
  }

  delay(2000);
}
