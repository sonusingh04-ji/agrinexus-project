package com.agrinexus.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /*
     * =========================================================
     * Illegal / validation-type application errors
     * =========================================================
     *
     * Your current services use RuntimeException for validation
     * and missing-resource cases.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request
    ) {

        HttpStatus status = determineStatus(ex.getMessage());

        return buildResponse(
                status,
                ex.getMessage(),
                request.getRequestURI()
        );
    }


    /*
     * =========================================================
     * Bean Validation errors
     * =========================================================
     *
     * Handles:
     *
     * @Valid
     * @NotNull
     * @NotBlank
     * @Size
     * etc.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        Map<String, String> validationErrors =
                new LinkedHashMap<>();


        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        validationErrors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );


        Map<String, Object> body =
                new LinkedHashMap<>();


        body.put(
                "timestamp",
                LocalDateTime.now()
        );


        body.put(
                "status",
                HttpStatus.BAD_REQUEST.value()
        );


        body.put(
                "error",
                HttpStatus.BAD_REQUEST.getReasonPhrase()
        );


        body.put(
                "message",
                "Request validation failed"
        );


        body.put(
                "path",
                request.getRequestURI()
        );


        body.put(
                "validationErrors",
                validationErrors
        );


        return ResponseEntity
                .status(
                        HttpStatus.BAD_REQUEST
                )
                .body(
                        body
                );
    }


    /*
     * =========================================================
     * Malformed / unsupported request bodies
     * =========================================================
     */
    @ExceptionHandler(
            org.springframework.http.converter.HttpMessageNotReadableException.class
    )
    public ResponseEntity<Map<String, Object>>
    handleMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Request body is invalid or malformed.",
                request.getRequestURI()
        );
    }


    /*
     * =========================================================
     * Wrong path / missing endpoint
     * =========================================================
     *
     * Example:
     *
     * /api/nutrient-balance
     *
     * when the actual endpoint is:
     *
     * /api/nutrient-balances
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>>
    handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "API endpoint not found.",
                request.getRequestURI()
        );
    }


    /*
     * =========================================================
     * Illegal argument errors
     * =========================================================
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>>
    handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI()
        );
    }


    /*
     * =========================================================
     * Fallback
     * =========================================================
     *
     * Prevents raw stack traces from being returned to clients.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>>
    handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred.",
                request.getRequestURI()
        );
    }


    /*
     * =========================================================
     * Determine application status
     * =========================================================
     */
    private HttpStatus determineStatus(
            String message
    ) {

        if (
                message == null
                        || message.isBlank()
        ) {

            return HttpStatus.BAD_REQUEST;
        }


        String normalized =
                message.toLowerCase();


        /*
         * Missing resources.
         */
        if (
                normalized.contains("not found")
                        || normalized.contains("does not exist")
                        || normalized.contains("could not be found")
        ) {

            return HttpStatus.NOT_FOUND;
        }


        /*
         * Invalid input / validation.
         */
        if (
                normalized.contains("required")
                        || normalized.contains("cannot be")
                        || normalized.contains("must be")
                        || normalized.contains("invalid")
                        || normalized.contains("not recognized")
                        || normalized.contains("cannot exceed")
                        || normalized.contains("already")
                        || normalized.contains("unavailable")
                        || normalized.contains("only cooperative members")
                        || normalized.contains("does not belong")
        ) {

            return HttpStatus.BAD_REQUEST;
        }


        /*
         * Safe default for application RuntimeExceptions.
         */
        return HttpStatus.BAD_REQUEST;
    }


    /*
     * =========================================================
     * Standard response builder
     * =========================================================
     */
    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String message,
            String path
    ) {

        Map<String, Object> body =
                new LinkedHashMap<>();


        body.put(
                "timestamp",
                LocalDateTime.now()
        );


        body.put(
                "status",
                status.value()
        );


        body.put(
                "error",
                status.getReasonPhrase()
        );


        body.put(
                "message",
                message != null
                        ? message
                        : "An error occurred."
        );


        body.put(
                "path",
                path
        );


        return ResponseEntity
                .status(status)
                .body(body);
    }
}