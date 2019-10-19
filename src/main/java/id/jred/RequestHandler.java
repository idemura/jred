package id.jred;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RequestHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RequestHandler.class);

    private final ServerConfig config;

    public static void start(ServerConfig config) {
        Spark.ipAddress(config.getHost());
        Spark.port(config.getPort());

        var handler = new RequestHandler(config);
        Spark.get("/", handler::root);
        Spark.post("/copy", handler::copy);
        Spark.post("/diff", handler::diff);

        new Thread(() -> {
            while (PidFile.exists(false /* checkAlive */)) {
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
        try {
            response.type(MimeType.TEXT);
            var os = new ByteArrayOutputStream();
            os.write("jred is running\n\n".getBytes(StandardCharsets.UTF_8));
            Json.writeFormatted(config, os);
            return os.toString(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            LOG.error(ex.toString());
            return respondCode(response, 400, ex);
        }
    }

    private Object copy(Request req, Response response) {
        try {
            var copyRequest = Json.read(Protocol.Copy.class, req.bodyAsBytes());
            var repoCfg = config.getRepo().get(copyRequest.getRepo().getName());
            if (repoCfg == null) {
                return respondCode(response, 400,
                        "Repo not found: " + copyRequest.getRepo().getName());
            }
            var repoPath = Path.of(repoCfg.getPath()).toAbsolutePath().normalize();
            var destPath = repoPath.resolve(copyRequest.getFileName()).normalize();
            if (!destPath.startsWith(repoPath)) {
                return respondCode(response, 400,
                        "File must belong to repo directory tree: " + copyRequest.getFileName());
            }
            Files.createDirectories(destPath.getParent());
            try (var stream = new FileOutputStream(destPath.toString())) {
                stream.write(copyRequest.getData().getBytes(StandardCharsets.UTF_8));
            }
            return respondSuccess(response);
        } catch (Exception ex) {
            LOG.error(ex.toString());
            return respondCode(response, 400, ex);
        }
    }

    private Object diff(Request req, Response response) {
        try {
            var diffRequest = Json.read(Protocol.Diff.class, req.bodyAsBytes());
            var repoCfg = config.getRepo().get(diffRequest.getRepo().getName());
            if (repoCfg == null) {
                return respondCode(response, 400,
                        "Repo not found: " + diffRequest.getRepo().getName());
            }
            var repoPath = Path.of(repoCfg.getPath()).toAbsolutePath().normalize();
            return respondSuccess(response);
        } catch (Exception ex) {
            LOG.error(ex.toString());
            return respondCode(response, 400, ex);
        }
    }

    private static String respondSuccess(Response response) {
        return respondCode(response, 200, "");
    }

    private static String respondCode(Response response, int code, Exception ex) {
        return respondCode(response, code, ex.toString());
    }

    private static String respondCode(Response response, int code, String details) {
        response.status(code);
        response.type(MimeType.JSON);
        var os = new ByteArrayOutputStream();
        Json.write(new Protocol.Status(code, details), os);
        return os.toString(StandardCharsets.UTF_8);
    }
}
