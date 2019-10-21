package id.jred;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Script {
    private Script() {}

    private static final List<String> SHELL = new ArrayList<>();
    static {
        if (System.getenv().containsKey("JRED_SHELL")) {
            Collections.addAll(SHELL, System.getenv().get("JRED_SHELL").split(" "));
            SHELL.removeIf(String::isEmpty);
        } else {
            Collections.addAll(SHELL, "/bin/bash", "-e");
        }
    }

    public static String run(String name)
            throws InterruptedException, IOException {
        return run(name, null, null);
    }

    public static String run(String name, File workDir)
            throws InterruptedException, IOException {
        return run(name, workDir, null);
    }

    public static String run(String name, File workDir, String stdin)
            throws InterruptedException, IOException {
        Process process = null;
        try {
            var command = new ArrayList<>(SHELL);
            command.add(new File(Dir.getHome(), name).toString());

            var pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            if (workDir != null) {
                pb.directory(workDir);
            }
            process = pb.start();
            if (stdin != null) {
                process.getOutputStream().write(stdin.getBytes());
                process.getOutputStream().close();
            }
            var exitCode = process.waitFor();
            var output = new String(process.getInputStream().readAllBytes());
            if (exitCode != 0) {
                // Normal IO with process is not possible.
                throw new IOException(
                        "Script exit code " + exitCode + ": " + output);
            }
            return output;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}
