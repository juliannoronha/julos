package com.demoproject.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WellcaSummaryDTO {
    
    /* Time Period */
    private LocalDate startDate;
    private LocalDate endDate;

    /* Delivery Totals */
    private BigDecimal totalDeliveryCost;
    private BigDecimal purolatorTotal;
    private BigDecimal fedexTotal;
    private BigDecimal oneCourierTotal;
    private BigDecimal goBoltTotal;

    /* RX Summary */
    private Integer totalNewRx;
    private Integer totalRefills;
    private Integer totalReAuth;
    private Integer totalHold;
    private Integer totalFilled;
    private BigDecimal averageRxPerDay;

    /* Profile Statistics */
    private Integer totalProfilesEntered;
    private BigDecimal averageProfilesPerDay;
    private BigDecimal averageActivePercentage;

    /* Service Summary */
    private Integer totalServices;
    private BigDecimal totalServiceRevenue;
    private BigDecimal averageServiceCost;
}
