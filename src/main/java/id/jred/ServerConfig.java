package id.jred;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class ServerConfig extends Config {
    private List<String> repo = new ArrayList<>();

    ServerConfig() {}

    public static ServerConfig create(CmdLineArgs cmdLineArgs) {
        var cfg = create("server.config", ServerConfig.class);
        cfg.useCommandLineArgs(cmdLineArgs);
        return cfg;
    }

    @JsonProperty
    public List<String> getRepo() {
        return repo;
    }

    public HashMap<String, Path> createRepoNameMap() {
        var repoMap = new HashMap<String, Path>();
        for (var repoPath : repo) {
            var p = Path.of(repoPath).toAbsolutePath();
            var key = p.getFileName().toString();
            if (repoMap.containsKey(key)) {
                throw new RuntimeException("Repository duplicate: " + key);
            }
            repoMap.put(key, p);
        }
        return repoMap;
    }
}
