package id.jred;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WorkDir {
    private WorkDir() {}

    public static void create() {
        try {
            Files.createDirectory(getPath());
        } catch (FileAlreadyExistsException ex) {
            // Ignore
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Path getPath() {
        return Path.of(System.getProperty("user.home"), ".jred");
    }
}
