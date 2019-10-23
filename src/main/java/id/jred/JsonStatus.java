package id.jred;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class JsonStatus {
    private String message;

    public JsonStatus() {
        this("");
    }

    public JsonStatus(String message) {
        this.message = message;
    }

    @JsonProperty("message")
    public String getMessage() {
        return message;
    }
}
