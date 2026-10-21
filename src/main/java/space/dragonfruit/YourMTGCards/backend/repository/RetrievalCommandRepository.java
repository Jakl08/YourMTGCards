package space.dragonfruit.YourMTGCards.backend.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import space.dragonfruit.YourMTGCards.backend.model.CommandStatus;
import space.dragonfruit.YourMTGCards.backend.model.RetrievalCommand;

/** Datenzugriff auf die persistente Warteschlange für die Regal-Hardware. */
public interface RetrievalCommandRepository extends JpaRepository<RetrievalCommand, UUID> {

	List<RetrievalCommand> findAllByStatusOrderByCreatedAtAsc(CommandStatus status);
}
