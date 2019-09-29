package id.jred;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class App {
    public static void main(String[] args) {
        var cmdLineArgs = new CmdLineArgs(args);
        if (cmdLineArgs.isHelp()) {
            cmdLineArgs.printHelp();
            return;
        }
        var positional = cmdLineArgs.getPositional();
        if (positional.isEmpty() || positional.get(0).equals("client")) {
            var config = ClientConfig.create(cmdLineArgs);
            System.out.println(config.getHost());
            System.out.println(config.getPort());

            System.out.println("jred client");
        } else if (positional.get(0).equals("server")) {
            var config = ServerConfig.create(cmdLineArgs);
            var repoNameMap = createRepoNameMap(config.getRepo());
            RequestHandlers.start(config, repoNameMap);
        } else {
            System.err.println("Invalid mode: " + positional.get(0));
            System.exit(1);
        }
    }

    private static Map<String, File> createRepoNameMap(List<String> repos) {
        var repoMap = new HashMap<String, File>();
        for (var repoPath : repos) {
            var f = new File(repoPath);
            if (repoMap.containsKey(f.getName())) {
                throw new RuntimeException("Repository duplicate: " + f.getName());
            }
            repoMap.put(f.getName(), f);
        }
        return repoMap;
    }
}
