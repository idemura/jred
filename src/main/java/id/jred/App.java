package id.jred;

import java.io.FileInputStream;
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
        try {
            ClassLoader cl = App.class.getClassLoader();
            var enumerator = cl.getResources("/");
            while (enumerator.hasMoreElements()) {
                System.out.println(enumerator.nextElement());
            }
        } catch (Exception ex) {
            return;
        }

        var cmdLineArgs = new CmdLineArgs(args);
        if (cmdLineArgs.isHelp() || cmdLineArgs.getPositional().isEmpty()) {
            cmdLineArgs.printHelp();
            return;
        }

        try {
            WorkDir.create();
            var app = new App(cmdLineArgs);

            var posArgs = cmdLineArgs.getPositional();
            switch (posArgs.get(0)) {
            case "client":
                if (posArgs.size() < 2) {
                    throw new AppException("Client command missing");
                }
                app.clientCommand(posArgs.get(1), posArgs.subList(2, posArgs.size()));
                break;

            case "server":
                if (posArgs.size() != 2) {
                    throw new AppException("Server command missing");
                }
                app.serverCommand(posArgs.get(1));
                break;

            default:
                throw new AppException("Invalid mode: " + posArgs.get(0));
            }
        } catch (AppException ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        }
    }

    private App(CmdLineArgs cmdLineArgs) {
        this.cmdLineArgs = cmdLineArgs;
    }

    private void serverCommand(String command) {
        switch (command) {
        case "start":
            if (PidFile.exists(true /* checkAlive */)) {
                System.out.println("Server is running, pid=" + PidFile.read());
                break;
            }
            PidFile.create();
            try {
                RequestHandler.start(ServerConfig.create(cmdLineArgs));
            } catch (RuntimeException ex) {
                PidFile.delete();
                throw ex;
            }
            break;

        case "stop":
            if (PidFile.exists(false /* checkAlive */)) {
                var pid = PidFile.read();
                PidFile.delete();
                ProcessHandle.of(pid).ifPresent(handle -> {
                    try {
                        handle.onExit().get(3, TimeUnit.SECONDS);
                        System.out.println("Server process " + pid + " stopped");
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
            }
            break;

        default:
            throw new AppException("Invalid server command: " + command);
        }
    }

    private void clientCommand(String command, List<String> args) {
        switch (command) {
        case "copy":
            if (args.isEmpty()) {
                throw new AppException("Empty file list");
            }
            var url = buildUrl(ClientConfig.create(cmdLineArgs), "/copy");
            var currentDir = Path.of(".").toAbsolutePath().normalize();
            for (var fileName : args) {
                var path = Path.of(fileName).toAbsolutePath().normalize();
                if (!path.startsWith(currentDir)) {
                    throw new AppException(
                            "File must belong to repo directory tree: " +
                            path.toString());
                }
                var copyReq = new Protocol.Copy();
                copyReq.setFileName(currentDir.relativize(path).toString());
                copyReq.setRepo(initRepo(currentDir));
                try (var stream = new FileInputStream(path.toString())) {
                    copyReq.setData(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                var status = post(url, copyReq);
                if (status.getError() != 200) {
                    throw new AppException(
                            "Error copy " + fileName + ": " + status.getDetails());
                }
            }
            break;

        case "diff":
            break;

        default:
            throw new AppException("Invalid client command: " + command);
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

    private static Protocol.Status post(URL url, Object request) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", Protocol.MIME_JSON);
            connection.setRequestProperty("Accept", Protocol.MIME_JSON);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            try (var os = connection.getOutputStream()) {
                Json.write(request, os);
            }
            try (var is = connection.getInputStream()) {
                return Json.read(Protocol.Status.class, is);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
