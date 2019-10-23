package id.jred;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class JsonRepo {
    private String name;
    private String revision;

    public JsonRepo() {}

    public JsonRepo(String name, String revision) {
        this.name = name;
        this.revision = revision;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("revision")
    public String getRevision() {
        return revision;
    }
}
