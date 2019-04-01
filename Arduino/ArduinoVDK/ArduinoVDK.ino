


#include <ESP8266WiFi.h>
#include <WiFiManager.h>
#include <FirebaseArduino.h>

#define SSID_NAME "thacmoVDKteam"
#define PASSWORD "12345678"
#define FIREBASE_HOST "doanlthtvdk.firebaseio.com"
#define FIREBASE_AUTH "IwortuCU3Y2tt0IUeWUvsWW2Ddswl8ZkNaMemP96"
#define CAM_BIEN_RUNG D5
#define CAM_BIEN_QUANG D6
#define LED D7
#define LOA D8
#define DINH_MUC 700
const String& path_firebase = "/histories";

void configModeCallback (WiFiManager *myWiFiManager)
{
  Serial.println("Entered config mode");
  Serial.println(WiFi.softAPIP());
  Serial.println(myWiFiManager->getConfigPortalSSID());
}

void ketnoiwifi() {
  WiFiManager wifiManager;
  wifiManager.setAPCallback(configModeCallback);
  if (!wifiManager.autoConnect(SSID_NAME, PASSWORD))
  {
    Serial.println("khong the ket noi");
    ESP.reset();
    delay(1000);
  }
  Serial.println("connected...good");
}

void setup() {
  Serial.begin(115200);
  pinMode(LED, OUTPUT);
  digitalWrite(LED, LOW);
  ketnoiwifi();
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
      tone(LOA, 5000, 20000); //20s
    }
  }
}
void sendfirebase() {
  Firebase.setBool(path_firebase + "/" + Firebase.pushString(path_firebase, "") + "/verified", false);
  if (Firebase.failed()) {
    Serial.print("ko gui duoc du lieu");
    Serial.println(Firebase.error());
    return;
  }
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
  if (onOff) {
    check(value_quang, value_rung);
  } else {
    noTone(LOA);
    digitalWrite(LED, LOW);
  }
  delay(1000);
}
//       doan code tao jsonobject de push len firebse
//    StaticJsonBuffer<10000> jsonBuffer;
//    // create an object
//    JsonObject& object1 = jsonBuffer.createObject();
//    object1["verified"] = false;
//    const JsonVariant& variant = object1;

//  Serial.println("to connect");
//  WiFi.begin(SSID_NAME, PASSWORD);
//
//  Serial.println("connecting");
//  while (WiFi.status() != WL_CONNECTED) {
//    delay(500);
//    Serial.print(".");
//  }
//
//  Serial.println("ket noi thanh cong");
//  Serial.println("ip : ");
//  Serial.print(WiFi.localIP());

//#include <chrono>
//#include <map>
//#include <sstream>
//
//// ...
//
//using namespace std::chrono;
