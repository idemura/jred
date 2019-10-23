package id.jred;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

public final class App {
    private static final Logger LOG = LoggerFactory.getLogger("jred");

    public static void main(String[] args) {
        try {
            var app = new App(args);
            app.run();
        } catch (Exception ex) {
            reportExceptionAndQuit(ex);
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

    private static void reportExceptionAndQuit(Throwable ex) {
        System.err.println("Exception: " + ex.getMessage());
        System.exit(1);
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
            submit();
            break;
        case "update":
            update();
            break;
        }
    }

    private void server() throws IOException {
        ProcessHandle ph = PidFile.read();
        if (ph != null && ph.isAlive()) {
            System.out.println("Server is running, pid=" + ph.pid());
            return;
        }
        var pid = PidFile.create();
        try {
            LOG.debug("Reading repo_map");
            Map<String, JsonTarget> repoMap = Json.mapper.readValue(
                    new File(Dir.getHome(), "repo_map"),
                    new TypeReference<HashMap<String, JsonTarget>>() {});

            LOG.debug("Substitute env vars");
            substituteEnvVars(repoMap);

            LOG.debug("Start server host={} port={}", host, port);
            Handlers.start(host, port, repoMap);
        } catch (IOException ex) {
            PidFile.delete();
            throw ex;
        }
        System.out.println("Server started, pid=" + pid +
                " host=" + host +
                " port=" + port);
    }

    private static void substituteEnvVars(Map<String, JsonTarget> repoMap) throws IOException {
        var pattern = Pattern.compile("\\$\\{(.+?)}");
        var env = System.getenv();
        for (var r : repoMap.values()) {
            var matcher = pattern.matcher(r.getPath());
            var error = new String[1];
            var subst = matcher.replaceAll(m -> {
                var g = m.group(1);
                if (env.containsKey(g)) {
                    return env.get(g);
                }
                if (error[0] == null) error[0] = g;
                return "";
            });
            if (error[0] != null) {
                // IO error because path is not valid
                throw new IOException("Unknown env variable: " + error[0]);
            }
            r.setPath(subst);
        }
    }

    private void stopServer() {
        ProcessHandle ph = PidFile.read();
        if (ph != null) {
            PidFile.delete();
            if (ph.isAlive()) {
                Future<ProcessHandle> future = ph.onExit();
                try {
                    future.get(3, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException ex) {
                    reportExceptionAndQuit(ex);
                }
                System.out.println("Server pid=" + ph.pid() + " stopped");
                return;
            }
        }
        System.out.println("Server is not running");
    }

    // We have vcs argument because we want to detect it actually.
    private void submit()
            throws InterruptedException, IOException {
        LOG.debug("Submit to host={} port={}", host, port);
        var repoDir = Dir.getParentWithFile(Dir.getCurrent(), ".git");
        if (repoDir == null) {
            throw new IOException("Path to .git not found in " + Dir.getCurrent());
        }
        LOG.debug("repoDir={}", repoDir.toString());
        var repo = new JsonRepo(
                repoDir.getName(),
                Script.run("git/revision", repoDir).trim());
        LOG.debug("Repo name={} revision={}", repo.getName(), repo.getRevision());

        var status = Script.run("git/status", repoDir).split("\n");
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

        LOG.debug("{} untracked files", untrackedFiles.size());
        for (var f : untrackedFiles) {
            String data;
            try (var stream = new FileInputStream(f)) {
                data = new String(stream.readAllBytes());
            }
            post(buildUrl("/copy"), new JsonCopy(
                    repo,
                    repoDir.toPath().relativize(f.toPath()).toString(),
                    data));
        }

        var diff = Script.run("git/diff", repoDir);
        post(buildUrl("/diff"), new JsonDiff(repo, diff));
    }

    private void update() throws IOException {
        copyScripts(App.class.getClassLoader(), "git");
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
        LOG.debug("POST to {}", url.toString());
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", MimeType.JSON);
            connection.setRequestProperty("Accept", MimeType.JSON);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            LOG.debug("POST: writing payload");
            try (var os = connection.getOutputStream()) {
                Json.write(request, os);
            }
            LOG.debug("POST: reading response");
            try (var is = connection.getInputStream()) {
                return is.readAllBytes();
            }
        } catch (IOException ex) {
            LOG.debug("POST: IO exception {}", ex.getMessage());
            try (var es = connection.getErrorStream()) {
                String msg;
                if (es != null) {
                    msg = Json.read(JsonStatus.class, es).getMessage();
                } else {
                    msg = "HTTP error code: " + connection.getResponseCode();
                }
                throw new IOException(msg, ex);
            }
        } finally {
            if (connection != null) {
                LOG.debug("POST: disconnect");
                connection.disconnect();
            }
        }
    }

    private static void copyScripts(ClassLoader cl, String dirName) throws IOException {
        var destPath = new File(Dir.getHome(), dirName);
        destPath.mkdir();
        var prefix = dirName + "/";
        for (var a : new String[]{"apply", "diff", "revision", "status"}) {
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
