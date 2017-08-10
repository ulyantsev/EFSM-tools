package continuous_trace_builders;

import java.util.Arrays;
import java.nio.file.Paths;
import java.io.*;

public class Utils {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    static String[] splitString(String str) {
        if (str.trim().equals("")) {
            return EMPTY_STRING_ARRAY;
        }
        String[] temp = str.split(" +");
        int start = temp[0].equals("") ? 1 : 0;
        int len = temp.length;
        int end = temp[len - 1].equals("") ? len - 1 : len;
        return Arrays.copyOfRange(temp, start, end);
    }

    public static String combinePaths(String... paths) {
        return Paths.get("", paths).toString();
    }

    static void writeToFile(String path, String text) throws FileNotFoundException {
        try (PrintWriter pw = new PrintWriter(path)) {
            pw.print(text);
        }
    }

    static boolean deleteDir(File dir) {
        final File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    if (!deleteDir(f)) {
                        return false;
                    }
                } else if (!f.delete()) {
                    return false;
                }
            }
        }
        return dir.delete();
    }
}
