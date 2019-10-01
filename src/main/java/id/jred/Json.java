package id.jred;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
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

    public static OutputStream write(Object object, OutputStream os) {
        try {
            mapper.writeValue(os, object);
            return os;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String write(Object object) {
        return write(object, new ByteArrayOutputStream()).toString();
    }
}
