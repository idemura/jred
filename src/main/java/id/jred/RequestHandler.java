package id.jred;

import spark.Request;
import spark.Response;
import spark.Spark;

import java.nio.file.Path;
import java.util.Map;

public final class RequestHandler {
    private final Map<String, Path> repoNameMap;

    public static void start(ServerConfig config) {
        Spark.ipAddress(config.getHost());
        Spark.port(config.getPort());

        var instance = new RequestHandler(config.createRepoNameMap());
        Spark.get("/", instance::root);

        new Thread(() -> {
            while (Util.isPidFileExists()) {
                try {
                    Thread.sleep(1000);
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
        var sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n")
                .append("<html>\n")
                .append("<head>\n")
                .append("<title>jred server</title>\n")
                .append("<body>\n")
                .append("<pre>\n");

        sb.append("jred is working\n");
        for (var f : repoNameMap.values()) {
            sb.append(f.toString()).append("\n");
        }

        sb.append("</pre>\n")
                .append("</body>\n")
                .append("</html>\n");
        return sb.toString();
    }
}
