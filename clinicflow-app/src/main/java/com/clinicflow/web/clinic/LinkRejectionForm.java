package com.clinicflow.web.clinic;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkRejectionForm {

    @Size(max = 500, message = "Note is too long.")
    private String note;
}
