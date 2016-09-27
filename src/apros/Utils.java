package apros;

import java.util.Arrays;

class Utils {
    private static final String[] emptyStringArray = new String[0];

    static String[] splitString(String str) {
        if (str.trim().equals("")) {
            return emptyStringArray;
        }
        String[] temp = str.split(" +");
        int start = temp[0].equals("") ? 1 : 0;
        int len = temp.length;
        int end = temp[len - 1].equals("") ? len - 1 : len;
        return Arrays.copyOfRange(temp, start, end);
    }
}
