// src/main/java/com/mercedes/workflowrh/controller/DashboardController.java
package com.mercedes.workflowrh.controller;

import com.mercedes.workflowrh.dto.*;
import com.mercedes.workflowrh.dto.dashboardDto.*;
import com.mercedes.workflowrh.service.DemandeService;
import lombok.RequiredArgsConstructor;
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

    // 1. Vue d'ensemble des demandes
    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewDTO> getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        String start = startDate != null ? startDate.atStartOfDay().toString() : null;
        String end = endDate != null ? endDate.atTime(23, 59, 59).toString() : null;

        DashboardOverviewDTO overview = demandeService.getDashboardOverview(start, end);
        return ResponseEntity.ok(overview);
    }

    // 2. Répartition des statuts des demandes


    // 3. Jours de congés pris et solde
    @GetMapping("/leave-balance")
    public ResponseEntity<LeaveBalanceDTO> getLeaveBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        String start = startDate != null ? startDate.atStartOfDay().toString() : null;
        String end = endDate != null ? endDate.atTime(23, 59, 59).toString() : null;

        LeaveBalanceDTO balance = demandeService.getLeaveBalanceOverview(start, end);
        return ResponseEntity.ok(balance);
    }

    // 3b. Ventilation des congés pris par service
    @GetMapping("/leave-by-service")
    public ResponseEntity<List<ServiceLeaveDTO>> getLeaveByService(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        String start = startDate != null ? startDate.atStartOfDay().toString() : null;
        String end = endDate != null ? endDate.atTime(23, 59, 59).toString() : null;

        List<ServiceLeaveDTO> serviceLeaves = demandeService.getLeaveBalanceByService(start, end);
        return ResponseEntity.ok(serviceLeaves);
    }

    // 4. Demandes acceptées par service
    @GetMapping("/accepted-requests")
    public ResponseEntity<List<AcceptedRequestsDTO>> getAcceptedRequests(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        String start = startDate != null ? startDate.atStartOfDay().toString() : null;
        String end = endDate != null ? endDate.atTime(23, 59, 59).toString() : null;

        List<AcceptedRequestsDTO> acceptedRequests = demandeService.getAcceptedRequestsByService(start, end);
        return ResponseEntity.ok(acceptedRequests);
    }

    // 5. Solde de congés par service
    @GetMapping("/employee-leave-balance")
    public ResponseEntity<List<EmployeeLeaveBalanceDTO>> getEmployeeLeaveBalance(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        String start = startDate != null ? startDate.atStartOfDay().toString() : null;
        String end = endDate != null ? endDate.atTime(23, 59, 59).toString() : null;

        List<EmployeeLeaveBalanceDTO> employeeBalances = demandeService.getLeaveBalanceDetails(service, start, end);
        return ResponseEntity.ok(employeeBalances);
    }

    @GetMapping("/status-distribution")
    public ResponseEntity<List<StatusDistributionDTO>> getStatusDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            // Set default dates if not provided
            LocalDateTime start = (startDate != null)
                    ? startDate.atStartOfDay()
                    : LocalDateTime.of(2000, 1, 1, 0, 0); // Default start date

            LocalDateTime end = (endDate != null)
                    ? endDate.atTime(LocalTime.MAX)
                    : LocalDateTime.now(); // Default to current time

            // Validate date range
            if (start.isAfter(end)) {
                return ResponseEntity.badRequest().body(null);
            }

            List<StatusDistributionDTO> distribution = demandeService.getStatusDistribution(start, end);
            return ResponseEntity.ok(distribution);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

}