package id.jred;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public final class PidFile {
    public static long create() throws IOException {
        var pid = ProcessHandle.current().pid();
        try (var writer = new PrintWriter(getPath())) {
            writer.print(pid);
        }
        return pid;
    }

    public static void delete() {
        getPath().delete();
    }

    public static ProcessHandle read() {
        try (var scanner = new Scanner(getPath())) {
            return ProcessHandle.of(scanner.nextLong()).orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }

    private static File getPath() {
        return new File(Dir.getHome(), "pid");
    }
}
