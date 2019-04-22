#include <ESP8266WiFi.h>
#include <DNSServer.h>
#include <ESP8266WebServer.h>
#include "WiFiManager.h"
#include <FirebaseArduino.h>
#include"pitches.h"

#define FIREBASE_HOST "doanlthtvdk.firebaseio.com"
#define FIREBASE_AUTH "IwortuCU3Y2tt0IUeWUvsWW2Ddswl8ZkNaMemP96"
#define CAM_BIEN_RUNG D5
#define CAM_BIEN_QUANG D6
#define LED D7
#define LOA D8
#define DINH_MUC_QUANG 700
#define DINH_MUC_RUNG 700

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
  digitalWrite(LED, LOW);

  WiFiManager wifiManager;
  wifiManager.setAPCallback(configModeCallback);
  
  if(!wifiManager.autoConnect()) {
    Serial.println("failed to connect and hit timeout");
    ESP.reset();
    delay(1000);
  }
  
  Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
  Serial.println("[connected]");
}

void check(int value_quang, int value_rung) {
  if (value_quang > DINH_MUC_QUANG) {

    Serial.print("[on_off_send]=");
    String on_off_send = Firebase.getString("on_off_send");
    Serial.println(on_off_send);
    if (on_off_send == "1") {
      send_firebase();
    }

    play();
  } else if (value_rung > DINH_MUC_RUNG) {
    play();
  }
}

void send_firebase() {
  StaticJsonBuffer<1000> jsonBuffer;
  JsonObject& object1 = jsonBuffer.createObject();
  object1["verified"] = false;
  const JsonVariant& variant = object1;
  Firebase.push(path_firebase, variant);
  
  delay(300);
  Serial.println("[send_firebase_done]");
}


void loop() {
  Serial.print("[on_off]=");
  String on_off = Firebase.getString("on_off");
  Serial.println(on_off);

  if (on_off == "1") {
    digitalWrite(LED, HIGH);

    int quang = analogRead(CAM_BIEN_QUANG);
    int rung = analogRead(CAM_BIEN_RUNG);
    
    Serial.print("[quang]=");
    Serial.println(quang);
    Serial.print("[rung]=");
    Serial.println(rung);

    check(quang, rung);
  } else if (on_off == "0") {
    noTone(LOA);
    digitalWrite(LED, LOW);
  }

  delay(2000);
}
