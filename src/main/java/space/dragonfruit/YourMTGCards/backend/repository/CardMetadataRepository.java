package space.dragonfruit.YourMTGCards.backend.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import space.dragonfruit.YourMTGCards.backend.model.CardMetadata;

/** Datenzugriff auf einmalig gespeicherte Scryfall-Ausgaben. */
public interface CardMetadataRepository extends JpaRepository<CardMetadata, UUID> {
}
