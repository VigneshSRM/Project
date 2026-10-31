package com.clinicflow.web.clinic;

import com.clinicflow.domain.entity.Clinic;
import com.clinicflow.domain.entity.ClinicMedicine;
import com.clinicflow.domain.repository.ClinicDoctorRepository;
import com.clinicflow.domain.repository.ClinicMedicineRepository;
import com.clinicflow.domain.repository.ClinicRepository;
import com.clinicflow.domain.repository.DoctorUserLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ClinicInventoryService {

    private final ClinicRepository clinicRepository;
    private final ClinicMedicineRepository medicineRepository;
    private final DoctorUserLinkRepository doctorUserLinkRepository;
    private final ClinicDoctorRepository clinicDoctorRepository;

    public ClinicInventoryService(ClinicRepository clinicRepository,
                                 ClinicMedicineRepository medicineRepository,
                                 DoctorUserLinkRepository doctorUserLinkRepository,
                                 ClinicDoctorRepository clinicDoctorRepository) {
        this.clinicRepository = clinicRepository;
        this.medicineRepository = medicineRepository;
        this.doctorUserLinkRepository = doctorUserLinkRepository;
        this.clinicDoctorRepository = clinicDoctorRepository;
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> optionsForDoctor(Long userId) {
        return listByDoctorEntities(userId).stream().map(item -> {
            java.util.Map<String, Object> option = new java.util.LinkedHashMap<>();
            option.put("medicine", item.getMedicine());
            option.put("dosage", item.getDosage());
            option.put("defaultFrequency", item.getDefaultFrequency());
            option.put("defaultDuration", item.getDefaultDuration());
            option.put("defaultQuantity", item.getDefaultQuantity());
            option.put("defaultUnit", item.getDefaultForm());
            option.put("defaultInstructions", item.getDefaultInstructions());
            return option;
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<ClinicMedicine> listByDoctorEntities(Long userId) {
        return doctorUserLinkRepository.findByUserUserId(userId)
                .flatMap(link -> clinicDoctorRepository.findByDoctorDoctorId(link.getDoctor().getDoctorId())
                        .stream().findFirst())
                .map(link -> medicineRepository.findByClinicClinicIdOrderByMedicineAsc(
                        link.getClinic().getClinicId()))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<ClinicMedicine> list(Long clinicId) {
        return medicineRepository.findByClinicClinicIdOrderByMedicineAsc(clinicId);
    }

    @Transactional
    public void add(Long clinicId, String medicine, String dosage, String frequency,
                    String duration, Integer quantity, String form, String instructions) {
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Clinic not found."));
        if (medicine == null || medicine.isBlank()) {
            throw new IllegalArgumentException("Enter a medicine name.");
        }
        String normalizedMedicine = medicine.trim();
        String normalizedDosage = dosage == null || dosage.isBlank() ? null : dosage.trim();
        if (medicineRepository.existsByClinicClinicIdAndMedicineAndDosage(
                clinicId, normalizedMedicine, normalizedDosage)) {
            throw new IllegalArgumentException("This medicine and dosage already exists in inventory.");
        }
        ClinicMedicine item = new ClinicMedicine();
        item.setClinic(clinic);
        item.setMedicine(normalizedMedicine);
        item.setDosage(normalizedDosage);
        item.setDefaultFrequency(frequency == null || frequency.isBlank() ? null : frequency.trim());
        item.setDefaultDuration(duration == null || duration.isBlank() ? null : duration.trim());
        item.setDefaultQuantity(quantity);
        item.setDefaultForm(form == null || form.isBlank() ? null : form.trim());
        item.setDefaultInstructions(instructions == null || instructions.isBlank() ? null : instructions.trim());
        item.setCreatedAt(LocalDateTime.now(java.time.ZoneOffset.UTC));
        medicineRepository.save(item);
    }

    @Transactional
    public ClinicMedicine get(Long clinicId, Long medicineId) {
        ClinicMedicine item = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found."));
        if (!item.getClinic().getClinicId().equals(clinicId)) {
            throw new IllegalArgumentException("Inventory item does not belong to this clinic.");
        }
        return item;
    }

    @Transactional
    public void update(Long clinicId, Long medicineId, String medicine, String dosage, String frequency,
                       String duration, Integer quantity, String form, String instructions) {
        ClinicMedicine item = get(clinicId, medicineId);
        if (medicine == null || medicine.isBlank()) {
            throw new IllegalArgumentException("Enter a medicine name.");
        }
        String normalizedMedicine = medicine.trim();
        String normalizedDosage = dosage == null || dosage.isBlank() ? null : dosage.trim();
        if (!normalizedMedicine.equals(item.getMedicine())
                || (normalizedDosage == null ? item.getDosage() != null : !normalizedDosage.equals(item.getDosage()))) {
            if (medicineRepository.existsByClinicClinicIdAndMedicineAndDosage(
                    clinicId, normalizedMedicine, normalizedDosage)) {
                throw new IllegalArgumentException("This medicine and dosage already exists in inventory.");
            }
        }
        item.setMedicine(normalizedMedicine);
        item.setDosage(normalizedDosage);
        item.setDefaultFrequency(frequency == null || frequency.isBlank() ? null : frequency.trim());
        item.setDefaultDuration(duration == null || duration.isBlank() ? null : duration.trim());
        item.setDefaultQuantity(quantity);
        item.setDefaultForm(form == null || form.isBlank() ? null : form.trim());
        item.setDefaultInstructions(instructions == null || instructions.isBlank() ? null : instructions.trim());
        medicineRepository.save(item);
    }

    @Transactional(readOnly = true)
    public String exportCsv(Long clinicId) {
        StringBuilder builder = new StringBuilder();
        builder.append("Medicine,Dosage,Default Frequency,Default Duration,Default Quantity,Form,Default Instructions\n");
        for (ClinicMedicine item : list(clinicId)) {
            builder.append(csv(item.getMedicine())).append(',')
                    .append(csv(item.getDosage())).append(',')
                    .append(csv(item.getDefaultFrequency())).append(',')
                    .append(csv(item.getDefaultDuration())).append(',')
                    .append(item.getDefaultQuantity() == null ? "" : item.getDefaultQuantity()).append(',')
                    .append(csv(item.getDefaultForm())).append(',')
                    .append(csv(item.getDefaultInstructions())).append('\n');
        }
        return builder.toString();
    }

    public String templateCsv() {
        return "Medicine,Dosage,Default Frequency,Default Duration,Default Quantity,Form,Default Instructions\n"
                + "Paracetamol,500 mg,Once a day,5 days,20,Tablet,Take with food\n";
    }

    @Transactional
    public int importCsv(Long clinicId, java.io.Reader reader) throws java.io.IOException {
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Clinic not found."));
        java.io.BufferedReader buffered = new java.io.BufferedReader(reader);
        String line;
        boolean header = true;
        int imported = 0;
        while ((line = buffered.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            List<String> fields = parseCsvLine(line);
            if (header) {
                header = false;
                continue;
            }
            if (fields.isEmpty() || fields.get(0).trim().isBlank()) {
                continue;
            }
            String medicine = fields.get(0).trim();
            String dosage = field(fields, 1);
            String frequency = field(fields, 2);
            String duration = field(fields, 3);
            Integer quantity = parseInteger(field(fields, 4));
            String form = field(fields, 5);
            String instructions = field(fields, 6);
            if (medicineRepository.existsByClinicClinicIdAndMedicineAndDosage(clinicId, medicine, dosage)) {
                continue;
            }
            ClinicMedicine item = new ClinicMedicine();
            item.setClinic(clinic);
            item.setMedicine(medicine);
            item.setDosage(dosage);
            item.setDefaultFrequency(frequency);
            item.setDefaultDuration(duration);
            item.setDefaultQuantity(quantity);
            item.setDefaultForm(form);
            item.setDefaultInstructions(instructions);
            item.setCreatedAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
            medicineRepository.save(item);
            imported++;
        }
        return imported;
    }

    private String field(List<String> fields, int index) {
        if (index >= fields.size()) {
            return null;
        }
        String value = fields.get(index).trim();
        return value.isBlank() ? null : value;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result;
    }

    private String csv(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @Transactional
    public void delete(Long clinicId, Long medicineId) {
        ClinicMedicine item = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found."));
        if (!item.getClinic().getClinicId().equals(clinicId)) {
            throw new IllegalArgumentException("Inventory item does not belong to this clinic.");
        }
        medicineRepository.delete(item);
    }
}
