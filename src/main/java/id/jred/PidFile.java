package id.jred;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.Scanner;

public final class PidFile {
    public static void create() throws IOException
    {
        try (var writer = new PrintWriter(getPath())) {
            writer.print(ProcessHandle.current().pid());
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
