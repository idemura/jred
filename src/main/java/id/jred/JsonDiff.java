package id.jred;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class JsonDiff {
    private JsonRepo repo;
    private String diff;

    public JsonDiff() {}

    public JsonDiff(JsonRepo repo, String diff) {
        this.repo = repo;
        this.diff = diff;
    }

    @JsonProperty("repo")
    public JsonRepo getRepo() {
        return repo;
    }

    @JsonProperty("diff")
    public String getDiff() {
        return diff;
    }
}
