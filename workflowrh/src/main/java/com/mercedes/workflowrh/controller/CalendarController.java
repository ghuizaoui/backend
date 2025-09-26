package com.mercedes.workflowrh.controller;

import com.mercedes.workflowrh.service.impl.HolidayService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/calendar")
public class CalendarController {




    private final HolidayService holidayService;

    public CalendarController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    @GetMapping("/holidays/{year}")
    public List<Map<String, Object>> getHolidays(@PathVariable int year) {
        return holidayService.getTunisianHolidays(year);
    }

    @GetMapping("/isFreeDay/{date}")
    public boolean isFreeDay(@PathVariable String date) {
        LocalDate localDate = LocalDate.parse(date);
        boolean weekend = holidayService.isWeekend(localDate);

        boolean holiday = holidayService.getTunisianHolidays(localDate.getYear()).stream()
                .anyMatch(h -> h.get("date").toString().equals(localDate.toString()));

        return weekend || holiday;
    }

}
