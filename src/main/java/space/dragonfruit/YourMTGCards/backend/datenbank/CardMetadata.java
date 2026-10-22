package space.dragonfruit.YourMTGCards.backend.datenbank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Einmal gespeicherte, abstrakte Kartendaten; die Scryfall-ID verhindert Metadaten-Duplikate. */
@Entity
@Table(name = "card_metadata")
public class CardMetadata {

	/** Eindeutige ID derselben Kartenausgabe bei Scryfall. */
	@Id
	private UUID id;

	@Column(nullable = false, length = 300)
	private String name;

	@Column(name = "set_code", nullable = false, length = 20)
	private String setCode;

	@Column(nullable = false, length = 10)
	private String language;

	@Column(name = "image_url", length = 2000)
	private String imageUrl;

	protected CardMetadata() {
		// JPA benötigt einen parameterlosen Konstruktor zum Laden gespeicherter Entitäten.
	}

	public CardMetadata(UUID id, String name, String setCode, String language, String imageUrl) {
		this.id = id;
		this.name = name;
		this.setCode = setCode;
		this.language = language;
		this.imageUrl = imageUrl;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getSetCode() {
		return setCode;
	}

	public String getLanguage() {
		return language;
	}

	public String getImageUrl() {
		return imageUrl;
	}
}
