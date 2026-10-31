package com.clinicflow.web.patient;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class BookingForm {

    @NotNull
    private Long clinicId;

    @NotNull
    private Long doctorId;

    @NotNull(message = "Choose a slot.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime slotStart;

    @Size(max = 500)
    private String reason;
}
