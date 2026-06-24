package org.trainbeans.clearancecard.repository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.trainbeans.clearancecard.model.ClearanceCard;
import org.trainbeans.clearancecard.model.ClearanceCardRequest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
@JdbcTest
@Import(ClearanceCardRepository.class)
@ActiveProfiles("test")
class ClearanceCardRepositoryTest {
    @Autowired
    private ClearanceCardRepository repository;
    private static ClearanceCardRequest sampleRequest() {
        return new ClearanceCardRequest(
                "Middletown",
                LocalTime.of(10, 30),
                LocalDate.of(2026, 3, 24),
                "Extra 101 East",
                3,
                "Extra 202 West",
                "J. Smith",
                List.of("12", "45", "87"));
    }
    @Test
    void savePersistsAllParentFields() {
        ClearanceCard saved = repository.save(sampleRequest());
        assertThat(saved.id()).isNotNull();
        assertThat(saved.station()).isEqualTo("Middletown");
        assertThat(saved.issuedTime()).isEqualTo(LocalTime.of(10, 30));
        assertThat(saved.issuedDate()).isEqualTo(LocalDate.of(2026, 3, 24));
        assertThat(saved.trainNumber()).isEqualTo("Extra 101 East");
        assertThat(saved.orderCount()).isEqualTo(3);
        assertThat(saved.signalStopFor()).isEqualTo("Extra 202 West");
        assertThat(saved.operatorName()).isEqualTo("J. Smith");
    }
    @Test
    void savePersistsOrderNumbersInSequence() {
        ClearanceCard saved = repository.save(sampleRequest());
        assertThat(saved.orderNumbers()).containsExactly("12", "45", "87");
    }
    @Test
    void findByIdRoundTripsFullCard() {
        ClearanceCard saved = repository.save(sampleRequest());
        Optional<ClearanceCard> found = repository.findById(saved.id());
        assertThat(found).isPresent();
        assertThat(found.get().trainNumber()).isEqualTo("Extra 101 East");
        assertThat(found.get().orderNumbers()).containsExactly("12", "45", "87");
    }
    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertThat(repository.findById(999L)).isEmpty();
    }
    @Test
    void findByDateFiltersToMatchingDateOnly() {
        repository.save(sampleRequest());
        ClearanceCardRequest nextDay = new ClearanceCardRequest(
                "Uptown", LocalTime.of(14, 0), LocalDate.of(2026, 3, 25),
                "Extra 303 West", 1, "Extra 404 East", "D. Jones", List.of("200"));
        repository.save(nextDay);
        List<ClearanceCard> march24 = repository.findByDate(LocalDate.of(2026, 3, 24));
        assertThat(march24).hasSize(1);
        assertThat(march24.get(0).station()).isEqualTo("Middletown");
    }
    @Test
    void findAllReturnsAllSavedCards() {
        repository.save(sampleRequest());
        repository.save(sampleRequest());
        assertThat(repository.findAll()).hasSizeGreaterThanOrEqualTo(2);
    }
    @Test
    void findByDateIncludesOrderNumbers() {
        repository.save(sampleRequest());
        List<ClearanceCard> results = repository.findByDate(LocalDate.of(2026, 3, 24));
        assertThat(results.get(0).orderNumbers()).containsExactly("12", "45", "87");
    }
    @Test
    void saveWithNoOrderNumbersStoresEmptyList() {
        ClearanceCardRequest req = new ClearanceCardRequest(
                "Downtown", LocalTime.of(8, 0), LocalDate.of(2026, 3, 24),
                "Extra 505 East", 0, null, "R. Brown", List.of());
        ClearanceCard saved = repository.save(req);
        assertThat(saved.orderNumbers()).isEmpty();
    }
}
