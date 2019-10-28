package id.jred;

import java.util.ArrayList;

public enum VCS {
    GIT,
    GITSVN,
    SVN;

    public static String allValuesAsString() {
        var valuesAsString = new ArrayList<String>();
        for (var v : values()) {
          valuesAsString.add(v.name().toLowerCase());
        }
        return String.join(", ", valuesAsString);
    }

    public static VCS fromCmdLineString(String s) {
        return valueOf(s.toUpperCase());
    }

    public String toCmdLineString() {
        return toString().toLowerCase();
    }
}
