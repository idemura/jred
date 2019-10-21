package id.jred;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class Repo {
    private String path;
    private String vcs = "git";

    @JsonProperty("path")
    public String getPath() {
        return path;
    }

    @JsonProperty("vcs")
    public String getVCS() {
        return vcs;
    }
}
