package id.jred;

import java.io.File;

public final class Dir {
    private Dir() {}

    public static boolean createHome() {
        return getHome().mkdirs();
    }

    public static File getHome() {
        return new File(System.getProperty("user.home"), ".jred");
    }

    public static File getCurrent() {
        try {
            return new File(".").getAbsoluteFile().getCanonicalFile();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
