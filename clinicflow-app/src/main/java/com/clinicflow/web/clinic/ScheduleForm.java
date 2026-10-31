package com.clinicflow.web.clinic;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ScheduleForm {

    @NotNull(message = "Choose at least one weekday.")
    @Size(min = 1, message = "Choose at least one weekday.")
    private List<Integer> weekdays = new ArrayList<>(List.of(1));

    @NotNull(message = "Enter a start time.")
    private LocalTime startTime = LocalTime.of(9, 0);

    @NotNull(message = "Enter an end time.")
    private LocalTime endTime = LocalTime.of(17, 0);

    @NotNull
    @Min(5)
    @Max(120)
    private Integer slotMinutes = 15;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate effectiveFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate effectiveTo;
}
