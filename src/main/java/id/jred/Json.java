package id.jred;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.OutputStream;

public final class Json {
    private Json() {}

    private static final ObjectMapper mapper = new ObjectMapper();

    public static <T> T read(Class<T> type, InputStream stream) {
        try {
            return mapper.readValue(stream, type);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T> T read(Class<T> type, byte[] json) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String writeString(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void write(Object object, OutputStream os) {
        try {
            mapper.writeValue(os, object);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void writeFormatted(Object object, OutputStream os) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(os, object);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
