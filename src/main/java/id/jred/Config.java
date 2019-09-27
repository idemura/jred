package id.jred;

import java.io.File;

public class Config {
    public static File getUserHomeFile(String name) {
        return new File(System.getProperty("user.home"), name);
    }
}
