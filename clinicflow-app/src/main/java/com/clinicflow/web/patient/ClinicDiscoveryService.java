package com.clinicflow.web.patient;

import com.clinicflow.domain.entity.Clinic;
import com.clinicflow.domain.entity.ClinicDoctor;
import com.clinicflow.domain.entity.Doctor;
import com.clinicflow.domain.enums.ClinicStatus;
import com.clinicflow.domain.enums.DoctorStatus;
import com.clinicflow.domain.repository.ClinicDoctorRepository;
import com.clinicflow.domain.repository.ClinicRepository;
import com.clinicflow.domain.repository.DoctorScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ClinicDiscoveryService {

    private final ClinicRepository clinicRepository;
    private final ClinicDoctorRepository clinicDoctorRepository;
    private final DoctorScheduleRepository scheduleRepository;

    public ClinicDiscoveryService(ClinicRepository clinicRepository,
                                  ClinicDoctorRepository clinicDoctorRepository,
                                  DoctorScheduleRepository scheduleRepository) {
        this.clinicRepository = clinicRepository;
        this.clinicDoctorRepository = clinicDoctorRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Transactional(readOnly = true)
    public List<ClinicCard> search(String query, Double latitude, Double longitude) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return clinicRepository.findByStatusOrderByNameAsc(ClinicStatus.ACTIVE).stream()
                .filter(clinic -> matches(clinic, q))
                .map(clinic -> toCard(clinic, latitude, longitude))
                .sorted(Comparator.comparing(ClinicCard::distanceKm,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ClinicCard::name))
                .toList();
    }

    @Transactional(readOnly = true)
    public ClinicProfile detail(UUID publicUuid) {
        Clinic clinic = clinicRepository.findByPublicUuid(publicUuid)
                .filter(candidate -> candidate.getStatus() == ClinicStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Clinic not found."));
        return profileFor(clinic);
    }

    @Transactional(readOnly = true)
    public ClinicProfile profileById(Long clinicId) {
        Clinic clinic = clinicRepository.findById(clinicId)
                .filter(candidate -> candidate.getStatus() == ClinicStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Clinic not found."));
        return profileFor(clinic);
    }

    private ClinicProfile profileFor(Clinic clinic) {
        List<DoctorOption> doctors = clinicDoctorRepository
                .findByClinicClinicIdAndStatus(clinic.getClinicId(), DoctorStatus.ACTIVE).stream()
                .filter(link -> link.getDoctor().getStatus() == DoctorStatus.ACTIVE)
                .map(link -> new DoctorOption(link.getDoctor().getDoctorId(), link.getDoctor().getName(),
                        link.getDoctor().getSpecialization(), link.getFee(),
                        scheduleRepository.findByDoctorDoctorIdAndClinicClinicId(
                                link.getDoctor().getDoctorId(), clinic.getClinicId()).size()))
                .toList();
        return new ClinicProfile(clinic.getClinicId(), clinic.getPublicUuid(), clinic.getName(),
                clinic.getAddressLine1(), clinic.getAddressLine2(), clinic.getCity(), clinic.getArea(),
                clinic.getDistrict(), clinic.getState(), clinic.getPostalCode(), clinic.getPhone(),
                clinic.getEmail(), clinic.getOpeningHours(), clinic.getFees(), clinic.getLatitude(),
                clinic.getLongitude(), doctors);
    }

    private boolean matches(Clinic clinic, String q) {
        if (q.isBlank()) {
            return true;
        }
        return contains(clinic.getName(), q) || contains(clinic.getCity(), q)
                || contains(clinic.getArea(), q) || contains(clinic.getDistrict(), q)
                || contains(clinic.getPostalCode(), q) || contains(clinic.getState(), q);
    }

    private ClinicCard toCard(Clinic clinic, Double latitude, Double longitude) {
        long doctors = clinicDoctorRepository
                .findByClinicClinicIdAndStatus(clinic.getClinicId(), DoctorStatus.ACTIVE).size();
        Double distance = null;
        if (latitude != null && longitude != null && clinic.getLatitude() != null
                && clinic.getLongitude() != null) {
            distance = Math.round(distanceKm(latitude, longitude,
                    clinic.getLatitude().doubleValue(), clinic.getLongitude().doubleValue()) * 10.0) / 10.0;
        }
        return new ClinicCard(clinic.getPublicUuid(), clinic.getName(), clinic.getAddressLine1(),
                clinic.getCity(), clinic.getArea(), clinic.getState(), clinic.getPostalCode(),
                clinic.getPhone(), clinic.getOpeningHours(), clinic.getFees(), doctors, distance,
                clinic.getLatitude(), clinic.getLongitude());
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }

    private static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double earth = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earth * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public record ClinicCard(UUID publicUuid, String name, String addressLine1, String city, String area,
                             String state, String postalCode, String phone, String openingHours,
                             String fees, long doctors, Double distanceKm, BigDecimal latitude,
                             BigDecimal longitude) {
    }

    public record DoctorOption(Long id, String name, String specialization, BigDecimal fee,
                               long schedules) {
    }

    public record ClinicProfile(Long id, UUID publicUuid, String name, String addressLine1,
                                String addressLine2, String city, String area, String district,
                                String state, String postalCode, String phone, String email,
                                String openingHours, String fees, BigDecimal latitude,
                                BigDecimal longitude, List<DoctorOption> doctors) {
    }
}
