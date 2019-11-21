package id.jred;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

public final class App {
    private static final Logger LOG = LoggerFactory.getLogger("jred");

    private static final Pattern regexCommit = Pattern.compile("^commit ([0-9a-f]+)");
    private static final Pattern regexGitSvnId = Pattern.compile("^\\s+git-svn-id: (.+)@([0-9]+) ");

    public static void main(String[] args) {
        try {
            var app = new App(args);
            app.run();
        } catch (Exception ex) {
            reportExceptionAndQuit(ex);
        }
    }

    @Parameters(separators="=", commandDescription="Start server")
    private static final class CommandServer {
        @Parameter(
            names={"-s", "--stop"},
            description="Stop running server")
        private boolean stop = false;
    }

    public static final class VCSValidator implements IValueValidator<String> {
        @Override
        public void validate(String name, String value)
                throws ParameterException {
            try {
                VCS.fromCmdLineString(value);
            } catch (IllegalArgumentException ex) {
                throw new ParameterException(
                        "VCS must be one of: " + VCS.allValuesAsString());
            }
        }
    }

    @Parameters(separators="=", commandDescription="Submit files and diff")
    private static final class CommandSubmit {
        @Parameter(
            names={"--vcs"},
            description="Repo VCS (git or gitsvn)",
            validateValueWith=VCSValidator.class)
        private String vcs = "git";

        @Parameter(
            names={"--log"},
            description="Max log length to find git-svn-id")
        private int logLength = 10;
    }

    @Parameters(separators="=", commandDescription="Update home dir files")
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
        var msg = ex.getMessage();
        System.err.print("Exception: " +
                (msg.endsWith("\n") ? ex.getMessage() : ex.getMessage() + "\n"));
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

            // Ensure repo map is correct
            for (var r : repoMap.values()) {
                var vcs = VCS.fromCmdLineString(r.getVCS());
                switch (vcs) {
                case GIT: {
                    var vcsDir = "." + vcs.toCmdLineString();
                    if (!new File(new File(r.getPath()), vcsDir).exists()) {
                        throw ioException("git repo not found in {0}", r.getPath());
                    }
                }
                case SVN: {
                    if (!new File(r.getPath()).exists()) {
                        throw ioException("svn repo not found in {0}", r.getPath());
                    }
                    break;
                }
                default:
                    throw ioException("Unsupported VCS: {0}", vcs);
                }
            }

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
                throw ioException("Unknown env variable: {0}", error[0]);
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
        var vcs = VCS.fromCmdLineString(cmdSubmit.vcs);
        if (!(vcs == VCS.GIT || vcs == VCS.GITSVN)) {
            throw new IllegalArgumentException("Unsupported VCS: " + vcs);
        }
        LOG.debug("Submit to host={} port={}", host, port);
        var repoDir = Dir.getParentWithFile(Dir.getCurrent(), ".git");
        if (repoDir == null) {
            throw ioException(".git not found in {0} or parent", Dir.getCurrent());
        }
        LOG.debug("Repo dir {}", repoDir.toString());
        var revision = getDiffBaseRevision(vcs, repoDir);
        LOG.debug("Revision to send: {}, base revision: {}",
                  revision[0],
                  revision[1]);
        var repo = new JsonRepo(repoDir.getName(), revision[0]);
        LOG.debug("Repo: name={} revision={}", repo.getName(), repo.getRevision());

        // First, send diff, because it does reset.
        var diff = Script.runShell("git/diff", Arrays.asList(revision[1]), repoDir);
        post(buildUrl("/diff"), new JsonDiff(repo, diff));

        // After, send untracked files
        var status = Script.runShell(
                "git/status", // Do not replace with VCS - could be GITSVN.
                Collections.emptyList(),
                repoDir).split("\n");
        var untrackedFiles = new ArrayList<File>();
        for (var s : status) {
            if (s.length() >= 3 && "??".equals(s.substring(0, 2))) {
                var path = new File(repoDir, s.substring(3)).getCanonicalFile();
                if (path.isDirectory()) {
                    var stack = new ArrayList<File>();
                    stack.add(path);
                    while (!stack.isEmpty()) {
                        var dirFiles = stack.remove(stack.size() - 1).listFiles();
                        if (dirFiles != null)
                        {
                            for (var f : dirFiles) {
                                if (f.isDirectory()) {
                                    stack.add(f);
                                } else {
                                    untrackedFiles.add(f);
                                }
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
    }

    // Returns two strings:
    //   1: Revision to send over network
    //   2: Revision as diff base
    private String[] getDiffBaseRevision(VCS vcs, File repoDir)
            throws InterruptedException, IOException {
        switch (vcs) {
        case GIT: {
            var gitRevision = Script.runShell(
                    vcs.toCmdLineString() + "/revision",
                    Collections.emptyList(),
                    repoDir).trim();
            return new String[]{gitRevision, gitRevision};
        }
        case GITSVN: {
            if (cmdSubmit.logLength <= 0) {
                throw new IllegalArgumentException("Invalid log length");
            }
            var output = Script.run(
                    Arrays.asList("git", "log", "-" + cmdSubmit.logLength),
                    repoDir,
                    null /* stdin */);
            String gitRevision = null;
            String gitSvnId = null;
            for (var s : output.split("\n")) {
                var gitCommitMatcher = regexCommit.matcher(s);
                if (gitCommitMatcher.find()) {
                    gitRevision = gitCommitMatcher.group(1);
                } else {
                    var gitSvnIdMatcher = regexGitSvnId.matcher(s);
                    if (gitSvnIdMatcher.find()) {
                        gitSvnId = gitSvnIdMatcher.group(2);
                        break;
                    }
                }
            }
            if (gitSvnId == null || gitRevision == null) {
                throw ioException("git-svn-id not found");
            }
            return new String[]{gitSvnId, gitRevision};
        }
        default:
            throw new IllegalArgumentException("Submit on SVN repo is not supported");
        }
    }

    private void update() throws IOException {
        copyScripts(App.class.getClassLoader(), "git");
        copyScripts(App.class.getClassLoader(), "gitsvn");
        copyScripts(App.class.getClassLoader(), "svn");
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
        for (var a : new String[]{"apply", "diff", "reset", "revision", "status"}) {
            var qualifiedName = prefix + a;
            try (var is = cl.getResourceAsStream(qualifiedName)) {
                if (is != null) {
                    try (var os = new FileOutputStream(new File(destPath, a))) {
                        os.write(is.readAllBytes());
                    }
                }
            }
        }
    }

    private static IOException ioException(String format, Object... arguments) {
        return new IOException(MessageFormat.format(format, arguments));
    }
}
