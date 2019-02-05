package android.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;

/**
 * Re-implementation of a JSON streamer from android.util on top of Gson.
 */
public final class JsonWriter implements Closeable {

    private final com.google.gson.stream.JsonWriter gson;

    public JsonWriter(Writer out) {
        gson = new com.google.gson.stream.JsonWriter(out);
    }

    public void setIndent(String indent) {
        gson.setIndent(indent);
    }

    public void setLenient(boolean lenient) {
        gson.setLenient(lenient);
    }

    public boolean isLenient() {
        return gson.isLenient();
    }

    public JsonWriter beginArray() throws IOException {
        gson.beginArray();
        return this;
    }

    public JsonWriter endArray() throws IOException {
        gson.endArray();
        return this;
    }

    public JsonWriter beginObject() throws IOException {
        gson.beginObject();
        return this;
    }

    public JsonWriter endObject() throws IOException {
        gson.endObject();
        return this;
    }

    public JsonWriter name(String name) throws IOException {
        gson.name(name);
        return this;
    }

    public JsonWriter value(String value) throws IOException {
        gson.value(value);
        return this;
    }

    public JsonWriter nullValue() throws IOException {
        gson.nullValue();
        return this;
    }

    public JsonWriter value(boolean value) throws IOException {
        gson.value(value);
        return this;
    }

    public JsonWriter value(double value) throws IOException {
        gson.value(value);
        return this;
    }

    public JsonWriter value(long value) throws IOException {
        gson.value(value);
        return this;
    }

    public JsonWriter value(Number value) throws IOException {
        gson.value(value);
        return this;
    }

    public void flush() throws IOException {
        gson.flush();
    }

    public void close() throws IOException {
        gson.close();
    }

}
