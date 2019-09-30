package id.jred;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.nio.file.Path;
import java.util.Map;

public final class RequestHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RequestHandler.class);

    private final Map<String, Path> repoNameMap;

    public static void start(ServerConfig config) {
        Spark.ipAddress(config.getHost());
        Spark.port(config.getPort());

        var handler = new RequestHandler(config.createRepoNameMap());
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

    private RequestHandler(Map<String, Path> repoNameMap) {
        this.repoNameMap = repoNameMap;
    }

    private Object root(Request req, Response response) {
        response.type(Protocol.MIME_TEXT);
        var sb = new StringBuilder("jred is running\n\n");
        for (var f : repoNameMap.values()) {
            sb.append(f.toString()).append("\n");
        }
        return sb.toString();
    }

    private Object copy(Request req, Response response) {
        response.type(Protocol.MIME_JSON);
        try {
            var copyRequest = Protocol.Copy.fromWire(req.body());
            var repoPath = repoNameMap.get(copyRequest.getRepo().getName());
            if (repoPath != null) {
                //
            }
            return createResponse(response, Protocol.OK);
        } catch (Exception ex) {
            LOG.error(ex.toString());
            return createResponse(response, new Protocol.Status(400, ex));
        }
    }

    private Object diff(Request req, Response response) {
        try {
            var diffRequest = Protocol.Diff.fromWire(req.body());
            return createResponse(response, Protocol.OK);
        } catch (Exception ex) {
            LOG.error(ex.toString());
            return createResponse(response, new Protocol.Status(400, ex));
        }
    }

    private static String createResponse(Response response, Protocol.Status status) {
        response.status(status.getError());
        response.type(Protocol.MIME_JSON);
        try {
            return Protocol.toWire(status);
        } catch (Exception ex) {
            LOG.error(ex.toString());
            response.status(500);
            return "{ \"error\": 500 }";
        }
    }
}
