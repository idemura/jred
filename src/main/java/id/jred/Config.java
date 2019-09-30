package id.jred;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;

import java.io.FileNotFoundException;

public abstract class Config {
    private static final ObjectMapper mapper = new ObjectMapper();

    private String host = "0.0.0.0";
    private int port = 8092;

    protected Config() {}

    protected void useCommandLineArgs(CmdLineArgs cmdLineArgs) {
        if (cmdLineArgs.getHost() != null) {
            this.host = cmdLineArgs.getHost();
        }
        if (cmdLineArgs.getPort() != 0) {
            this.port = cmdLineArgs.getPort();
        }
    }

    @JsonProperty
    String getHost() {
        return host;
    }

    @JsonProperty
    int getPort() {
        return port;
    }

    @NonNull
    protected static <T extends Config> T create(String configName, Class<T> type) {
        try {
            return mapper.readValue(Util.getWorkDir().resolve(configName).toFile(), type);
        } catch (FileNotFoundException ex) {
            return newInstance(type);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @NonNull
    public static <T extends Config> T newInstance(Class<T> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
