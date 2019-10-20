package id.jred;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public final class ServerConfig extends Config {
    public static final class Repo {
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

    private Map<String, Repo> repoMap = new HashMap<>();

    public ServerConfig() {}

    public static ServerConfig create(CmdLineArgs cmdLineArgs) {
        var cfg = create("server.config", ServerConfig.class);
        cfg.applyCmdLineArgs(cmdLineArgs);
        return cfg;
    }

    @JsonProperty("repo_map")
    public Map<String, Repo> getRepoMap() {
        return repoMap;
    }
}
