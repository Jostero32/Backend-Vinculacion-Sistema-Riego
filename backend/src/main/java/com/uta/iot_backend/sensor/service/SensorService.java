package com.uta.iot_backend.sensor.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.uta.iot_backend.sensor.model.SensorMqttMessage;
import com.uta.iot_backend.sensor.model.SensorReading;
import com.uta.iot_backend.sensor.repository.SensorRepository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SensorService {

    private static final Logger log = LoggerFactory.getLogger(SensorService.class);

    private final SensorRepository sensorRepository;

    private final ObjectMapper objectMapper;

    public void processMessage(String json) {
        try {
            SensorMqttMessage message = objectMapper.readValue(json, SensorMqttMessage.class);

            List<SensorReading> readings = message.payload().stream()
                    .map(sensor -> SensorReading.builder()
                            .deviceId(message.metadata().deviceId())
                            .sensorId(sensor.sensorId())
                            .type(sensor.type())
                            .value(sensor.value())
                            .unit(sensor.unit())
                            .timestamp(message.metadata().timestamp())
                            .build())
                    .toList();

            sensorRepository.saveAll(readings);
            log.info("Guardadas {} lecturas del dispositivo {}", readings.size(), message.metadata().deviceId());

        } catch (JsonProcessingException e) {
            log.error("Error parseando JSON: {}", e.getMessage());
        }
    }
}