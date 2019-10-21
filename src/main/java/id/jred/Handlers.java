package id.jred;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

public final class Handlers {
    private static final Logger LOG = LoggerFactory.getLogger(Handlers.class);

    private final Map<String, Repo> repoMap;

    public static void start(String host, int port, Map<String, Repo> repoMap) {
        Spark.ipAddress(host);
        Spark.port(port);

        var handler = new Handlers(repoMap);
        Spark.get("/", handler::root);
        Spark.post("/copy", handler::copy);
        Spark.post("/diff", handler::diff);

        new Thread(() -> {
            while (PidFile.read().isPresent()) {
                try {
                    Thread.sleep(200 /* millis */);
                } catch (InterruptedException ex) {
                    break;
                }
            }
            Spark.stop();
        }).start();
    }

    private Handlers(Map<String, Repo> repoMap) {
        this.repoMap = repoMap;
    }

    private Object root(Request req, Response response) {
        try {
            response.type(MimeType.TEXT);
            var os = new ByteArrayOutputStream();
            os.write("jred is running\n\n".getBytes());
            Json.writeFormatted(repoMap, os);
            return os.toString();
        } catch (Exception ex) {
            LOG.error(ex.getMessage());
            return respondError(response, 400, ex);
        }
    }

    private Object copy(Request req, Response response) {
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
            return respondSuccess(response);
        } catch (Exception ex) {
            LOG.error(ex.getMessage());
            return respondError(response, 400, ex);
        }
    }

    private Object diff(Request req, Response response) {
        try {
            var diffRequest = Json.read(Protocol.Diff.class, req.bodyAsBytes());
            var repo = repoMap.get(diffRequest.getRepo().getName());
            if (repo == null) {
                return respondError(response, 400,
                        "Repo not found: " + diffRequest.getRepo().getName());
            }
            var repoPath = new File(repo.getPath()).getAbsoluteFile().getCanonicalFile();
            var revision = Script.run(repo.getVCS() + "/revision", repoPath).trim();
            if (!revision.equals(diffRequest.getRepo().getRevision())) {
                return respondError(response, 400,
                        "Revision mismatch: server " + revision +
                        ", client " + diffRequest.getRepo().getRevision());
            }
            if (!diffRequest.getDiff().isEmpty()) {
                Script.run(
                        repo.getVCS() + "/apply",
                        repoPath,
                        diffRequest.getDiff());
            }
            return respondSuccess(response);
        } catch (Exception ex) {
            LOG.error(ex.getMessage());
            return respondError(response, 400, ex);
        }
    }

    private static String respondSuccess(Response response) {
        return respondError(response, 200, "");
    }

    private static String respondError(Response response, int code, Exception ex) {
        return respondError(response, code, ex.getMessage());
    }

    private static String respondError(Response response, int code, String errorMsg) {
        response.status(code);
        response.type(MimeType.JSON);
        return Json.writeString(new Protocol.Error(errorMsg));
    }
}
