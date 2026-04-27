package com.uta.iot_backend.sensor.repository;

import com.uta.iot_backend.sensor.model.SensorReading;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SensorRepository extends MongoRepository<SensorReading, String> {

    List<SensorReading> findByDeviceId(String deviceId);

    List<SensorReading> findByType(String type);

    List<SensorReading> findBySensorId(String sensorId);

    List<SensorReading> findByDeviceIdAndTimestampBetween(String deviceId, Instant from, Instant to);


}
    