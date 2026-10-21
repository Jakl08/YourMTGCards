package space.dragonfruit.YourMTGCards.backend.model;

/** Beschreibt, ob ein physisches Exemplar noch abgeglichen oder bereits ausgelagert wird. */
public enum CardStatus {
	PENDING,
	CONFIRMED,
	RETRIEVAL_REQUESTED,
	RETRIEVED
}
