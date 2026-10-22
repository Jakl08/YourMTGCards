package space.dragonfruit.YourMTGCards.backend.web_api;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

/** Wandelt bekannte Fach-, Validierungs- und Upstream-Fehler in ein einheitliches JSON-Format um. */
@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception) {
		return error(exception.getStatus(), exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.findFirst()
				.orElse("Die übermittelten Daten sind ungültig.");
		return error(HttpStatus.BAD_REQUEST, message);
	}

	@ExceptionHandler(RestClientException.class)
	public ResponseEntity<ApiErrorResponse> handleExternalApi(RestClientException exception) {
		return error(HttpStatus.BAD_GATEWAY, "Die Karten-API ist derzeit nicht erreichbar.");
	}

	private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
		return ResponseEntity.status(status)
				.body(new ApiErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message));
	}
}
