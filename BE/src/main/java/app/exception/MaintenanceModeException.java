package app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class MaintenanceModeException extends RuntimeException {
    public MaintenanceModeException(String message) {
        super(message);
    }
}
