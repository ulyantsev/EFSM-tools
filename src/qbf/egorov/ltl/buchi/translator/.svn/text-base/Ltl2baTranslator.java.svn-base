/*
 * Developed by eVelopers Corporation - 26.05.2008
 */
package ru.ifmo.ltl.buchi.translator;

import ru.ifmo.ltl.buchi.ITranslator;
import ru.ifmo.ltl.buchi.IBuchiAutomata;
import ru.ifmo.ltl.buchi.ITransitionCondition;
import ru.ifmo.ltl.buchi.impl.*;
import ru.ifmo.ltl.grammar.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ognl.OgnlException;

/**
 * Ltl formula to automata Buchi translator.
 * Used Runtime.exec() to call Ltl2ba. Than parse result to construct automata Buchi.
 * <br>Visit <a href="http://www.lsv.ens-cachan.fr/~gastin/ltl2ba/index.php">LTL2BA site</a>
 * for more information about this library.
 */
public class Ltl2baTranslator implements ITranslator {
    private static final String PATH_WIN = "bin" + File.separator + "ltl2ba.exe";
    private static final String PATH_LINUX = "bin" + File.separator + "ltl2ba";
    private static final String ACCEPT_ALL = "accept_all";
    private static final String GOTO = "-> goto";

    private ExpressionMap expr = new ExpressionMap();
    private VisitorImpl visitor = new VisitorImpl();

    private final String execPath;

    public Ltl2baTranslator() {
        if (System.getProperty("os.name").startsWith("Windows")) {
            execPath = PATH_WIN;
        } else if (System.getProperty("os.name").startsWith("Linux")) {
            execPath = PATH_LINUX;
        } else {
            throw new RuntimeException("Unsupported OS");
        }
    }

    public Ltl2baTranslator(String execPath) {
        this.execPath = execPath;
    }

    public IBuchiAutomata translate(LtlNode root) {
        try {
            String formula = getFormula(root);
            String automata = executeLlt2ba(formula);

            //TODO: remove next line
            System.out.println(automata);

            return extractBuchi(automata);
        } catch (Exception e) {
            throw new TranslationException(e);
        }
    }

    protected IBuchiAutomata extractBuchi(String automata) {
        BuchiAutomata buchi = new BuchiAutomata();
        String init = getInitState(automata);
        List<String> states = getStates(automata);
        List<String> accept = getAcceptStates(automata);
        int idSeq = 0;

        Map<String, BuchiNode> map = new HashMap<String, BuchiNode>();
        Set<BuchiNode> acceptSet = new HashSet<BuchiNode>();
        BuchiNode initNode = new BuchiNode(idSeq++);

        map.put(extractName(init), initNode);
        if (init.startsWith("accept")) {
            //init state is accept state
            accept.remove(0);
            acceptSet.add(initNode);
        }
        for (String str: states) {
            map.put(extractName(str), new BuchiNode(idSeq++));
        }

        for (String str: accept) {
            BuchiNode bNode = new BuchiNode(idSeq++);
            map.put(extractName(str), bNode);
            acceptSet.add(bNode);
        }

        //TODO: check contains automata accept_all state or not
        BuchiNode acceptAll = new BuchiNode(idSeq++);
        map.put(ACCEPT_ALL, acceptAll);
        acceptSet.add(acceptAll);

        buchi.setStartNode(map.get(extractName(init)));
        buchi.addNodes(map.values());

        buchi.addAcceptSet(acceptSet);

        parseTransitions(map, init);
        for (String str: states) {
            parseTransitions(map, str);
        }
        for (String str: accept) {
            parseTransitions(map, str);
        }
        
        //create transition from accept_all to accept_all
        TransitionCondition cond = new TransitionCondition();
        cond.addExpression(BooleanNode.TRUE);
        acceptAll.addTransition(cond, acceptAll);

        return buchi;
    }

    protected void parseTransitions(Map<String, BuchiNode> map, String state) {
        BuchiNode node = map.get(extractName(state));

        Matcher m = Pattern.compile("::.*").matcher(state);
        List<String> trans = findAll(m, state);
        for (String t: trans) {
            int i = t.lastIndexOf("::");
            int j = t.indexOf(GOTO);
            if (i >= 0 && j >= 0) {
                ITransitionCondition cond = extractCondition(t.substring(i + 2, j));
                if (j < 0) {
                    throw new TranslationException("Unexpected transition format: " + t);
                } else {
                    String stateName = t.substring(j + GOTO.length() + 1);
                    node.addTransition(cond, map.get(stateName));
                }
            } else {
                throw new TranslationException("Unexpected transition format: " + t);
            }
        }
    }

    protected ITransitionCondition extractCondition(String condStr) {
//        TransitionCondition cond = new TransitionCondition();
        if (condStr.trim().equals("(1)")) {
            return new TransitionCondition();
        }
        try {
            return new OgnlTransitionCondition(condStr, expr);
        } catch (OgnlException e) {
            throw new TranslationException("Unexpected transition condition: " + condStr, e);
        }
//        String[] arr = condStr.split("&&");
//
//        for (String c: arr) {
//            c = c.trim();
//            if (c.charAt(0) == '!') {
//                cond.addNegExpression(expr.get(c.substring(1)));
//            } else {
//                cond.addExpression(expr.get(c));
//            }
//        }
//        return cond;
    }

    protected String extractName(String state) {
        return state.substring(0, state.indexOf(':'));
    }

    protected String getInitState(String automata) {
        Matcher m = Pattern.compile("\\w+_init:\\s+if(\\s+::.*)+\\s+fi;").matcher(automata);
        return (m.find()) ? automata.substring(m.start(), m.end()) : null;
    }

    protected List<String> getStates(String automata) {
        Matcher m = Pattern.compile("T\\d+_S\\d+:\\s+if(\\s+::.*)+\\s+fi;").matcher(automata);
        return findAll(m, automata);
    }

    protected List<String> getAcceptStates(String automata) {
        Matcher m = Pattern.compile("accept_\\w+:\\s+if(\\s+::.*)+\\s+fi;").matcher(automata);
        return findAll(m, automata);
    }

    protected List<String> findAll(Matcher m, String automata) {
        List<String> res = new ArrayList<String>();
        while (m.find()) {
            res.add(automata.substring(m.start(), m.end()));
        }
        return res;
    }

    public String executeLlt2ba(String formula) throws IOException, InterruptedException {
        Process proc = Runtime.getRuntime().exec(execPath + " -f \"" + formula + "\"");
        StreamReader reader = new StreamReader(proc.getInputStream());

        reader.start();
        proc.waitFor();
        reader.join();

        return reader.getResult();
    }

    public String getFormula(LtlNode root) {
        StringBuilder buf = new StringBuilder();
        convert(root, buf);
        return buf.toString();
    }

    private void convert(LtlNode node, StringBuilder buf) {
        if (node instanceof UnaryOperator) {
            buf.append(node.accept(visitor, null)).append('(');
            convert(((UnaryOperator) node).getOperand(), buf);
            buf.append(')');
        } else if (node instanceof BinaryOperator) {
            BinaryOperator op = (BinaryOperator) node;
            buf.append('(');
            convert(op.getLeftOperand(), buf);
            buf.append(')');
            buf.append(node.accept(visitor, null));
            buf.append('(');
            convert(op.getRightOperand(), buf);
            buf.append(')');
        } else if (node instanceof IExpression) {
            String tmp = node.accept(visitor, null);

            expr.put(tmp, (IExpression<Boolean>) node);
            buf.append(tmp);
        } else {
            throw new IllegalArgumentException("Unexpected node type: " + node.getClass());
        }
    }

    private class StreamReader extends Thread {
        private InputStream is;
        private StringWriter sw;

        StreamReader(InputStream is) {
            this.is = is;
            sw = new StringWriter();
        }

        public void run() {
            try {
                int c;
                while ((c = is.read()) != -1)
                    sw.write(c);
                }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        String getResult() {
            return sw.toString();
        }
    }

    /**
     * Use Spin syntax
     */
    private class VisitorImpl implements INodeVisitor<String, Void> {

        public String visitPredicate(Predicate p, Void aVoid) {
            return p.getUniqueName();
        }

        public String visitNeg(UnaryOperator op, Void aVoid) {
            return "!";
        }

        public String visitFuture(UnaryOperator op, Void aVoid) {
            return "<>";
        }

        public String visitNext(UnaryOperator op, Void aVoid) {
            return "X";
        }

        public String visitAnd(BinaryOperator op, Void aVoid) {
            return "&&";
        }

        public String visitOr(BinaryOperator op, Void aVoid) {
            return "||";
        }

        public String visitRelease(BinaryOperator op, Void aVoid) {
            return "V";
        }

        public String visitUntil(BinaryOperator op, Void aVoid) {
            return "U";
        }

        public String visitGlobal(UnaryOperator op, Void aVoid) {
            return "[]";
        }

        public String visitBoolean(BooleanNode b, Void aVoid) {
            return b.getValue().toString();
        }
    }
}
