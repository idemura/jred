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

    private final Map<String, JsonTarget> repoMap;

    public static void start(String host, int port, Map<String, JsonTarget> repoMap) {
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

    private Handlers(Map<String, JsonTarget> repoMap) {
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
            return respondUnexpected(response, ex);
        }
    }

    private Object copy(Request req, Response response) {
        LOG.debug("Handle /copy");
        try {
            var copyRequest = Json.read(JsonCopy.class, req.bodyAsBytes());
            var repo = repoMap.get(copyRequest.getRepo().getName());
            if (repo == null) {
                return respondBadRequest(
                        response,
                        "Repo not found: " + copyRequest.getRepo().getName());
            }
            var repoPath = new File(repo.getPath()).getAbsoluteFile().getCanonicalFile();
            var destPath = new File(repoPath, copyRequest.getFile()).getCanonicalFile();
            if (!destPath.toPath().startsWith(repoPath.toPath())) {
                return respondBadRequest(
                        response,
                        "File must belong to repo directory tree: " + copyRequest.getFile());
            }
            destPath.getParentFile().mkdirs();
            try (var stream = new FileOutputStream(destPath)) {
                stream.write(copyRequest.getData().getBytes());
            }
            return respondOK(response);
        } catch (IOException ex) {
            return respondUnexpected(response, ex);
        }
    }

    private Object diff(Request req, Response response) {
        LOG.debug("Handle /diff");
        try {
            var diffRequest = Json.read(JsonDiff.class, req.bodyAsBytes());
            var repo = repoMap.get(diffRequest.getRepo().getName());
            if (repo == null) {
                return respondBadRequest(
                        response,
                        "Repo not found: " + diffRequest.getRepo().getName());
            }
            var repoPath = new File(repo.getPath()).getAbsoluteFile().getCanonicalFile();
            var revision = Script.run("git/revision", repoPath).trim();
            if (!revision.equals(diffRequest.getRepo().getRevision())) {
                return respondBadRequest(response,
                        "Revision mismatch: server " + revision +
                        ", client " + diffRequest.getRepo().getRevision());
            }
            if (!diffRequest.getDiff().isEmpty()) {
                Script.run(
                        "git/apply",
                        repoPath,
                        diffRequest.getDiff());
            }
            return respondOK(response);
        } catch (InterruptedException | IOException ex) {
            return respondUnexpected(response, ex);
        }
    }

    private static String respondOK(Response response)
            throws IOException {
        response.status(200);
        return renderJson(response, new JsonStatus());
    }

    private static String respondBadRequest(Response response, String message)
            throws IOException {
        response.status(400);
        return renderJson(response, new JsonStatus(message));
    }

    private static String respondUnexpected(Response response, Exception cause) {
        LOG.error("Error: {}", cause.getMessage());
        response.status(500);
        try {
            return renderJson(response, new JsonStatus(cause.getMessage()));
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
