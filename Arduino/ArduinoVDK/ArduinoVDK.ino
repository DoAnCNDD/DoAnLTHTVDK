#include <ESP8266WiFi.h>
#include "WiFiManager.h"
#include "pitches.h"
#include <ESP8266HTTPClient.h>

#define CAM_BIEN_RUNG D5
#define LED D4
#define LOA D3
#define TRIG D7
#define ECHO D6
#define DINH_MUC_RUNG 700
#define DINH_MUC_KHOANG_CACH 10

const String& BASE_URL = "http://node-auth-081098.herokuapp.com/do_an_ltht_vdk";
const String& path_firebase = "/histories";

// notes in the song 'Mukkathe Penne'
static const int melody[] = {
  NOTE_D4, NOTE_G4, NOTE_FS4, NOTE_A4,
  NOTE_G4, NOTE_C5, NOTE_AS4, NOTE_A4,
  NOTE_FS4, NOTE_G4, NOTE_A4, NOTE_FS4, NOTE_DS4, NOTE_D4,
  NOTE_C4, NOTE_D4, 0,

  NOTE_D4, NOTE_G4, NOTE_FS4, NOTE_A4,
  NOTE_G4, NOTE_C5, NOTE_D5, NOTE_C5, NOTE_AS4, NOTE_C5, NOTE_AS4, NOTE_A4,      //29               //8
  NOTE_FS4, NOTE_G4, NOTE_A4, NOTE_FS4, NOTE_DS4, NOTE_D4,
  NOTE_C4, NOTE_D4, 0,

  NOTE_D4, NOTE_FS4, NOTE_G4, NOTE_A4, NOTE_DS5, NOTE_D5,
  NOTE_C5, NOTE_AS4, NOTE_A4, NOTE_C5,
  NOTE_C4, NOTE_D4, NOTE_DS4, NOTE_FS4, NOTE_D5, NOTE_C5,
  NOTE_AS4, NOTE_A4, NOTE_C5, NOTE_AS4,             //58

  NOTE_D4, NOTE_FS4, NOTE_G4, NOTE_A4, NOTE_DS5, NOTE_D5,
  NOTE_C5, NOTE_D5, NOTE_C5, NOTE_AS4, NOTE_C5, NOTE_AS4, NOTE_A4, NOTE_C5, NOTE_G4,
  NOTE_A4, 0, NOTE_AS4, NOTE_A4, 0, NOTE_G4,
  NOTE_G4, NOTE_A4, NOTE_G4, NOTE_FS4, 0,

  NOTE_C4, NOTE_D4, NOTE_G4, NOTE_FS4, NOTE_DS4,
  NOTE_C4, NOTE_D4, 0,
  NOTE_C4, NOTE_D4, NOTE_G4, NOTE_FS4, NOTE_DS4,
  NOTE_C4, NOTE_D4, END

};

// note durations: 8 = quarter note, 4 = 8th note, etc.
static const int noteDurations[] = {       //duration of the notes
  8, 4, 8, 4,
  4, 4, 4, 12,
  4, 4, 4, 4, 4, 4,
  4, 16, 4,

  8, 4, 8, 4,
  4, 2, 1, 1, 2, 1, 1, 12,
  4, 4, 4, 4, 4, 4,
  4, 16, 4,

  4, 4, 4, 4, 4, 4,
  4, 4, 4, 12,
  4, 4, 4, 4, 4, 4,
  4, 4, 4, 12,

  4, 4, 4, 4, 4, 4,
  2, 1, 1, 2, 1, 1, 4, 8, 4,
  2, 6, 4, 2, 6, 4,
  2, 1, 1, 16, 4,

  4, 8, 4, 4, 4,
  4, 16, 4,
  4, 8, 4, 4, 4,
  4, 20,
};

void configModeCallback (WiFiManager *myWiFiManager) {
  Serial.println("Entered config mode");
  Serial.println(WiFi.softAPIP());
  Serial.println(myWiFiManager->getConfigPortalSSID());
  Serial.println("------------------------------------");
}

void play() {
  Serial.println("[play]");
  int speed = 20; //higher value, slower notes
  for (int thisNote = 0; melody[thisNote] != -1; thisNote++) {
    int noteDuration = speed * noteDurations[thisNote];
    tone(LOA, melody[thisNote], noteDuration * .95);
  }
  Serial.println("[end_play]");
}

void setup() {
  Serial.begin(115200);
  pinMode(LED, OUTPUT);
  pinMode(ECHO, INPUT);
  pinMode(TRIG, OUTPUT);
  digitalWrite(LED, LOW);

  WiFiManager wifiManager;
  //wifiManager.resetSettings();
  wifiManager.setAPCallback(configModeCallback);

  if (!wifiManager.autoConnect()) {
    Serial.println("[ERROR] failed to connect and hit timeout");
    ESP.reset();
    delay(1000);
  }

  Serial.println("[WIFI_CONNECTED]");
}

void check(int khoang_cach, int value_rung) {
  if (khoang_cach > DINH_MUC_KHOANG_CACH) {
    int on_off_send = getHTTP("/on_off_send");
    if (on_off_send == 1) {
      postHTTP();
    }
    play();
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

int getHTTP(String path) {
  if (WiFi.status() == WL_CONNECTED) {
    HTTPClient http;
    http.setTimeout(10000);
    http.begin(BASE_URL + path);

    int httpCode = http.GET();
    if (httpCode > 0) {
      String payload = http.getString();
      Serial.print("[GET_");
      Serial.print(path);
      Serial.print("_SUCCESS] = ");
      Serial.println(payload);
      return payload.toInt();
    } else {
      Serial.print("[GET_");
      Serial.print(path);
      Serial.print("_ERROR] httpCode = ");
      Serial.println(httpCode);
      return -1;
    }
    http.end();
  } else {
    Serial.print("[GET_");
    Serial.print(path);
    Serial.println("_ERROR] no connected wifi");
    return -1;
  }
}

void loop() {
  int on_off = getHTTP("/on_off");
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
