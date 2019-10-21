package id.jred;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public final class App {
    public static void main(String[] args) {
        try {
            var app = new App(args);
            app.run();
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
    }

    @Parameters(commandDescription="Start server")
    private static final class CommandServer {
        @Parameter(
            names={"-s", "--stop"},
            description="Stop running server")
        private boolean stop = false;
    }

    @Parameters(commandDescription="Submit files and diff")
    private static final class CommandSubmit {
    }

    @Parameters(commandDescription="Update home dir files")
    private static final class CommandUpdate {
    }

    private final JCommander jcmd;
    private final CommandServer cmdServer = new CommandServer();
    private final CommandSubmit cmdSubmit = new CommandSubmit();
    private final CommandUpdate cmdUpdate = new CommandUpdate();

    @Parameter(names={"--help"}, help=true)
    private boolean help;

    @Parameter(
        names={"-h", "--host"},
        description="Host to connect/listen")
    private String host = "127.0.0.1";

    @Parameter(
        names={"-p", "--port"},
        description="Port to connect/listen")
    private int port = 8040;

    @Parameter(
        names={"--vcs"},
        description="Version control system")
    private String vcs = "git";

    private App(String[] args) {
        this.jcmd = JCommander.newBuilder()
            .addObject(this)
            .addCommand("server", cmdServer)
            .addCommand("submit", cmdSubmit)
            .addCommand("update", cmdUpdate)
            .programName("jred")
            .args(args)
            .build();
    }

    private void run() throws InterruptedException, IOException {
        if (help) {
            jcmd.usage();
            return;
        }

        if (Dir.createHome()) {
            update();
        }

        switch (jcmd.getParsedCommand()) {
        case "server":
            if (cmdServer.stop) {
                stopServer();
            } else {
                server();
            }
            break;
        case "submit":
            submit(vcs);
            break;
        case "update":
            update();
            break;
        }
    }

    private void server() throws IOException {
        Optional<ProcessHandle> ph = PidFile.read();
        if (ph.filter(ProcessHandle::isAlive).isPresent()) {
            System.out.println("Server is running, pid=" + ph.get().pid());
            return;
        }
        PidFile.create();
        try {
            Map<String, Repo> repoMap = Json.mapper.readValue(
                    new File(Dir.getHome(), "repo_map"),
                    new TypeReference<HashMap<String, Repo>>() {});
            Handlers.start(host, port, repoMap);
        } catch (IOException ex) {
            PidFile.delete();
            throw ex;
        }
    }

    private void stopServer() {
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
        } else {
            System.out.println("Server is not running");
        }
    }

    // We have vcs argument because we want to detect it actually.
    private void submit(String vcs)
            throws InterruptedException, IOException {
        var currentDir = Dir.getCurrent();
        var repo = new Protocol.Repo(
                currentDir.getName(),
                Script.run(vcs + "/revision").trim());

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

        for (var f : untrackedFiles) {
            String data;
            try (var stream = new FileInputStream(f)) {
                data = new String(stream.readAllBytes());
            }
            post(buildUrl("/copy"), new Protocol.Copy(
                    repo,
                    currentDir.toPath().relativize(f.toPath()).toString(),
                    data));
        }

        var diff = Script.run(vcs + "/diff");
        post(buildUrl("/diff"), new Protocol.Diff(repo, diff));
    }

    private void update() throws IOException {
        var cl = App.class.getClassLoader();
        String registry;
        try (var registryStream = cl.getResourceAsStream("source_control/registry")) {
            if (registryStream != null) {
                registry = new String(registryStream.readAllBytes());
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
        System.out.println("Home dir updated");
    }

    private URL buildUrl(String path) throws IOException {
        try {
            return new URI(
                    "http",
                    null,
                    host,
                    port,
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
                    msg = Json.read(Protocol.Error.class, es).getMessage();
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

    private static final String[] vcsFiles = {"apply", "diff", "status", "revision"};

    private static void copyOpFiles(ClassLoader cl, String name) throws IOException {
        var destPath = new File(Dir.getHome(), name);
        destPath.mkdir();
        var prefix = "source_control/" + name + "/";
        for (var a : vcsFiles) {
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
