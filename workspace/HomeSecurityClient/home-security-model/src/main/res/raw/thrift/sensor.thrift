namespace java com.example.lisi4ka.homesecuritymodel

struct SensorThrift {
  1: string name;
  2: bool state;
}

struct SensorListThrift {
    1: list <SensorThrift> sensors;
}