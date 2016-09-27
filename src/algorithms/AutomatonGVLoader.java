package algorithms;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bool.MyBooleanExpression;
import scenario.StringActions;
import structures.mealy.MealyAutomaton;
import structures.mealy.MealyNode;
import structures.mealy.MealyTransition;

public class AutomatonGVLoader {
    private static String readFileAsString(String filePath) throws IOException {
        byte[] buffer = new byte[(int) new File(filePath).length()];
        try (BufferedInputStream f = new BufferedInputStream(new FileInputStream(filePath))) {
            f.read(buffer);
        }
        return new String(buffer);
    }
    
    public static MealyAutomaton load(String fp) throws IOException, ParseException {
        String expr = "(\\d+) ?-> ?(\\d+) ?\\[label ?= ?\" ?(\\w+) ?\\[(.+)\\] \\((.*)\\) ?\"\\];";
        Pattern strPattern = Pattern.compile(expr);
        
        String target = readFileAsString(fp);
        Matcher matcher = strPattern.matcher(target);
        
        int maxNum = 0;
        List<Integer> srcList = new ArrayList<>();
        List<Integer> dstList = new ArrayList<>();
        List<String> eventsList = new ArrayList<>();
        List<MyBooleanExpression> guardConditionsList = new ArrayList<>();
        List<StringActions> actionsList= new ArrayList<>();
        
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
        
        MealyAutomaton res = new MealyAutomaton(maxNum + 1);
        for (int i = 0; i < srcList.size(); i++) {
            MealyNode src = res.state(srcList.get(i)), dst = res.state(dstList.get(i));
            MealyTransition transition = new MealyTransition(src, dst, eventsList.get(i), guardConditionsList.get(i),
                    actionsList.get(i));
            res.addTransition(src, transition);
        }
        
        return res;
    }
}
