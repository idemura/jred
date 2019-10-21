package id.jred;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public final class App {
    private final CmdLineArgs cmdLineArgs;

    public static void main(String[] args) {
        var cmdLineArgs = new CmdLineArgs(args);
        if (cmdLineArgs.isHelp() || cmdLineArgs.getPositional().isEmpty()) {
            cmdLineArgs.printHelp();
            return;
        }

        try {
            var app = new App(cmdLineArgs);
            if (Dir.createHome()) {
                app.update();
            }

            var posArgs = cmdLineArgs.getPositional();
            switch (posArgs.get(0)) {
            case "server":
                if (posArgs.size() != 2) {
                    throw new AppException("Invalid command format");
                }
                app.serverCommand(posArgs.get(1));
                break;

            case "submit":
                if (posArgs.size() != 1) {
                    throw new AppException("Invalid command format");
                }
                app.submit(cmdLineArgs.getVCS());
                break;

            case "update":
                if (posArgs.size() != 1) {
                    throw new AppException("Invalid command format");
                }
                app.update();
                break;

            default:
                throw new AppException("Invalid mode: " + posArgs.get(0));
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
    }

    private App(CmdLineArgs cmdLineArgs) {
        this.cmdLineArgs = cmdLineArgs;
    }

    private void serverCommand(String command) throws IOException {
        switch (command) {
        case "start": {
            Optional<ProcessHandle> ph = PidFile.read();
            if (ph.filter(ProcessHandle::isAlive).isPresent()) {
                System.out.println("Server is running, pid=" + ph.get().pid());
                break;
            }
            PidFile.create();
            try {
                Map<String, Repo> repoMap = Json.mapper.readValue(
                        new File(Dir.getHome(), "repo_map"),
                        new TypeReference<HashMap<String, Repo>>() {});
                Handlers.start(cmdLineArgs.getHost(), cmdLineArgs.getPort(), repoMap);
            } catch (Exception ex) {
                PidFile.delete();
                throw ex;
            }
            break;
        }
        case "stop": {
            Optional<ProcessHandle> ph = PidFile.read();
            if (ph.isPresent()) {
                PidFile.delete();
                Future<ProcessHandle> future = ph.get().onExit();
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException ex) {
                    throw new AppException(ex.getMessage());
                }
                System.out.println("Server process " + ph.get().pid() + " stopped");
            }
            break;
        }
        default:
            throw new AppException("Invalid server command: " + command);
        }
    }

    // We have vcs argument because we want to detect it actually.
    private void submit(String vcs) throws IOException {
        var currentDir = Dir.getCurrent();
        var repo = initRepo(currentDir, vcs);
        var status = Script.run(vcs + "/status").split("\n");

        var untrackedFiles = new ArrayList<File>();
        for (var s : status) {
            if ("??".equals(s.substring(0, 2))) {
                var path = new File(s.substring(3)).getAbsoluteFile().getCanonicalFile();
                if (path.isDirectory()) {
                    var stack = new ArrayList<File>();
                    stack.add(path);
                     while (!stack.isEmpty()) {
                        var dir = stack.remove(stack.size() - 1);
                        for (var f : dir.listFiles()) {
                            if (f.isDirectory()) {
                                stack.add(f);
                            } else {
                                untrackedFiles.add(f);
                            }
                        }
                    }
                } else {
                    untrackedFiles.add(path);
                }
            }
        }

        var copyRequest = new Protocol.Copy();
        copyRequest.setRepo(repo);
        for (var f : untrackedFiles) {
            copyRequest.setFileName(currentDir.toPath().relativize(f.toPath()).toString());
            try (var stream = new FileInputStream(f)) {
                copyRequest.setData(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            }
            post(buildUrl("/copy"), copyRequest);
        }

        var diffRequest = new Protocol.Diff();
        diffRequest.setRepo(repo);
        diffRequest.setDiff(Script.run(vcs + "/diff"));
        post(buildUrl("/diff"), diffRequest);
    }

    private void update() throws IOException {
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
        System.out.println("Scripts updated");
    }

    private static Protocol.Repo initRepo(File absCurrDir, String vcs) {
        var repo = new Protocol.Repo();
        repo.setName(absCurrDir.getName());
        repo.setRevision(Script.run(vcs + "/revision").trim());
        return repo;
    }

    private URL buildUrl(String path) throws IOException {
        try {
            return new URI(
                    "http",
                    null,
                    cmdLineArgs.getHost(),
                    cmdLineArgs.getPort(),
                    path,
                    null,
                    null).toURL();
        } catch (URISyntaxException ex) {
            throw new IOException("Invalid URI: " + ex.getMessage());
        }
    }

    private static byte[] post(URL url, Object request) throws IOException {
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
                return is.readAllBytes();
            }
        } catch (IOException ex) {
            AppException appEx;
            try (var es = connection.getErrorStream()) {
                String msg;
                if (es != null) {
                    msg = Json.read(Protocol.Error.class, es).getErrorMsg();
                } else {
                    msg = "HTTP error code: " + connection.getResponseCode();
                }
                appEx = new AppException(msg);
            }
            throw appEx;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static final String[] vcsActions = {"apply", "diff", "status", "revision"};

    private static void copyOpFiles(ClassLoader cl, String name) throws IOException {
        var destPath = new File(Dir.getHome(), name);
        destPath.mkdir();
        var prefix = "source_control/" + name + "/";
        for (var a : vcsActions) {
            var qualifiedName = prefix + a;
            try (var is = cl.getResourceAsStream(qualifiedName)) {
                if (is == null) {
                    throw new IOException("Resource not found: " + qualifiedName);
                }
                try (var os = new FileOutputStream(new File(destPath, a))) {
                    os.write(is.readAllBytes());
                }
            }
        }
    }
}
