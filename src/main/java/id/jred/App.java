package id.jred;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
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

        try {
            WorkDir.create();
            var app = new App(cmdLineArgs);

            var posArgs = cmdLineArgs.getPositional();
            switch (posArgs.get(0)) {
            case "client":
                if (posArgs.size() < 2) {
                    throw new AppException("Client command invalid");
                }
                app.clientCommand(posArgs.get(1), posArgs.subList(2, posArgs.size()));
                break;

            case "server":
                if (posArgs.size() != 2) {
                    throw new AppException("Server command invalid");
                }
                app.serverCommand(posArgs.get(1));
                break;

            case "update":
                if (posArgs.size() != 1) {
                    throw new AppException("Update command invalid");
                }
                app.update();
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

    private void update() {
        try {
            var cl = App.class.getClassLoader();
            String registry;
            try (var registryStream = cl.getResourceAsStream("source_control/registry")) {
                if (registryStream != null) {
                    registry = new String(
                            registryStream.readAllBytes(),
                            StandardCharsets.UTF_8);
                } else {
                    throw new AppException("registry not found");
                }
            }
            for (var name : registry.split("\n")) {
                name = name.trim();
                if (!name.isEmpty()) {
                    copyOpFiles(cl, name);
                }
            }
            System.out.println("Update done");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
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
            connection.setRequestProperty("Content-Type", MimeType.JSON);
            connection.setRequestProperty("Accept", MimeType.JSON);
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

    private static void copyOpFiles(ClassLoader cl, String name)
            throws IOException
    {
        var destPath = WorkDir.getPath().resolve(name);
        try {
            Files.createDirectory(destPath);
        } catch (FileAlreadyExistsException ex) {
            // Ignore
        }
        var prefix = "source_control/" + name + "/";
        for (var a : new String[]{"apply", "diff", "revision"}) {
            var qualifiedName = prefix + a;
            try (var is = cl.getResourceAsStream(qualifiedName)) {
                if (is == null) {
                    throw new IOException("Resource not found: " + qualifiedName);
                }
                try (var os = new FileOutputStream(destPath.resolve(a).toFile())) {
                    os.write(is.readAllBytes());
                }
            }
        }
    }
}
