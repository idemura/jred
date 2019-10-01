package id.jred;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RequestHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RequestHandler.class);

    private final static class ResponseWrapper {
        private final Response response;

        public ResponseWrapper(Response response) {
            this.response = response;
        }

        public String ok() {
            return build(200, "OK");
        }

        public String build(int code, Throwable ex) {
            return build(code, ex.toString());
        }

        public String build(int code, String details) {
            response.status(code);
            response.type(Protocol.MIME_JSON);
            return Json.write(new Protocol.Status(code, details));
        }
    }

    private final ServerConfig config;

    public static void start(ServerConfig config) {
        Spark.ipAddress(config.getHost());
        Spark.port(config.getPort());

        var handler = new RequestHandler(config);
        Spark.get("/", handler::root);
        Spark.post("/copy", handler::copy);
        Spark.post("/diff", handler::diff);

        new Thread(() -> {
            while (Util.isPidFileExists()) {
                try {
                    Thread.sleep(200 /* millis */);
                } catch (InterruptedException ex) {
                    break;
                }
            }
            Spark.stop();
        }).start();
    }

    private RequestHandler(ServerConfig config) {
        this.config = config;
    }

    private Object root(Request req, Response response) {
        response.type(Protocol.MIME_TEXT);
        var sb = new StringBuilder("jred is running\n\n");
        for (var f : config.getRepo().values()) {
            sb.append(f.getPath()).append("\n");
        }
        return sb.toString();
    }

    private Object copy(Request req, Response response) {
        var rw = new ResponseWrapper(response);
        try {
            var copyRequest = Json.read(Protocol.Copy.class, req.bodyAsBytes());
            var repoCfg = config.getRepo().get(copyRequest.getRepo().getName());
            if (repoCfg == null) {
                return rw.build(
                        400,
                        "Repo not found: " + copyRequest.getRepo().getName());
            }
            var repoPath = Path.of(repoCfg.getPath()).toAbsolutePath().normalize();
            var destPath = repoPath.resolve(copyRequest.getFileName()).normalize();
            if (!destPath.startsWith(repoPath)) {
                return rw.build(
                        400,
                        "File must belong to repo directory tree: " + copyRequest.getFileName());
            }
            Files.createDirectories(destPath.getParent());
            try (var stream = new FileOutputStream(destPath.toString())) {
                stream.write(copyRequest.getData().getBytes(StandardCharsets.UTF_8));
            }
            return rw.ok();
        } catch (Exception ex) {
            LOG.error(ex.toString());
            return rw.build(400, ex);
        }
    }

    private Object diff(Request req, Response response) {
        var rw = new ResponseWrapper(response);
        try {
            var diffRequest = Json.read(Protocol.Diff.class, req.bodyAsBytes());
            var repoCfg = config.getRepo().get(diffRequest.getRepo().getName());
            if (repoCfg == null) {
                return rw.build(
                        400,
                        "Repo not found: " + diffRequest.getRepo().getName());
            }
            var repoPath = Path.of(repoCfg.getPath()).toAbsolutePath().normalize();
            return rw.ok();
        } catch (Exception ex) {
            LOG.error(ex.toString());
            return rw.build(400, ex);
        }
    }
}
