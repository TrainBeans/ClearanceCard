package org.trainbeans.clearancecard.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Represents a completed Form A clearance card.
 * Each field maps directly to one blank on NMRA/ORS Form 427-A.
 */
public record ClearanceCard(
        Long id,
        String station,
        LocalTime issuedTime,
        LocalDate issuedDate,
        String trainNumber,
        int orderCount,
        String signalStopFor,
        String operatorName,
        List<String> orderNumbers
) {}

