package id.jred;

import spark.Spark;

public class App {
    public static void main(String[] args) {
        var cmdLineArgs = new CmdLineArgs(args);
        if (cmdLineArgs.isHelp()) {
            cmdLineArgs.printHelp();
            return;
        }
        var positional = cmdLineArgs.getPositional();
        if (positional.isEmpty() || positional.get(0).equals("client")) {
            System.out.println("jred client");
        } else if (positional.get(0).equals("server")) {
            Spark.ipAddress(cmdLineArgs.getHost());
            Spark.port(cmdLineArgs.getPort());

            var server = new RequestHandlers();
            Spark.get("/", server::hello);
            Spark.get("/hello", server::hello);
        } else {
            System.err.println("Invalid mode: " + positional.get(0));
            System.exit(1);
        }
    }
}
