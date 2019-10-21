package id.jred;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Scanner;

public class PidFile {
    public static void create() {
        try (var writer = new PrintWriter(getPath(), StandardCharsets.UTF_8)) {
            writer.print(ProcessHandle.current().pid());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void delete() {
        getPath().delete();
    }

    public static Optional<ProcessHandle> read() {
        try (var scanner = new Scanner(getPath())) {
            return ProcessHandle.of(scanner.nextLong());
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private static File getPath() {
        return new File(Dir.getHome(), "pid");
    }
}
