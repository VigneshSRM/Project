package com.clinicflow.web.clinic;

import com.clinicflow.domain.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class PatientForm {

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @NotNull(message = "Enter the date of birth.")
    @Past(message = "Date of birth must be in the past.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dob;

    private Gender gender;

    @Size(max = 30)
    private String mobile;

    @Email
    @Size(max = 254)
    private String email;

    @Size(max = 200)
    private String addressLine1;

    @Size(max = 200)
    private String addressLine2;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String district;

    @Size(max = 100)
    private String state;

    @Size(max = 20)
    private String postalCode;

    @Size(max = 10)
    private String bloodGroup;

    @Size(max = 1000)
    private String allergies;

    @Size(max = 1000)
    private String conditions;

    @Size(max = 200)
    private String emergencyName;

    @Size(max = 30)
    private String emergencyPhone;
}
