package id.jred;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class ServerConfig extends Config {
    private List<String> repo = new ArrayList<>();

    ServerConfig() {}

    public static ServerConfig create(CmdLineArgs cmdLineArgs) {
        var cfg = create(".reds", ServerConfig.class);
        cfg.useCommandLineArgs(cmdLineArgs);
        return cfg;
    }

    @JsonProperty
    public List<String> getRepo() {
        return repo;
    }
}
