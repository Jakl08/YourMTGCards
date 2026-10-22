package space.dragonfruit.YourMTGCards.backend.datenbank;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Datenzugriff auf einmalig gespeicherte Scryfall-Ausgaben. */
public interface CardMetadataRepository extends JpaRepository<CardMetadata, UUID> {
}
