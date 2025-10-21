package app.cinematch.util;

/** Test stub pour capter les appels sans I/O réelle. */
public class JsonStorage {
    public static String lastTitle;
    public static String lastStatus;

    public static void addOrUpdate(String title, String status) {
        lastTitle = title;
        lastStatus = status;
    }

    public static void reset() {
        lastTitle = null;
        lastStatus = null;
    }
}
