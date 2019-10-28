package id.jred;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class JsonTarget {
    private String path;
    private String vcs = "git";

    public void setPath(String path) {
        this.path = path;
    }

    @JsonProperty("path")
    public String getPath() {
        return path;
    }

    @JsonProperty("vcs")
    public String getVCS() {
        return vcs;
    }
}
