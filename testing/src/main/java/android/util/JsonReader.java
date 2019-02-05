package android.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

/**
 * Re-implementation of a streaming parser from android.util on top of Gson.
 */
public final class JsonReader implements Closeable {

    private final com.google.gson.stream.JsonReader gson;

    public JsonReader(Reader in) {
        gson = new com.google.gson.stream.JsonReader(in);
    }

    public void setLenient(boolean lenient) {
        gson.setLenient(lenient);
    }

    public boolean isLenient() {
        return gson.isLenient();
    }

    public void beginArray() throws IOException {
        gson.beginArray();
    }

    public void endArray() throws IOException {
        gson.endArray();
    }

    public void beginObject() throws IOException {
        gson.beginObject();
    }

    public void endObject() throws IOException {
        gson.endObject();
    }

    public boolean hasNext() throws IOException {
        return gson.hasNext();
    }

    public JsonToken peek() throws IOException {
        return JsonToken.valueOf(gson.peek().name());
    }

    public String nextName() throws IOException {
        return gson.nextName();
    }

    public String nextString() throws IOException {
        return gson.nextString();
    }

    public boolean nextBoolean() throws IOException {
        return gson.nextBoolean();
    }

    public void nextNull() throws IOException {
        gson.nextNull();
    }

    public double nextDouble() throws IOException {
        return gson.nextDouble();
    }

    public long nextLong() throws IOException {
        return gson.nextLong();
    }

    public int nextInt() throws IOException {
        return gson.nextInt();
    }

    public void close() throws IOException {
        gson.close();
    }

    public void skipValue() throws IOException {
        gson.skipValue();
    }

}
