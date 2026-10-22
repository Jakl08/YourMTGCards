package space.dragonfruit.YourMTGCards.backend.web_api;

import org.springframework.http.HttpStatus;

/** Fachlicher HTTP-Fehler, dessen Statuscode kontrolliert an den API-Aufrufer weitergegeben wird. */
public class ApiException extends RuntimeException {

	private final HttpStatus status;

	public ApiException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}
}
