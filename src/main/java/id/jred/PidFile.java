package id.jred;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class PidFile {
    public static void create() {
        try (var writer = new PrintWriter(
                getPath().toString(),
                StandardCharsets.UTF_8)) {
            writer.print(ProcessHandle.current().pid());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static boolean delete() {
        try {
            return Files.deleteIfExists(getPath());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static boolean exists(boolean checkAlive) {
        var exists = Files.exists(getPath());
        if (exists && checkAlive) {
            return ProcessHandle.of(read()).isPresent();
        }
        return exists;
    }

    public static long read() {
        try {
            return Long.parseLong(Files.readString(getPath()).trim());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Path getPath() {
        return WorkDir.getPath().resolve("pid");
    }
}
