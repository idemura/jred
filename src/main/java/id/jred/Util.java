package id.jred;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Util {
    private Util() {}

    public static void createWorkDir() {
        try {
            Files.createDirectory(getWorkDir());
        } catch (FileAlreadyExistsException ex) {
            // Empty
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Path getWorkDir() {
        return Path.of(System.getProperty("user.home"), ".jred");
    }

    public static void createPidFile() {
        try (var writer = new PrintWriter(
                getPidPath().toString(),
                StandardCharsets.UTF_8)) {
            writer.print(ProcessHandle.current().pid());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static boolean deletePidFile() {
        try {
            return Files.deleteIfExists(getPidPath());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static boolean isPidFileExists() {
        return Files.exists(getPidPath());
    }

    public static long readPidFile() {
        try {
            return Long.parseLong(Files.readString(getPidPath()).trim());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Path getPidPath() {
        return getWorkDir().resolve("pid");
    }
}
