package space.dragonfruit.YourMTGCards.backend.datenbank;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Datenzugriff auf die persistente Warteschlange für die Regal-Hardware. */
public interface RetrievalCommandRepository extends JpaRepository<RetrievalCommand, UUID> {

	List<RetrievalCommand> findAllByStatusOrderByCreatedAtAsc(CommandStatus status);
}
