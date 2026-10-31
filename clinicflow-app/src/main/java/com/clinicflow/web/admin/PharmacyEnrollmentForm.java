package com.clinicflow.web.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PharmacyEnrollmentForm {

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotBlank
    @Size(max = 50)
    private String licenceNumber;

    @NotBlank
    @Size(max = 200)
    private String addressLine1;

    @Size(max = 200)
    private String addressLine2;

    @NotBlank
    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String district;

    @NotBlank
    @Size(max = 100)
    private String state;

    @NotBlank
    @Size(max = 20)
    private String postalCode;

    @Size(max = 30)
    private String phone;

    @Email
    @Size(max = 254)
    private String email;

    @NotBlank(message = "Enter the pharmacy administrator's email.")
    @Email(message = "Enter a valid administrator email.")
    @Size(max = 254)
    private String adminEmail;
}
