package com.uta.iot_backend.sensor.controllers;

import com.uta.iot_backend.sensor.dto.ApiResponse;
import com.uta.iot_backend.sensor.dto.SensorInfoResponse;
import com.uta.iot_backend.sensor.dto.SensorReadingResponse;
import com.uta.iot_backend.sensor.dto.SensorStatsResponse;
import com.uta.iot_backend.sensor.service.SensorService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * REST Controller para consulta de lecturas de sensores.
 * Base path: /api/v1
 *
 * Endpoints diseñados para alimentar un dashboard frontend con:
 * - Estado actual de sensores (latest)
 * - Histórico de lecturas con filtros (history)
 * - Estadísticas agregadas (stats)
 * - Descubrimiento de dispositivos y sensores (devices)
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SensorReadingController {

    private final SensorService sensorService;

    // ========================
    // Lecturas
    // ========================

    /**
     * GET /api/v1/readings
     * Obtiene todas las lecturas paginadas, ordenadas por timestamp descendente.
     *
     * @param page Número de página (default: 0)
     * @param size Tamaño de página (default: 20, max: 100)
     */
    @GetMapping("/readings")
    public ResponseEntity<ApiResponse<Page<SensorReadingResponse>>> getReadings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<SensorReadingResponse> readings = sensorService.getReadings(pageable);
        return ResponseEntity.ok(ApiResponse.ok(readings));
    }

    /**
     * GET /api/v1/readings/latest?deviceId=esp32_01
     * Obtiene la última lectura de cada sensor para un dispositivo.
     * Ideal para las cards de estado actual del dashboard.
     *
     * @param deviceId ID del dispositivo (requerido)
     */
    @GetMapping("/readings/latest")
    public ResponseEntity<ApiResponse<List<SensorReadingResponse>>> getLatestReadings(
            @RequestParam String deviceId) {

        List<SensorReadingResponse> readings = sensorService.getLatestReadings(deviceId);
        return ResponseEntity.ok(ApiResponse.ok(readings,
                String.format("Últimas lecturas del dispositivo %s", deviceId)));
    }

    /**
     * GET /api/v1/readings/history?deviceId=esp32_01&type=temperature&from=...&to=...
     * Obtiene lecturas históricas con filtros opcionales.
     * Diseñado para gráficos de series de tiempo.
     *
     * @param deviceId ID del dispositivo (opcional)
     * @param type     Tipo de sensor: "temperature", "humidity", etc. (opcional)
     * @param sensorId ID del sensor: "TMP_INT", "HUM_INT", etc. (opcional)
     * @param from     Inicio del rango ISO-8601, default: 24h atrás
     * @param to       Fin del rango ISO-8601, default: ahora
     * @param page     Número de página (default: 0)
     * @param size     Tamaño de página (default: 50, max: 500)
     */
    @GetMapping("/readings/history")
    public ResponseEntity<ApiResponse<Page<SensorReadingResponse>>> getHistory(
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String sensorId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        // Defaults: últimas 24 horas
        if (from == null) from = Instant.now().minus(24, ChronoUnit.HOURS);
        if (to == null) to = Instant.now();

        size = Math.min(size, 500);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "timestamp"));

        Page<SensorReadingResponse> readings = sensorService.getHistory(
                deviceId, type, sensorId, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.ok(readings));
    }

    // ========================
    // Estadísticas
    // ========================

    /**
     * GET /api/v1/readings/stats?deviceId=esp32_01&from=...&to=...
     * Calcula min, max, promedio y conteo por sensor usando MongoDB Aggregation.
     *
     * @param deviceId ID del dispositivo (requerido)
     * @param sensorId ID del sensor (opcional, null = todos)
     * @param from     Inicio del rango ISO-8601, default: 24h atrás
     * @param to       Fin del rango ISO-8601, default: ahora
     */
    @GetMapping("/readings/stats")
    public ResponseEntity<ApiResponse<List<SensorStatsResponse>>> getStats(
            @RequestParam String deviceId,
            @RequestParam(required = false) String sensorId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {

        if (from == null) from = Instant.now().minus(24, ChronoUnit.HOURS);
        if (to == null) to = Instant.now();

        List<SensorStatsResponse> stats = sensorService.getStats(deviceId, sensorId, from, to);
        return ResponseEntity.ok(ApiResponse.ok(stats,
                String.format("Estadísticas del dispositivo %s", deviceId)));
    }

    /**
     * GET /api/v1/devices
     * Lista todos los device IDs únicos registrados en el sistema.
     * Usado para el selector de dispositivos del dashboard.
     */
    @GetMapping("/devices")
    public ResponseEntity<ApiResponse<List<String>>> getDevices() {
        List<String> deviceIds = sensorService.getDeviceIds();
        return ResponseEntity.ok(ApiResponse.ok(deviceIds,
                String.format("Se encontraron %d dispositivos", deviceIds.size())));
    }

    /**
     * GET /api/v1/devices/{deviceId}/sensors
     * Lista los sensores únicos registrados para un dispositivo.
     * Devuelve sensorId, type y unit de cada sensor.
     *
     * @param deviceId ID del dispositivo
     */
    @GetMapping("/devices/{deviceId}/sensors")
    public ResponseEntity<ApiResponse<List<SensorInfoResponse>>> getSensorsForDevice(
            @PathVariable String deviceId) {

        List<SensorInfoResponse> sensors = sensorService.getSensorsForDevice(deviceId);
        return ResponseEntity.ok(ApiResponse.ok(sensors,
                String.format("Sensores del dispositivo %s", deviceId)));
    }
}
