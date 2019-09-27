package id.jred;

import spark.Request;
import spark.Response;
import spark.Spark;

public class RequestHandlers {
    public RequestHandlers() {}

    public Object hello(Request req, Response response) {
      return "hello";
    }
}
