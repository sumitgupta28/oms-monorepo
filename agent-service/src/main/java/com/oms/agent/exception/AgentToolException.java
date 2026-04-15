package com.oms.agent.exception;

import lombok.Getter;

/**
 * Exception thrown when an AI tool fails to execute.
 * Used for failures in downstream service calls (order, payment, inventory, product).
 */
@Getter
public class AgentToolException extends RuntimeException {

    private final String toolName;

    public AgentToolException(String toolName, String message) {
        super("%s failed: %s".formatted(toolName, message));
        this.toolName = toolName;
    }

    public AgentToolException(String toolName, String message, Throwable cause) {
        super("%s failed: %s".formatted(toolName, message), cause);
        this.toolName = toolName;
    }

}