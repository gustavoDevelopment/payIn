package com.payin.domain.exceptions;

/**
 * Excepción de dominio que representa un error de negocio.
 * Incluye un código de error para facilitar la identificación del tipo de error.
 */
public class DomainException extends RuntimeException {
    
    private final String errorCode;
    
    public DomainException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public DomainException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
