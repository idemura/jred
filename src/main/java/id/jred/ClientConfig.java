package id.jred;

public final class ClientConfig extends Config {
    public ClientConfig() {}

    public static ClientConfig create(CmdLineArgs cmdLineArgs) {
        var cfg = create(".jred/client.config", ClientConfig.class);
        cfg.useCommandLineArgs(cmdLineArgs);
        return cfg;
    }
}
