package id.jred;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public final class Handlers {
    private static final Logger LOG = LoggerFactory.getLogger("jred");

    private final Map<String, Repo> repoMap;

    public static void start(String host, int port, Map<String, Repo> repoMap) {
        Spark.ipAddress(host);
        Spark.port(port);

        var handler = new Handlers(repoMap);
        Spark.get("/", handler::root);
        Spark.post("/copy", handler::copy);
        Spark.post("/diff", handler::diff);

        new Thread(() -> {
            while (PidFile.read() != null) {
                try {
                    Thread.sleep(200 /* millis */);
                } catch (InterruptedException ex) {
                    break;
                }
            }
            LOG.debug("Stopping Spark");
            Spark.stop();
        }).start();
    }

    private Handlers(Map<String, Repo> repoMap) {
        this.repoMap = repoMap;
    }

    private Object root(Request req, Response response) {
        LOG.debug("Handle /");
        try {
            response.type(MimeType.TEXT);
            var os = new ByteArrayOutputStream();
            try (var writer = new PrintWriter(os)) {
                writer.println("jred is running");
                writer.println();
                writer.flush(); // Must be here, or repo map will be before
                Json.writeFormatted(repoMap, os);
            }
            return os.toString();
        } catch (IOException ex) {
            return respondCatch(response, ex);
        }
    }

    private Object copy(Request req, Response response) {
        LOG.debug("Handle /copy");
        try {
            var copyRequest = Json.read(Protocol.Copy.class, req.bodyAsBytes());
            var repo = repoMap.get(copyRequest.getRepo().getName());
            if (repo == null) {
                return respondError(response, 400,
                                    "Repo not found: " + copyRequest.getRepo().getName());
            }
            var repoPath = new File(repo.getPath()).getAbsoluteFile().getCanonicalFile();
            var destPath = new File(repoPath, copyRequest.getFile()).getCanonicalFile();
            if (!destPath.toPath().startsWith(repoPath.toPath())) {
                return respondError(response, 400,
                                    "File must belong to repo directory tree: " + copyRequest.getFile());
            }
            destPath.getParentFile().mkdirs();
            try (var stream = new FileOutputStream(destPath)) {
                stream.write(copyRequest.getData().getBytes());
            }
            return respond200(response);
        } catch (IOException ex) {
            return respondCatch(response, ex);
        }
    }

    private Object diff(Request req, Response response) {
        LOG.debug("Handle /diff");
        try {
            var diffRequest = Json.read(Protocol.Diff.class, req.bodyAsBytes());
            var repo = repoMap.get(diffRequest.getRepo().getName());
            if (repo == null) {
                return respondError(response, 400,
                                    "Repo not found: " + diffRequest.getRepo().getName());
            }
            var repoPath = new File(repo.getPath()).getAbsoluteFile().getCanonicalFile();
            var revision = Script.run("git/revision", repoPath).trim();
            if (!revision.equals(diffRequest.getRepo().getRevision())) {
                return respondError(response, 400,
                                    "Revision mismatch: server " + revision +
                        ", client " + diffRequest.getRepo().getRevision());
            }
            if (!diffRequest.getDiff().isEmpty()) {
                Script.run(
                        "git/apply",
                        repoPath,
                        diffRequest.getDiff());
            }
            return respond200(response);
        } catch (InterruptedException | IOException ex) {
            return respondCatch(response, ex);
        }
    }

    private static String respond200(Response response)
            throws IOException {
        response.status(200);
        return renderJson(response, Protocol.error());
    }

    private static String respondError(Response response, int status, String message)
            throws IOException {
        response.status(status);
        return renderJson(response, Protocol.error(message));
    }

    private static String respondCatch(Response response, Exception cause) {
        LOG.error("Error: {}", cause.getMessage());
        response.status(500);
        try {
            return renderJson(response, Protocol.error(cause.getMessage()));
        } catch (IOException ex) {
            LOG.error("Fatal: {}", ex.getMessage());
            return null;
        }
    }

    private static String renderJson(Response response, Object object)
            throws IOException {
        response.type(MimeType.JSON);
        return Json.writeString(object);
    }
}
