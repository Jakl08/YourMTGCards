package space.dragonfruit.YourMTGCards;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Startpunkt der Spring-Boot-Anwendung: aktiviert Autokonfiguration und scannt die Backend-Pakete.
 */
@SpringBootApplication
public class YourMtgCardsApplication {

	/** Startet den eingebetteten Webserver und die YourMTGCards-Anwendung. */
	public static void main(String[] args) {
		SpringApplication.run(YourMtgCardsApplication.class, args);
	}

}
