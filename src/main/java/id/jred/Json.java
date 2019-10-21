package id.jred;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class Json {
    private Json() {}

    public static final ObjectMapper mapper = new ObjectMapper();

    public static <T> T read(Class<T> type, InputStream stream)
            throws IOException {
        return mapper.readValue(stream, type);
    }

    public static <T> T read(Class<T> type, byte[] json)
            throws IOException {
        return mapper.readValue(json, type);
    }

    public static String writeString(Object object)
            throws IOException {
        return mapper.writeValueAsString(object);
    }

    public static void write(Object object, OutputStream os)
            throws IOException {
        mapper.writeValue(os, object);
    }

    public static void writeFormatted(Object object, OutputStream os)
            throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(os, object);
    }
}
