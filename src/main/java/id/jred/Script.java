package id.jred;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

public final class Script {
    private Script() {}

    public static String run(String name, Optional<Path> workDir) {
        return run(name, workDir, Optional.empty());
    }

    public static String run(String name, Optional<Path> workDir, Optional<String> stdin) {
        Process process = null;
        try {
            var pb = new ProcessBuilder(
                    "/bin/bash",
                    "-e",
                    WorkDir.getPath().resolve(name).toString());
            pb.redirectErrorStream(true);
            workDir.ifPresent(p -> pb.directory(p.toFile()));
            process = pb.start();
            if (stdin.isPresent()) {
                var s = stdin.get();
                process.getOutputStream().write(s.getBytes(StandardCharsets.UTF_8));
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
