package id.jred;

import java.util.concurrent.TimeUnit;

public final class App {
    public static void main(String[] args) {
        var cmdLineArgs = new CmdLineArgs(args);
        if (cmdLineArgs.isHelp()) {
            cmdLineArgs.printHelp();
            return;
        }
        Util.createWorkDir();
        var positional = cmdLineArgs.getPositional();
        if (positional.isEmpty() || positional.get(0).equals("client")) {
            var config = ClientConfig.create(cmdLineArgs);
            System.out.println(config.getHost());
            System.out.println(config.getPort());
            System.out.println("jred client");
        } else if (positional.get(0).equals("server")) {
            var config = ServerConfig.create(cmdLineArgs);
            Util.createPidFile();
            try {
                RequestHandler.start(config);
            } catch (RuntimeException ex) {
                Util.deletePidFile();
                throw ex;
            }
        } else if (positional.get(0).equals("stop")) {
            if (Util.isPidFileExists()) {
                var pid = Util.readPidFile();
                Util.deletePidFile();
                ProcessHandle.of(pid).ifPresent(handle -> {
                    try {
                        handle.onExit().get(3, TimeUnit.SECONDS);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
            }
        } else {
            System.err.println("Invalid mode: " + positional.get(0));
            System.exit(1);
        }
    }
}
