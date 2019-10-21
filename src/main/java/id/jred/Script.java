package id.jred;

import java.io.File;
import java.nio.charset.StandardCharsets;

public final class Script {
    private Script() {}

    public static String run(String name) {
        return run(name, null, null);
    }

    public static String run(String name, File workDir) {
        return run(name, workDir, null);
    }

    public static String run(String name, File workDir, String stdin) {
        Process process = null;
        try {
            var pb = new ProcessBuilder(
                    "/bin/bash",
                    "-e",
                    new File(Dir.getHome(), name).toString());
            pb.redirectErrorStream(true);
            if (workDir != null) {
                pb.directory(workDir);
            }
            process = pb.start();
            if (stdin != null) {
                process.getOutputStream().write(stdin.getBytes(StandardCharsets.UTF_8));
                process.getOutputStream().close();
            }
            var exitCode = process.waitFor();
            var output = new String(
                    process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            if (exitCode != 0) {
                throw new RuntimeException(
                        "Script exit code " + exitCode + ": " + output);
            }
            return output;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}
