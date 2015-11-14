package algorithms;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bool.MyBooleanExpression;
import scenario.StringActions;
import structures.Automaton;
import structures.Node;
import structures.Transition;

public class AutomatonGVLoader {
    private static String readFileAsString(String filePath) throws IOException {
        byte[] buffer = new byte[(int) new File(filePath).length()];
        try (BufferedInputStream f = new BufferedInputStream(new FileInputStream(filePath))) {
        	f.read(buffer);
        }
        return new String(buffer);
    }
    
    public static Automaton load(String fp) throws IOException, ParseException {
        String expr = "(\\d+) ?-> ?(\\d+) ?\\[label ?= ?\" ?(\\w+) ?\\[(.+)\\] \\((.*)\\) ?\"\\];";
        Pattern strPattern = Pattern.compile(expr);
        
        String target = readFileAsString(fp);
        Matcher matcher = strPattern.matcher(target);
        
        int maxNum = 0;
        List<Integer> srcList = new ArrayList<Integer>();
        List<Integer> dstList = new ArrayList<Integer>();
        List<String> eventsList = new ArrayList<String>();
        List<MyBooleanExpression> guardConditionsList = new ArrayList<MyBooleanExpression>();
        List<StringActions> actionsList= new ArrayList<StringActions>();
        
        while (matcher.find()) {
            int srcNum = Integer.parseInt(matcher.group(1));
            srcList.add(srcNum);
            int dstNum = Integer.parseInt(matcher.group(2));
            dstList.add(dstNum);
            maxNum = Math.max(maxNum, Math.max(srcNum, dstNum));
            
            String event = matcher.group(3);
            eventsList.add(event);
            MyBooleanExpression condition = MyBooleanExpression.get(matcher.group(4));
            guardConditionsList.add(condition);
            StringActions actions = new StringActions(matcher.group(5));
            actionsList.add(actions);
        }
        
        Automaton res = new Automaton(maxNum + 1);
        for (int i = 0; i < srcList.size(); i++) {
            Node src = res.state(srcList.get(i)), dst = res.state(dstList.get(i));
            Transition transition = new Transition(src, dst, eventsList.get(i), guardConditionsList.get(i), actionsList.get(i));
            res.addTransition(src, transition);
        }
        
        return res;
    }
}
