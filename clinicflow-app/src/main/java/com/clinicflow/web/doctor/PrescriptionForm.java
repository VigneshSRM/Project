package com.clinicflow.web.doctor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PrescriptionForm {

    @Min(1)
    @Max(365)
    private int validDays = 30;

    @Valid
    private List<ItemForm> items = new ArrayList<>();

    public PrescriptionForm() {
        items.add(new ItemForm());
    }

    @Getter
    @Setter
    public static class ItemForm {

        @Size(max = 200)
        private String medicine;

        @Size(max = 100)
        private String dosage;

        @Size(max = 100)
        private String frequency;

        @Size(max = 100)
        private String duration;

        @Min(1)
        @Max(10000)
        private Integer quantity;

        @Size(max = 30)
        private String quantityUnit;

        @Size(max = 500)
        private String instructions;

        private boolean lifetime;
    }
}
