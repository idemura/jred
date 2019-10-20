package id.jred;

public final class ClientConfig extends Config {
    public ClientConfig() {}

    public static ClientConfig create(CmdLineArgs cmdLineArgs) {
        var cfg = create("client.config", ClientConfig.class);
        cfg.applyCmdLineArgs(cmdLineArgs);
        return cfg;
    }
}
