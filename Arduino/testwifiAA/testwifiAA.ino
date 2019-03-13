#include <chrono>
#include <map>
#include <sstream>

// ...

using namespace std::chrono;

#include <ESP8266WiFi.h>
#include <FirebaseArduino.h>

#define SSID_NAME "Mimi"
#define PASSWORD "12345678"
#define FIREBASE_HOST "doanlthtvdk.firebaseio.com"
#define FIREBASE_AUTH "IwortuCU3Y2tt0IUeWUvsWW2Ddswl8ZkNaMemP96"
#define CAM_BIEN_RUNG D5
#define CAM_BIEN_QUANG D6
#define LED D7
#define LOA D8
#define DINH_MUC 700
const String& path_firebase = "/histories";
void setup() {
  Serial.begin(115200);
  pinMode(LED, OUTPUT);
  digitalWrite(LED, LOW);

  Serial.println("to connect");
  WiFi.begin(SSID_NAME, PASSWORD);

  Serial.println("connecting");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("ket noi thanh cong");
  Serial.println("ip : ");
  Serial.print(WiFi.localIP());
  Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
}

void check(int valuequang, int valuerung) {
  if (valuequang < DINH_MUC) {
    digitalWrite(LED, HIGH);
    tone(LOA, 8000);
    sendfirebase();
  } else {
    if (valuerung > DINH_MUC) {
      digitalWrite(LED, HIGH);
      tone(LOA, 5000);
    } else {
      digitalWrite(LED, LOW);
      noTone(LOA);
    }
  }
}
void sendfirebase() {
  Firebase.setBool(path_firebase + "/" + Firebase.pushString(path_firebase, "") + "/verified", false);
}
void loop() {
  int value_rung = analogRead(CAM_BIEN_RUNG);
  int value_quang = analogRead(CAM_BIEN_QUANG);
  bool onOff = Firebase.getBool("on_off");
  Serial.print("CAM_BIEN_RUNG: ");
  Serial.println(value_rung);
  Serial.print("CAM_BIEN_QUANG: ");
  Serial.println(value_quang);
  Serial.print("on_off: ");
  Serial.println(onOff);
  if(onOff){
    check(value_quang, value_rung);
    delay(1000);
  }
delay(1000);
}
//       doan code tao jsonobject de push len firebse
//    StaticJsonBuffer<10000> jsonBuffer;
//    // create an object
//    JsonObject& object1 = jsonBuffer.createObject();
//    object1["verified"] = false;
//    const JsonVariant& variant = object1;
