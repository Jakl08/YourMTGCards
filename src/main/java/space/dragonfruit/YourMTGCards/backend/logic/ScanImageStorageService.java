package space.dragonfruit.YourMTGCards.backend.logic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import space.dragonfruit.YourMTGCards.backend.web_api.ApiException;
import org.springframework.http.HttpStatus;

/** Speichert geprüfte Scanbilder außerhalb des öffentlichen Verzeichnisses mit servergenerierten Dateinamen. */
@Service
public class ScanImageStorageService {

	private final Path imageDirectory;

	public ScanImageStorageService(@Value("${yourmtgcards.image-directory}") String imageDirectory) {
		this.imageDirectory = Path.of(imageDirectory).toAbsolutePath().normalize();
	}

	/** Validiert Bildtyp und Inhalt und liefert den internen Pfad für die Datenbank zurück. */
	public String store(UUID cardId, MultipartFile image) {
		String contentType = image.getContentType();
		String extension;
		if (MediaType.IMAGE_JPEG_VALUE.equals(contentType)) {
			extension = ".jpg";
		} else if (MediaType.IMAGE_PNG_VALUE.equals(contentType)) {
			extension = ".png";
		} else {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Das Scanbild muss JPEG oder PNG sein.");
		}
		if (image.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Das Scanbild darf nicht leer sein.");
		}

		try {
			Files.createDirectories(imageDirectory);
			Path destination = imageDirectory.resolve(cardId + extension).normalize();
			if (!destination.getParent().equals(imageDirectory)) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Ungültiger Bildname.");
			}
			Files.copy(image.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
			return destination.toString();
		} catch (IOException exception) {
			throw new IllegalStateException("Das Scanbild konnte nicht dauerhaft gespeichert werden.", exception);
		}
	}

	/** Öffnet einen zuvor gespeicherten Scan zur kontrollierten Auslieferung an die Weboberfläche. */
	public byte[] read(String storedPath) {
		Path path = Path.of(storedPath).toAbsolutePath().normalize();
		if (!path.getParent().equals(imageDirectory)) {
			throw new IllegalStateException("Der gespeicherte Bildpfad liegt außerhalb des Bildverzeichnisses.");
		}
		try {
			return Files.readAllBytes(path);
		} catch (IOException exception) {
			throw new IllegalStateException("Das gespeicherte Scanbild kann nicht gelesen werden.", exception);
		}
	}
}
