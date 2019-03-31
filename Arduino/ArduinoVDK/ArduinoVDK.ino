#include <ESP8266WiFi.h>
#include <FirebaseArduino.h>

#define SSID_NAME "Hanh Nguyen"
#define PASSWORD "12345679"
#define FIREBASE_HOST "doanlthtvdk.firebaseio.com"
#define FIREBASE_AUTH "IwortuCU3Y2tt0IUeWUvsWW2Ddswl8ZkNaMemP96"
#define CAM_BIEN_RUNG D5
#define CAM_BIEN_QUANG D6
#define LED D7
#define LOA D8
#define DINH_MUC_QUANG 700
#define DINH_MUC_RUNG 700

const String& path_firebase = "/histories";

#include"pitches.h"

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

void play() {
  Serial.println("[PLAY]");
  int speed = 10; //higher value, slower notes
  for (int thisNote = 0; melody[thisNote] != -1; thisNote++) {
    int noteDuration = speed * noteDurations[thisNote];
    tone(LOA, melody[thisNote], noteDuration * .95);
  }
  Serial.println("[END_PLAY]");
}

void setup() {
  Serial.begin(115200);
  pinMode(LED, OUTPUT);
  digitalWrite(LED, LOW);

  WiFi.begin(SSID_NAME, PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print('.');
  }

  Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
}

void check(int value_quang, int value_rung) {
  if (value_quang > DINH_MUC_QUANG) {
    
    Serial.print("on_off_send=");
    int on_off_send = Firebase.getString("on_off_send").toInt();
    Serial.println(on_off_send);
    if (on_off_send == 1) {
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
  delay(200);
}


void loop() {
  Serial.print("on_off: ");
  int on_off = Firebase.getString("on_off").toInt();
  Serial.println(on_off);

  if (on_off == 1) {
    digitalWrite(LED, HIGH);

    int quang = analogRead(CAM_BIEN_QUANG);
    int rung = analogRead(CAM_BIEN_RUNG);
    Serial.print("quang=");
    Serial.println(quang);
    Serial.print("rung=");
    Serial.println(rung);

    check(quang, rung);
  } else {
    noTone(LOA);
    digitalWrite(LED, LOW);
  }

  delay(1700);
}
