package org.trainbeans.clearancecard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.trainbeans.clearancecard.model.ClearanceCard;
import org.trainbeans.clearancecard.model.ClearanceCardRequest;
import org.trainbeans.clearancecard.service.ClearanceCardService;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClearanceCardPersistenceRestartTest {

    private static final String H2_CONNECTION_OPTIONS = ";DB_CLOSE_ON_EXIT=FALSE;INIT=RUNSCRIPT FROM 'classpath:schema.sql'";

    @TempDir
    Path tempDir;

    @Test
    void fileBackedDatabaseSurvivesApplicationRestart() {
        String dbPath = tempDir.resolve("clearancecard").toAbsolutePath().toString();
        String url = "jdbc:h2:file:" + dbPath + H2_CONNECTION_OPTIONS;

        try (ConfigurableApplicationContext first = runWith(url)) {
            ClearanceCardService service = first.getBean(ClearanceCardService.class);
            ClearanceCard saved = service.create(new ClearanceCardRequest(
                    "Middletown",
                    LocalTime.of(10, 30),
                    LocalDate.of(2026, 3, 24),
                    "Extra 101 East",
                    3,
                    "Extra 202 West",
                    "J. Smith",
                    List.of("12", "45", "87")));
            assertThat(saved.id()).isNotNull();
        }

        try (ConfigurableApplicationContext second = runWith(url)) {
            ClearanceCardService service = second.getBean(ClearanceCardService.class);
            List<ClearanceCard> cards = service.findAll(null);
            assertThat(cards).hasSize(1);
            assertThat(cards.get(0).trainNumber()).isEqualTo("Extra 101 East");
            assertThat(cards.get(0).orderNumbers()).containsExactly("12", "45", "87");
        }
    }

    private ConfigurableApplicationContext runWith(String url) {
        return new SpringApplicationBuilder(ClearanceCardApplication.class)
                .properties("spring.main.web-application-type=none")
                .run(
                        "--spring.datasource.url=" + url,
                        "--app.railroad.name=Test Railroad");
    }
}
