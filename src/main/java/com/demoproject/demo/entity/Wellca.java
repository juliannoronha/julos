package com.demoproject.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.demoproject.demo.config.AttributeEncryptor;

/**
 * Entity representing daily pharmacy operations and metrics tracking.
 * Stores delivery counts, prescription statistics, and professional services data.
 *
 * @Entity Maps this class to the wellca_entries database table
 * @Table Specifies the table name as "wellca_entries"
 * @Data Lombok annotation for getters, setters, equals, hashCode and toString
 */
@Entity
@Table(name = "wellca_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Wellca {

    /**
     * Unique identifier for each entry
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ========== Delivery Tracking Section ========== */
    
    /**
     * Date of the entry record
     * Required field that cannot be null
     */
    @Column(nullable = false)
    private LocalDate date;

    /**
     * Daily delivery counts for Purolator services
     */
    @Column
    private Integer purolator;

    /**
     * Daily delivery counts for FedEx services
     */
    @Column
    private Integer fedex;

    /**
     * Daily delivery counts for OneCourier services
     */
    @Column(name = "one_courier")
    private Integer oneCourier;

    /**
     * Daily delivery counts for GoBolt services
     */
    @Column
    private Integer goBolt;

    /* ========== RX Sales Summary Section ========== */
    
    /**
     * Count of new prescriptions processed
     */
    private Integer newRx;

    /**
     * Count of prescription refills processed
     */
    private Integer refill;

    /**
     * Count of prescription reauthorizations processed
     */
    private Integer reAuth;

    /**
     * Count of prescriptions on hold
     */
    private Integer hold;

    /* ========== Weekly Profiles Section ========== */
    
    /**
     * Number of patient profiles entered into the system
     */
    private Integer profilesEntered;

    /**
     * Count of prescriptions filled by pharmacist
     */
    private Integer whoFilledRx;

    /**
     * Percentage of active profiles
     * Stored with 2 decimal precision
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal activePercentage;

    /* ========== Professional Services Section ========== */
    
    /**
     * Type of professional service provided
     * Stored as a string representation of ServiceType enum
     */
    @Column(name = "service_type")
    private String serviceType;

    /**
     * Cost associated with the professional service
     * Stored with 2 decimal precision
     */
    @Column(name = "service_cost", precision = 10, scale = 2)
    private BigDecimal serviceCost;

    /* ========== Calculated Fields Section ========== */
    
    /**
     * Calculates the total delivery counts across all courier services
     * @return Sum of all delivery counts as Integer
     */
    @Transient
    public Integer getTotalDeliveries() {
        return (purolator != null ? purolator : 0)
             + (fedex != null ? fedex : 0)
             + (oneCourier != null ? oneCourier : 0)
             + (goBolt != null ? goBolt : 0);
    }

    /**
     * Calculates total number of prescriptions filled
     * Includes new, refill, and reauthorized prescriptions
     * @return Total count of filled prescriptions
     */
    @Transient
    public Integer getTotalFilled() {
        return newRx + refill + reAuth;
    }

    /**
     * Calculates total number of prescriptions entered
     * Includes all filled prescriptions plus those on hold
     * @return Total count of entered prescriptions
     */
    @Transient
    public Integer getTotalEntered() {
        return getTotalFilled() + hold;
    }

    /**
     * Validates data before persistence operations
     * Ensures date is not null and initializes null delivery counts to zero
     * @throws IllegalStateException if date is null
     */
    @PrePersist
    @PreUpdate
    protected void validateData() {
        if (date == null) {
            throw new IllegalStateException("Date cannot be null");
        }
        if (purolator == null) purolator = 0;
        if (fedex == null) fedex = 0;
        if (oneCourier == null) oneCourier = 0;
        if (goBolt == null) goBolt = 0;
    }

    /**
     * Enumeration of available professional pharmacy services
     */
    public enum ServiceType {
        FOLLOW_UP_MED_REVIEW,
        OTHER_INJECTION_BILLED,
        RENEWAL_CHARGED,
        FOLLOW_UP_DIABETIC_MED_REVIEW,
        MINOR_ALIGNMENT_VIRTUAL,
        ANNUAL_DIABETIC_MED_REVIEW,
        ANNUAL_MED_REVIEW,
        DIABETIC_EDUCATION_REVIEW,
        MINOR_ALIGNMENT_IN_PERSON
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public BigDecimal getServiceCost() {
        return serviceCost;
    }

    public void setServiceCost(BigDecimal serviceCost) {
        this.serviceCost = serviceCost;
    }

    @Column(name = "patient_name")
    @Convert(converter = AttributeEncryptor.class)  // For encryption
    private String patientName;

    @Column(name = "patient_dob")
    private LocalDate patientDob;

    @Column(name = "pharmacist_name")
    private String pharmacistName;

    // Add getters and setters
    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public LocalDate getPatientDob() {
        return patientDob;
    }

    public void setPatientDob(LocalDate patientDob) {
        this.patientDob = patientDob;
    }

    public String getPharmacistName() {
        return pharmacistName;
    }

    public void setPharmacistName(String pharmacistName) {
        this.pharmacistName = pharmacistName;
    }
}
