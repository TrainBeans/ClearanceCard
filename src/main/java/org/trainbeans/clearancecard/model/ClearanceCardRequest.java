package org.trainbeans.clearancecard.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Inbound DTO for creating a new Form A clearance card.
 * orderCount is the operator's stated count and is stored as-is without
 * validation against the size of orderNumbers.
 */
public record ClearanceCardRequest(
        String station,
        LocalTime issuedTime,
        LocalDate issuedDate,
        String trainNumber,
        int orderCount,
        String signalStopFor,
        String operatorName,
        List<String> orderNumbers
) {}

