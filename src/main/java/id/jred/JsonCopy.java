package id.jred;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class JsonCopy {
    private JsonRepo repo;
    private String file;
    private String data;

    public JsonCopy() {}

    public JsonCopy(JsonRepo repo, String file, String data) {
        this.repo = repo;
        this.file = file;
        this.data = data;
    }

    @JsonProperty("repo")
    public JsonRepo getRepo() {
        return repo;
    }

    @JsonProperty("file")
    public String getFile() {
        return file;
    }

    @JsonProperty("data")
    public String getData() {
        return data;
    }
}
