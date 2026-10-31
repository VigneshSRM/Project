package com.clinicflow.web.admin;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ClinicEnrollmentForm {

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotBlank
    @Size(max = 200)
    private String addressLine1;

    @Size(max = 200)
    private String addressLine2;

    @NotBlank
    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String area;

    @Size(max = 100)
    private String district;

    @NotBlank
    @Size(max = 100)
    private String state;

    @NotBlank
    @Size(max = 20)
    private String postalCode;

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal longitude;

    @Size(max = 30)
    private String phone;

    @Email
    @Size(max = 254)
    private String email;

    @Size(max = 500)
    private String openingHours;

    @Size(max = 200)
    private String fees;

    @NotBlank(message = "Enter the clinic administrator's email.")
    @Email(message = "Enter a valid administrator email.")
    @Size(max = 254)
    private String adminEmail;
}
