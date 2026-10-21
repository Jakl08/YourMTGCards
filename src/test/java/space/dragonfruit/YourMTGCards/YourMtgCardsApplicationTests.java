package space.dragonfruit.YourMTGCards;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import space.dragonfruit.YourMTGCards.backend.model.PhysicalCard;
import space.dragonfruit.YourMTGCards.backend.repository.PhysicalCardRepository;

/** Prüft den Anwendungsstart und die Speicherung einzelner physischer Scans in der Testdatenbank. */
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:yourmtgcards;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class YourMtgCardsApplicationTests {

	@Autowired
	private PhysicalCardRepository physicalCardRepository;

	/** Der leere Test bestätigt, dass Controller, Services und JPA gemeinsam starten. */
	@Test
	void contextLoads() {
	}

	/** Ein neu eingescannter Datensatz startet ohne Kartenzuordnung im Status PENDING. */
	@Test
	void newScanIsStoredAsPending() {
		PhysicalCard card = new PhysicalCard(
				java.util.UUID.randomUUID(),
				"R1",
				"B2",
				"Black Lotus",
				"/tmp/scan.jpg");

		PhysicalCard savedCard = physicalCardRepository.saveAndFlush(card);

		org.junit.jupiter.api.Assertions.assertEquals(
				space.dragonfruit.YourMTGCards.backend.model.CardStatus.PENDING,
				savedCard.getStatus());
		org.junit.jupiter.api.Assertions.assertTrue(physicalCardRepository.existsById(savedCard.getId()));
	}
}
