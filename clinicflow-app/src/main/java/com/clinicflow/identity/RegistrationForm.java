package com.clinicflow.identity;

import com.clinicflow.domain.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Patient self-registration, grouped as Personal, Address and Optional medical.
 * Bean Validation covers field-level rules; cross-field rules live in the
 * controller/service so they can be returned as field errors.
 */
public class RegistrationForm {

    // Personal
    @NotBlank(message = "First name is required.")
    @Size(max = 100, message = "First name is too long.")
    private String firstName;

    @NotBlank(message = "Last name is required.")
    @Size(max = 100, message = "Last name is too long.")
    private String lastName;

    @NotNull(message = "Date of birth is required.")
    @Past(message = "Date of birth must be in the past.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dob;

    private Gender gender = Gender.UNKNOWN;

    @Pattern(regexp = "^$|^[0-9+()\\-\\s]{7,30}$",
            message = "Enter a valid mobile number (7-30 digits and phone symbols).")
    private String mobile;

    @NotBlank(message = "Email address is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 254, message = "Email address is too long.")
    private String email;

    @NotBlank(message = "Password is required.")
    private String password;

    @NotBlank(message = "Please confirm your password.")
    private String confirmPassword;

    // Address (optional, but stored as patient-entered data when supplied)
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

    // Optional medical
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public String getEmergencyName() {
        return emergencyName;
    }

    public void setEmergencyName(String emergencyName) {
        this.emergencyName = emergencyName;
    }

    public String getEmergencyPhone() {
        return emergencyPhone;
    }

    public void setEmergencyPhone(String emergencyPhone) {
        this.emergencyPhone = emergencyPhone;
    }
}
