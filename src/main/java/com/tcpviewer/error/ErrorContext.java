package com.tcpviewer.error;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Complete context information about an error, including classification, messages, and metadata.
 * This class encapsulates all information needed to properly handle and display an error.
 */
public class ErrorContext {
    private final Throwable throwable;
    private final ErrorCategory category;
    private final ErrorSeverity severity;
    private final String userMessage;
    private final String technicalDetails;
    private final LocalDateTime timestamp;
    private final Map<String, Object> additionalContext;

    private ErrorContext(Builder builder) {
        this.throwable = builder.throwable;
        this.category = builder.category;
        this.severity = builder.severity;
        this.userMessage = builder.userMessage;
        this.technicalDetails = builder.technicalDetails;
        this.timestamp = builder.timestamp;
        this.additionalContext = new HashMap<>(builder.additionalContext);
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public ErrorCategory getCategory() {
        return category;
    }

    public ErrorSeverity getSeverity() {
        return severity;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getTechnicalDetails() {
        return technicalDetails;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getAdditionalContext() {
        return new HashMap<>(additionalContext);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Throwable throwable;
        private ErrorCategory category;
        private ErrorSeverity severity;
        private String userMessage;
        private String technicalDetails;
        private LocalDateTime timestamp = LocalDateTime.now();
        private Map<String, Object> additionalContext = new HashMap<>();

        public Builder throwable(Throwable throwable) {
            this.throwable = throwable;
            return this;
        }

        public Builder category(ErrorCategory category) {
            this.category = category;
            return this;
        }

        public Builder severity(ErrorSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder userMessage(String userMessage) {
            this.userMessage = userMessage;
            return this;
        }

        public Builder technicalDetails(String technicalDetails) {
            this.technicalDetails = technicalDetails;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder addContext(String key, Object value) {
            this.additionalContext.put(key, value);
            return this;
        }

        public Builder additionalContext(Map<String, Object> additionalContext) {
            this.additionalContext = new HashMap<>(additionalContext);
            return this;
        }

        public ErrorContext build() {
            if (throwable == null) {
                throw new IllegalStateException("throwable is required");
            }
            if (category == null) {
                throw new IllegalStateException("category is required");
            }
            if (severity == null) {
                throw new IllegalStateException("severity is required");
            }
            if (userMessage == null || userMessage.trim().isEmpty()) {
                throw new IllegalStateException("userMessage is required");
            }
            if (technicalDetails == null || technicalDetails.trim().isEmpty()) {
                throw new IllegalStateException("technicalDetails is required");
            }
            return new ErrorContext(this);
        }
    }
}
