package dev.marzetti.pricemonitor.web;

import dev.marzetti.pricemonitor.ingestion.ListingNotFoundException;
import dev.marzetti.pricemonitor.ingestion.SourceUnavailableException;
import dev.marzetti.pricemonitor.ingestion.UnsupportedStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las excepciones de los módulos a respuestas RFC 9457 (Problem Details). */
@RestControllerAdvice
class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(UnsupportedStoreException.class)
    ProblemDetail unsupportedStore(UnsupportedStoreException e) {
        return problem(HttpStatusCode.valueOf(422), "Tienda no soportada", e.getMessage());
    }

    @ExceptionHandler(ListingNotFoundException.class)
    ProblemDetail listingNotFound(ListingNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Producto no encontrado", e.getMessage());
    }

    @ExceptionHandler(SourceUnavailableException.class)
    ProblemDetail sourceUnavailable(SourceUnavailableException e) {
        log.warn("Fuente no disponible: {}", e.getMessage());
        return problem(HttpStatus.BAD_GATEWAY, "La tienda no respondió correctamente", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, "Solicitud inválida", e.getMessage());
    }

    private static ProblemDetail problem(HttpStatusCode status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
