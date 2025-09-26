package com.mercedes.workflowrh.controller;

import com.mercedes.workflowrh.dto.dashboardDto.*;
import com.mercedes.workflowrh.service.DemandeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DemandeService demandeService;
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    // 1. Vue d'ensemble des demandes
    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewDTO> getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            logger.info("Getting dashboard overview for period: {} to {}", startDate, endDate);

            LocalDateTime start = (startDate != null)
                    ? startDate.atStartOfDay()
                    : LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);

            LocalDateTime end = (endDate != null)
                    ? endDate.atTime(23, 59, 59)
                    : LocalDateTime.now();

            if (start.isAfter(end)) {
                logger.warn("Invalid date range: start {} is after end {}", start, end);
                return ResponseEntity.badRequest().body(null);
            }

            DashboardOverviewDTO overview = demandeService.getDashboardOverview(
                    start.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    end.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            return ResponseEntity.ok(overview);

        } catch (Exception e) {
            logger.error("Error getting dashboard overview", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    // 2. Répartition des statuts des demandes
    @GetMapping("/status-distribution")
    public ResponseEntity<List<StatusDistributionDTO>> getStatusDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            logger.info("Getting status distribution for period: {} to {}", startDate, endDate);

            LocalDateTime start = (startDate != null)
                    ? startDate.atStartOfDay()
                    : LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);

            LocalDateTime end = (endDate != null)
                    ? endDate.atTime(LocalTime.MAX)
                    : LocalDateTime.now();

            if (start.isAfter(end)) {
                logger.warn("Invalid date range: start {} is after end {}", start, end);
                return ResponseEntity.badRequest().body(null);
            }

            List<StatusDistributionDTO> distribution = demandeService.getStatusDistribution(start, end);
            return ResponseEntity.ok(distribution);

        } catch (Exception e) {
            logger.error("Error getting status distribution", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    // 3. Jours de congés pris et solde
    @GetMapping("/leave-balance")
    public ResponseEntity<LeaveBalanceDTO> getLeaveBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            logger.info("Getting leave balance for period: {} to {}", startDate, endDate);

            LocalDateTime start = (startDate != null)
                    ? startDate.atStartOfDay()
                    : LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);

            LocalDateTime end = (endDate != null)
                    ? endDate.atTime(23, 59, 59)
                    : LocalDateTime.now();

            if (start.isAfter(end)) {
                logger.warn("Invalid date range: start {} is after end {}", start, end);
                return ResponseEntity.badRequest().body(null);
            }

            LeaveBalanceDTO balance = demandeService.getLeaveBalanceOverview(
                    start.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    end.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            return ResponseEntity.ok(balance);

        } catch (Exception e) {
            logger.error("Error getting leave balance", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    // 3b. Ventilation des congés pris par service
    @GetMapping("/leave-by-service")
    public ResponseEntity<List<ServiceLeaveDTO>> getLeaveByService(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            logger.info("Getting leave by service for period: {} to {}", startDate, endDate);

            LocalDateTime start = (startDate != null)
                    ? startDate.atStartOfDay()
                    : LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);

            LocalDateTime end = (endDate != null)
                    ? endDate.atTime(23, 59, 59)
                    : LocalDateTime.now();

            if (start.isAfter(end)) {
                logger.warn("Invalid date range: start {} is after end {}", start, end);
                return ResponseEntity.badRequest().body(null);
            }

            List<ServiceLeaveDTO> serviceLeaves = demandeService.getLeaveBalanceByService(
                    start.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    end.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            return ResponseEntity.ok(serviceLeaves);

        } catch (Exception e) {
            logger.error("Error getting leave by service", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    // 4. Demandes acceptées par service
    @GetMapping("/accepted-requests")
    public ResponseEntity<List<AcceptedRequestsDTO>> getAcceptedRequests(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            logger.info("Getting accepted requests for period: {} to {}", startDate, endDate);

            LocalDateTime start = (startDate != null)
                    ? startDate.atStartOfDay()
                    : LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);

            LocalDateTime end = (endDate != null)
                    ? endDate.atTime(23, 59, 59)
                    : LocalDateTime.now();

            if (start.isAfter(end)) {
                logger.warn("Invalid date range: start {} is after end {}", start, end);
                return ResponseEntity.badRequest().body(null);
            }

            List<AcceptedRequestsDTO> acceptedRequests = demandeService.getAcceptedRequestsByService(
                    start.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    end.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            return ResponseEntity.ok(acceptedRequests);

        } catch (Exception e) {
            logger.error("Error getting accepted requests", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    // 5. Solde de congés par service
    @GetMapping("/employee-leave-balance")
    public ResponseEntity<List<EmployeeLeaveBalanceDTO>> getEmployeeLeaveBalance(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            logger.info("Getting employee leave balance for service: {}, period: {} to {}", service, startDate, endDate);

            LocalDateTime start = (startDate != null)
                    ? startDate.atStartOfDay()
                    : LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);

            LocalDateTime end = (endDate != null)
                    ? endDate.atTime(23, 59, 59)
                    : LocalDateTime.now();

            if (start.isAfter(end)) {
                logger.warn("Invalid date range: start {} is after end {}", start, end);
                return ResponseEntity.badRequest().body(null);
            }

            List<EmployeeLeaveBalanceDTO> employeeBalances = demandeService.getLeaveBalanceDetails(
                    service,
                    start.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    end.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            return ResponseEntity.ok(employeeBalances);

        } catch (Exception e) {
            logger.error("Error getting employee leave balance", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }
}