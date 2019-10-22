package id.jred;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class Repo {
    private String path;

    public void setPath(String path) {
        this.path = path;
    }

    @JsonProperty("path")
    public String getPath() {
        return path;
    }
}
