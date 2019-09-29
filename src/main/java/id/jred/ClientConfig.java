package id.jred;

public final class ClientConfig extends Config {
    public ClientConfig() {}

    public static ClientConfig create(CmdLineArgs cmdLineArgs) {
        var cfg = create(".redc", ClientConfig.class);
        cfg.useCommandLineArgs(cmdLineArgs);
        return cfg;
    }
}
