package org.trainbeans.clearancecard.service;

import org.springframework.stereotype.Service;
import org.trainbeans.clearancecard.exception.ClearanceCardNotFoundException;
import org.trainbeans.clearancecard.model.ClearanceCard;
import org.trainbeans.clearancecard.model.ClearanceCardRequest;
import org.trainbeans.clearancecard.repository.ClearanceCardRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class ClearanceCardService {

    private final ClearanceCardRepository repository;

    public ClearanceCardService(ClearanceCardRepository repository) {
        this.repository = repository;
    }

    public ClearanceCard create(ClearanceCardRequest request) {
        return repository.save(request);
    }

    public ClearanceCard findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ClearanceCardNotFoundException(id));
    }

    /**
     * Returns all cards, or only cards for the given date if {@code date} is not null.
     */
    public List<ClearanceCard> findAll(LocalDate date) {
        return (date != null) ? repository.findByDate(date) : repository.findAll();
    }
}

