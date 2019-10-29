package id.jred;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Script {
    private static final Logger LOG = LoggerFactory.getLogger("jred");

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

    public static String runShell(String name, List<String> args, File workDir)
            throws InterruptedException, IOException {
        var command = new ArrayList<>(SHELL);
        command.add(new File(Dir.getHome(), name).toString());
        command.addAll(args);
        return run(command, workDir, null /* stdin */);
    }

    public static String run(List<String> command, File workDir, String stdin)
            throws InterruptedException, IOException {
        Process process = null;
        try {
            LOG.debug("Run `{}` in {}", String.join(" ", command), workDir.toString());
            var pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.directory(workDir); // Accepts null
            LOG.debug("Starting process");
            process = pb.start();
            if (stdin != null) {
                LOG.debug("Sending stdin");
                process.getOutputStream().write(stdin.getBytes());
                process.getOutputStream().close();
            }
            LOG.debug("Reading stdout/stderr");
            var output = new String(process.getInputStream().readAllBytes());

            LOG.debug("Waiting to finish...");
            var exitCode = process.waitFor();
            LOG.debug("Process exit code {}", exitCode);
            if (exitCode != 0) {
                // Normal IO with process is not possible.
                throw new IOException(MessageFormat.format(
                        "`{0}` exit code {1}:\n{2}",
                        String.join(" ", command),
                        exitCode,
                        output));
            }
            return output;
        } finally {
            if (process != null) {
                LOG.debug("Destroy process");
                process.destroy();
            }
        }
    }
}
