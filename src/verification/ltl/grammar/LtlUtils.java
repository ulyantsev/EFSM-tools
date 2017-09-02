/**
 * ${NAME}.java, 22.03.2008
 */
package verification.ltl.grammar;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class LtlUtils {
    // expands things like event(A0, A1, A2) to (event(A0) || event(A1) || event(A2))
    public static String expandEventList(String input) {
        final Pattern p = Pattern.compile("event\\(((\\w+, *)+\\w+)\\)");
        final StringBuilder sb = new StringBuilder();
        final Matcher m = p.matcher(input);
        int lastPos = 0;
        while (m.find()) {
            final String events = m.group(1);
            sb.append(input.substring(lastPos, m.start()));
            final List<String> tokens = Arrays.stream(events.split(", *"))
                    .map(s -> "event(" + s + ")").collect(Collectors.toList());
            sb.append("(").append(String.join(" || ", tokens)).append(")");
            lastPos = m.end();
        }
        sb.append(input.substring(lastPos, input.length()));
        return sb.toString();
    }
}
