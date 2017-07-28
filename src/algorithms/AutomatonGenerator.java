package algorithms;

import bool.MyBooleanExpression;
import scenario.StringActions;
import structures.mealy.MealyAutomaton;
import structures.mealy.MealyNode;
import structures.mealy.MealyTransition;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Generating a variable-complete automaton
 * 
 * @author ulyantsev
 * 
 */
public class AutomatonGenerator {
    public static MealyAutomaton generate(int statesCount, int eventsCount, int actionsCount, int minActions,
                                          int maxActions, int varsCount, double transitionsPersent, Random random) {

        assert 0 < eventsCount && eventsCount <= 26;
        List<String> events = new ArrayList<>();
        for (int i = 0; i < eventsCount; i++) {
            events.add("" + (char)('A' + i));
        }

        List<String> actions = new ArrayList<>();
        for (int i = 0; i < actionsCount; i++) {
            actions.add("z" + i);
        }

        List<String> vars = new ArrayList<>();
        for (int i = 0; i < varsCount; i++) {
            vars.add("x" + i);
        }

        return generate(statesCount, events, actions, minActions, maxActions, vars, transitionsPersent, random);
    }

    private static MealyAutomaton generate(int statesCount, List<String> events, List<String> actions, int minActions,
                                           int maxActions, List<String> vars, double transitionsPercentage,
                                           Random random) {

        assert 0.1 <= transitionsPercentage && transitionsPercentage <= 1;

        int eventsCount = events.size();
        int varsCount = vars.size();

        int maxTransitions = (int) (statesCount * eventsCount * Math.pow(2, varsCount));
        int transitionsCount = (int) (transitionsPercentage * maxTransitions);

        int[][] cnt = getTransitionsPowers(statesCount, eventsCount, varsCount, transitionsCount, random);
        int[][][] dst = getDestinations(statesCount, eventsCount, cnt, random);
        String[][][] expressions = getExpressions(statesCount, eventsCount, vars, cnt, random);
        String[][][] act = getActions(statesCount, eventsCount, minActions, maxActions, actions, cnt, random);

        MealyAutomaton ans = new MealyAutomaton(statesCount);
        for (int stateNum = 0; stateNum < statesCount; stateNum++) {
            for (int eventNum = 0; eventNum < eventsCount; eventNum++) {

                for (int i = 0; i < dst[stateNum][eventNum].length; i++) {
                    MealyNode srcNode = ans.state(stateNum);
                    MealyNode dstNode = ans.state(dst[stateNum][eventNum][i]);
                    String event = events.get(eventNum);
                    MyBooleanExpression expr = null;
                    try {
                        expr = MyBooleanExpression.get(expressions[stateNum][eventNum][i]);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    StringActions a = new StringActions(act[stateNum][eventNum][i]);

                    MealyTransition t = new MealyTransition(srcNode, dstNode, event, expr, a);
                    ans.addTransition(srcNode, t);
                }
            }
        }

        return ans;
    }

    private static int[][] getTransitionsPowers(int statesCount, int eventsCount, int varsCount, int transitionsCount,
            Random random) {

        int[] pow = new int[varsCount + 1];
        pow[0] = 1;
        for (int i = 1; i <= varsCount; i++) {
            pow[i] = pow[i - 1] * 2;
        }

        int[][] ans = new int[statesCount][eventsCount];
        for (int i = 0; i < statesCount; i++) {
            Arrays.fill(ans[i], varsCount);
        }

        int maxTransitions = statesCount * eventsCount * pow[varsCount];

        int[] stateSum = new int[statesCount];
        Arrays.fill(stateSum, eventsCount * pow[varsCount]);

        int sum = maxTransitions;
        while (sum > transitionsCount) {
            int state = random.nextInt(statesCount);
            int event = random.nextInt(eventsCount);
            if (ans[state][event] == -1) {
                continue;
            }

            int newVal = random.nextInt(varsCount + 2) - 1;
            if (newVal == -1 && stateSum[state] == pow[ans[state][event]]) {
                continue;
            }

            if (ans[state][event] > newVal) {
                int diff = pow[ans[state][event]];
                if (newVal > -1) {
                    diff -= pow[newVal];
                }
                sum -= diff;
                stateSum[state] -= diff;
                ans[state][event] = newVal;
            }
        }
        return ans;
    }

    /**
     * Assigning the edges. One edge always leads to the next state,
     * others are random
     *
     * @param statesCount
     * @param eventsCount
     * @param cnt
     * @param random
     * @return
     */
    private static int[][][] getDestinations(int statesCount, int eventsCount, int[][] cnt, Random random) {
        int[][][] ans = new int[statesCount][eventsCount][];
        for (int stateNum = 0; stateNum < statesCount; stateNum++) {
            for (int eventNum = 0; eventNum < eventsCount; eventNum++) {
                int size = 0;
                if (cnt[stateNum][eventNum] >= 0) {
                    size = (int) Math.pow(2, cnt[stateNum][eventNum]);
                }
                ans[stateNum][eventNum] = new int[size];
                Arrays.fill(ans[stateNum][eventNum], -1);
            }
        }

        for (int stateNum = 0; stateNum < statesCount; stateNum++) {
            boolean was = false;
            for (int eventNum = 0; eventNum < eventsCount; eventNum++) {
                for (int tr = 0; tr < ans[stateNum][eventNum].length; tr++) {
                    if (!was) {
                        ans[stateNum][eventNum][tr] = (stateNum + 1) % statesCount;
                        was = true;
                    } else {
                        int dst = random.nextInt(statesCount);
                        ans[stateNum][eventNum][tr] = dst;
                    }
                }
            }
        }

        return ans;
    }

    private static List<String> choice(List<String> c, int k, Random random) {
        List<String> ans = new ArrayList<>();

        boolean[] was = new boolean[c.size()];
        for (int i = 0; i < k; i++) {
            int pos = random.nextInt(c.size());
            while (was[pos]) {
                pos = random.nextInt(c.size());
            }
            ans.add(c.get(pos));
            was[pos] = true;
        }

        return ans;
    }

    private static String[][][] getExpressions(int statesCount, int eventsCount, List<String> vars, int[][] cnt,
            Random random) {
        String[][][] ans = new String[statesCount][eventsCount][];
        for (int stateNum = 0; stateNum < statesCount; stateNum++) {
            for (int eventNum = 0; eventNum < eventsCount; eventNum++) {
                int size = 0;
                if (cnt[stateNum][eventNum] >= 0) {
                    size = (int) Math.pow(2, cnt[stateNum][eventNum]);
                }
                ans[stateNum][eventNum] = new String[size];

                if (size == 0) {
                    continue;
                }
                if (size == 1) {
                    ans[stateNum][eventNum][0] = "1";
                    continue;
                }

                List<String> curVars = choice(vars, cnt[stateNum][eventNum], random);

                for (int mask = 0; mask < size; mask++) {
                    String s = "";
                    int i = mask;
                    for (int var = 0; var < cnt[stateNum][eventNum]; var++) {
                        if (var > 0) {
                            s += " & ";
                        }
                        if (i % 2 == 0) {
                            s += "~";
                        }

                        s += curVars.get(var);

                        i /= 2;
                    }
                    ans[stateNum][eventNum][mask] = s;
                }
            }
        }

        return ans;
    }

    private static String[][][] getActions(int statesCount, int eventsCount, int minActions, int maxActions,
            List<String> actions, int[][] cnt, Random random) {

        String[][][] ans = new String[statesCount][eventsCount][];
        for (int stateNum = 0; stateNum < statesCount; stateNum++) {
            for (int eventNum = 0; eventNum < eventsCount; eventNum++) {
                int size = 0;
                if (cnt[stateNum][eventNum] >= 0) {
                    size = (int) Math.pow(2, cnt[stateNum][eventNum]);
                }
                ans[stateNum][eventNum] = new String[size];

                for (int t = 0; t < size; t++) {
                    String s = "";
                    int actionsCount = minActions + random.nextInt(maxActions - minActions + 1);
                    for (int a = 0; a < actionsCount; a++) {
                        if (a > 0) {
                            s += ",";
                        }
                        s += actions.get(random.nextInt(actions.size()));
                    }
                    ans[stateNum][eventNum][t] = s;
                }
            }
        }

        return ans;
    }
}
