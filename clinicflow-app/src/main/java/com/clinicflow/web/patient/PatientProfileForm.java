package com.clinicflow.web.patient;

import com.clinicflow.domain.entity.Patient;
import com.clinicflow.domain.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Editable portion of the patient's own profile. Identity fields that require
 * verification (email, mobile, date of birth) are intentionally not part of this
 * form; they are shown read-only on the page.
 */
@Getter
@Setter
public class PatientProfileForm {

    @NotBlank(message = "First name is required.")
    @Size(max = 100, message = "First name is too long.")
    private String firstName;

    @NotBlank(message = "Last name is required.")
    @Size(max = 100, message = "Last name is too long.")
    private String lastName;

    private Gender gender;

    @Size(max = 200, message = "Address line 1 is too long.")
    private String addressLine1;

    @Size(max = 200, message = "Address line 2 is too long.")
    private String addressLine2;

    @Size(max = 100, message = "City is too long.")
    private String city;

    @Size(max = 100, message = "District is too long.")
    private String district;

    @Size(max = 100, message = "State is too long.")
    private String state;

    @Size(max = 20, message = "Postal code is too long.")
    private String postalCode;

    @Size(max = 10, message = "Blood group is too long.")
    private String bloodGroup;

    @Size(max = 1000, message = "Allergies are too long.")
    private String allergies;

    @Size(max = 1000, message = "Conditions are too long.")
    private String conditions;

    @Size(max = 200, message = "Emergency contact name is too long.")
    private String emergencyName;

    @Pattern(regexp = "^$|^[0-9+()\\-\\s]{7,30}$",
            message = "Enter a valid emergency phone number.")
    private String emergencyPhone;

    public static PatientProfileForm from(Patient patient) {
        PatientProfileForm form = new PatientProfileForm();
        form.setFirstName(patient.getFirstName());
        form.setLastName(patient.getLastName());
        form.setGender(patient.getGender() == null ? Gender.UNKNOWN : patient.getGender());
        form.setAddressLine1(patient.getAddressLine1());
        form.setAddressLine2(patient.getAddressLine2());
        form.setCity(patient.getCity());
        form.setDistrict(patient.getDistrict());
        form.setState(patient.getState());
        form.setPostalCode(patient.getPostalCode());
        form.setBloodGroup(patient.getBloodGroup());
        form.setAllergies(patient.getAllergies());
        form.setConditions(patient.getConditions());
        form.setEmergencyName(patient.getEmergencyName());
        form.setEmergencyPhone(patient.getEmergencyPhone());
        return form;
    }
}
