package org.trainbeans.clearancecard.exception;

public class ClearanceCardNotFoundException extends RuntimeException {

    public ClearanceCardNotFoundException(Long id) {
        super("Clearance card not found: " + id);
    }
}

