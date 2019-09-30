package id.jred;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class App {
    private final CmdLineArgs cmdLineArgs;

    public static void main(String[] args) {
        var cmdLineArgs = new CmdLineArgs(args);
        if (cmdLineArgs.isHelp() || cmdLineArgs.getPositional().isEmpty()) {
            cmdLineArgs.printHelp();
            return;
        }

        Util.createWorkDir();
        var app = new App(cmdLineArgs);

        var posArgs = cmdLineArgs.getPositional();
        switch (posArgs.get(0)) {
        case "client": {
            if (posArgs.size() < 2) {
                System.err.println("Client command missing");
                System.exit(1);
            }
            app.clientCommand(posArgs.get(1), posArgs.subList(2, posArgs.size()));
            break;
        }
        case "server": {
            if (posArgs.size() < 2) {
                System.err.println("Server command missing");
                System.exit(1);
            }
            app.serverCommand(posArgs.get(1), posArgs.subList(2, posArgs.size()));
            break;
        }
        default: {
            System.err.println("Invalid mode: " + posArgs.get(0));
            System.exit(1);
        }
        }
    }

    private App(CmdLineArgs cmdLineArgs) {
        this.cmdLineArgs = cmdLineArgs;
    }

    private void serverCommand(String command, List<String> args) {
        var config = ServerConfig.create(cmdLineArgs);
        switch (command) {
        case "start": {
            if (Util.isPidFileExists()) {
                var pid = Util.readPidFile();
                System.err.println("Server is running, pid=" + pid);
                System.exit(1);
            }
            Util.createPidFile();
            try {
                RequestHandler.start(config);
            } catch (RuntimeException ex) {
                Util.deletePidFile();
                throw ex;
            }
            break;
        }
        case "stop": {
            if (Util.isPidFileExists()) {
                var pid = Util.readPidFile();
                Util.deletePidFile();
                ProcessHandle.of(pid).ifPresent(handle -> {
                    try {
                        handle.onExit().get(3, TimeUnit.SECONDS);
                        System.out.println("Server " + pid + " stopped");
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
            }
            break;
        }
        default: {
            System.err.println("Invalid server command");
            System.exit(1);
        }
        }
    }

    private void clientCommand(String command, List<String> args) {
        var config = ClientConfig.create(cmdLineArgs);
        switch (command) {
        case "copy": {
            if (args.isEmpty()) {
                System.err.println("Missing list of files");
                System.exit(1);
            }
            var url = buildUrl(config, "/copy");
            var currentDir = Path.of(".").toAbsolutePath().normalize();
            for (var fileName : args) {
                var path = Path.of(fileName).toAbsolutePath();
                if (!path.startsWith(currentDir)) {
                    System.err.println(
                            "File must belong to current directory tree: " +
                            path.toString());
                    System.exit(1);
                }
                var copyReq = new Protocol.Copy();
                copyReq.setFileName(path.toString());
                copyReq.setRepo(initRepo(currentDir));
                try {
                    var connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", Protocol.MIME_JSON);
                    connection.setRequestProperty("Accept", Protocol.MIME_JSON);
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    try (var os = connection.getOutputStream()) {
                        os.write(Protocol.toWire(copyReq).getBytes(StandardCharsets.UTF_8));
                    }
                    try (var is = connection.getInputStream()) {
                        var status = Protocol.Status.fromWire(
                                new String(is.readAllBytes(), StandardCharsets.UTF_8));
                        if (status.getError() != 200) {
                            System.err.println(
                                    "Error copy " + fileName + ":\n" +
                                    "  " + status.getError() + ": " + status.getMessage());
                        }
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            break;
        }
        case "diff": {
            throw new RuntimeException("not implemented");
        }
        default: {
            System.err.println("Invalid client command");
            System.exit(1);
        }
        }
    }

    private static Protocol.Repo initRepo(Path absCurrDir) {
        var repo = new Protocol.Repo();
        repo.setName(absCurrDir.getFileName().toString());
        repo.setRevision("1200");
        return repo;
    }

    private static URL buildUrl(ClientConfig config, String path) {
        try {
            return new URI(
                    "http",
                    null,
                    config.getHost(),
                    config.getPort(),
                    path,
                    null,
                    null).toURL();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
