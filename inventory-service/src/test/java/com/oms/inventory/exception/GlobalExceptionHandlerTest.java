package com.oms.inventory.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnProblemDetail422ForInventoryException() {
        var exception = new InventoryException("Product not found: prod-42");

        var result = handler.handle(exception);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(result.getTitle()).isEqualTo("Inventory Error");
        assertThat(result.getDetail()).isEqualTo("Product not found: prod-42");
    }

    @Test
    void shouldReturnProblemDetail422ForInventoryExceptionWithEmptyMessage() {
        var exception = new InventoryException("");

        var result = handler.handle(exception);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(result.getTitle()).isEqualTo("Inventory Error");
        assertThat(result.getDetail()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnProblemDetail422ForValidationError() throws Exception {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "setStockRequest");
        bindingResult.addError(new FieldError("setStockRequest", "productId", "Product ID is required"));
        bindingResult.addError(new FieldError("setStockRequest", "quantity", "Quantity cannot be negative"));
        var methodParam = new MethodParameter(
            this.getClass().getDeclaredMethod("stubMethod"), -1);
        var exception = new MethodArgumentNotValidException(methodParam, bindingResult);

        var result = handler.validationError(exception);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(result.getTitle()).isEqualTo("Validation Error");
        assertThat(result.getDetail()).isEqualTo("Validation failed");
        var errors = (List<String>) result.getProperties().get("errors");
        assertThat(errors).hasSize(2);
        assertThat(errors).contains(
            "productId: Product ID is required",
            "quantity: Quantity cannot be negative"
        );
    }

    @Test
    void shouldReturnProblemDetail500ForGenericException() {
        var exception = new RuntimeException("Something went wrong");

        var result = handler.handleGeneric(exception);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getTitle()).isEqualTo("Internal Server Error");
        assertThat(result.getDetail()).isEqualTo("An unexpected error occurred");
    }

    @Test
    void shouldReturnProblemDetail500ForNullPointerException() {
        var exception = new NullPointerException();

        var result = handler.handleGeneric(exception);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(result.getTitle()).isEqualTo("Internal Server Error");
    }

    // Stub method needed by MethodParameter constructor in the validation test
    @SuppressWarnings("unused")
    private void stubMethod() {}
}
