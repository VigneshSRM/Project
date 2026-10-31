package com.clinicflow.web.doctor;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class ConsultationForm {

    @Size(max = 2000)
    private String diagnosis;

    @Size(max = 10000)
    private String notes;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime followUp;

    @Size(max = 300)
    private String addendumReason;

    @Size(max = 10000)
    private String addendumText;
}
