package id.jred;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public final class ServerConfig extends Config {
    public static final class Repo {
        private String path;
        private String type;

        @JsonProperty("path")
        public String getPath() {
            return path;
        }

        @JsonProperty("type")
        public String getType() {
            return type;
        }
    }

    private Map<String, Repo> repo = new HashMap<>();

    public ServerConfig() {}

    public static ServerConfig create(CmdLineArgs cmdLineArgs) {
        var cfg = create("server.config", ServerConfig.class);
        cfg.useCommandLineArgs(cmdLineArgs);
        return cfg;
    }

    @JsonProperty("repo_map")
    public Map<String, Repo> getRepo() {
        return repo;
    }
}
