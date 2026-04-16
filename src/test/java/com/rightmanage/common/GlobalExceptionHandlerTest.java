package com.rightmanage.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    @Test
    void handleException_returnsFallbackMessageWhenExceptionMessageIsNull() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        Result<?> result = handler.handleException(new NullPointerException());

        assertEquals(Integer.valueOf(500), result.getCode());
        assertTrue(result.getMessage().contains("NullPointerException"));
    }
}
