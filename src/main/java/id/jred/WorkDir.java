package id.jred;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WorkDir {
    private WorkDir() {}

    public static boolean create() {
        try {
            Files.createDirectory(getPath());
            return true;
        } catch (FileAlreadyExistsException ex) {
            return false;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Path getPath() {
        return Path.of(System.getProperty("user.home"), ".jred");
    }

    public static Path getCurrent() {
        return Path.of(".").toAbsolutePath().normalize();
    }
}
