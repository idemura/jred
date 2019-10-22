package id.jred;

import java.io.File;
import java.io.IOException;

public final class Dir {
    private Dir() {}

    public static boolean createHome() {
        return getHome().mkdirs();
    }

    public static File getHome() {
        return new File(System.getProperty("user.home"), ".jred");
    }

    public static File getCurrent() throws IOException {
        return new File(".").getAbsoluteFile().getCanonicalFile();
    }

    public static File getParentWithFile(File dir, String name) {
        while (dir != null && !new File(dir, name).exists()) {
            dir = dir.getParentFile();
        }
        return dir;
    }
}
