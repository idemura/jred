package id.jred;

import spark.*;

import java.io.File;
import java.util.Map;

public class RequestHandlers {
    private static RequestHandlers instance;
    private final Map<String, File> repoNameMap;

    public static void start(ServerConfig config, Map<String, File> repoNameMap) {
        if (instance != null) {
            throw new RuntimeException("Handler double init");
        }

        Spark.ipAddress(config.getHost());
        Spark.port(config.getPort());

        instance = new RequestHandlers(repoNameMap);
        Spark.get("/", instance::root);
    }

    private RequestHandlers(Map<String, File> repoNameMap) {
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
