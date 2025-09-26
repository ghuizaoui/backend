package com.mercedes.workflowrh.service.impl;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class HolidayService {

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    public boolean isHoliday(LocalDate date) {
        List<Map<String, Object>> holidays = getTunisianHolidays(date.getYear());
        return holidays.stream()
                .anyMatch(holiday -> {
                    String holidayDate = holiday.get("date").toString();
                    return holidayDate.equals(date.toString());
                });
    }

    public boolean isFreeDay(LocalDate date) {
        return isWeekend(date) || isHoliday(date);
    }

    public long calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
        return Stream.iterate(startDate, date -> date.plusDays(1))
                .limit(ChronoUnit.DAYS.between(startDate, endDate) + 1)
                .filter(date -> !isFreeDay(date))
                .count();
    }

    public double calculateWorkingHours(LocalTime startTime, LocalTime endTime, LocalDate date) {
        if (isFreeDay(date)) {
            return 0.0;
        }

        long minutes = ChronoUnit.MINUTES.between(startTime, endTime);
        return minutes / 60.0; // Retourne les heures travaillées
    }

    public List<Map<String, Object>> getTunisianHolidays(int year) {
        try {
            String url = "https://date.nager.at/api/v3/PublicHolidays/" + year + "/TN";
            ResponseEntity<List<Map<String, Object>>> response =
                    restTemplate.exchange(url, HttpMethod.GET, null,
                            new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            return response.getBody();
        } catch (Exception e) {
            // Fallback en cas d'erreur API
            return List.of();
        }
    }
}