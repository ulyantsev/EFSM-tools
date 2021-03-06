package bnf_formulae;

/**
 * (c) Igor Buzhinsky
 */

import sat_solving.Assignment;
import sat_solving.SatSolver;
import sat_solving.SolverResult;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public abstract class BooleanFormula {
    public static final BooleanFormula TRUE = new BooleanFormula() {
        @Override
        public String toLimbooleString() {
            throw new AssertionError();
        }

        @Override
        public String toString() {
            return "TRUE";
        }

        @Override
        public BooleanFormula multipleSubstitute(Map<BooleanVariable, BooleanFormula> replacement) {
            return this;
        }

        @Override
        public BooleanFormula simplify() {
            return this;
        }
    };

    public static final BooleanFormula FALSE = new BooleanFormula() {
        @Override
        public String toLimbooleString() {
            throw new AssertionError();
        }

        @Override
        public String toString() {
            return "FALSE";
        }

        @Override
        public BooleanFormula multipleSubstitute(Map<BooleanVariable, BooleanFormula> replacement) {
            return this;
        }

        @Override
        public BooleanFormula simplify() {
            return this;
        }
    };

    static Optional<Assignment> fromDimacsToken(String token, DimacsConversionInfo dimacs) {
        final boolean isTrue = token.charAt(0) != '-';
        if (!isTrue) {
            token = token.substring(1);
        }
        int dimacsIndex = Integer.parseInt(token);
        Optional<Integer> limbooleIndex = dimacs.toLimbooleNumber(dimacsIndex);
        return limbooleIndex.map(index -> new Assignment(BooleanVariable.getVarByNumber(index), isTrue));
    }
    
    public static final String DIMACS_FILENAME = "_tmp.dimacs";
    
    public static class SolveAsSatResult {
        private final List<Assignment> list;
        public final long time;
        public final DimacsConversionInfo info;
        
        public List<Assignment> list() {
            return Collections.unmodifiableList(list);
        }
        
        SolveAsSatResult(List<Assignment> list, long time, DimacsConversionInfo info) {
            this.list = list;
            this.time = time;
            this.info = info;
        }

        public SolverResult toSolverResult(int secondsLeft) {
            return list.isEmpty()
                    ? new SolverResult(time >= secondsLeft * 1000
                    ? SolverResult.SolverResults.UNKNOWN : SolverResult.SolverResults.UNSAT)
                    : new SolverResult(list);
        }
    }
    
    private static String fixedSizedDimacsHeader(int varNum, int clauseNum) {
        final int numLength = 10;
        final StringBuilder varStr = new StringBuilder(String.valueOf(varNum));
        while (varStr.length() < numLength) {
            varStr.append(" ");
        }
        final StringBuilder clauseStr = new StringBuilder(String.valueOf(clauseNum));
        while (clauseStr.length() < numLength) {
            clauseStr.append(" ");
        }
        return "p cnf " + varStr + " " + clauseStr;
    }
    
    public static void appendConstraintsToDimacs(Logger logger,
            List<String> newClauses, DimacsConversionInfo info) throws IOException {
        int oldClauseNum;
        final File file = new File(DIMACS_FILENAME);
        try (final BufferedReader input = new BufferedReader(new FileReader(file))) {
            final String[] tokens = input.readLine().split(" +");
            assert tokens.length == 4;
            oldClauseNum = Integer.parseInt(tokens[3]);
        }
        final int newClauseNum = oldClauseNum + newClauses.size();
        final int newVarNum = info.varNumber();

        // modify the header
        final String header = fixedSizedDimacsHeader(newVarNum, newClauseNum);
        final byte[] headerBytes = header.getBytes();
        try (final RandomAccessFile f = new RandomAccessFile(file, "rw")) {
            f.write(headerBytes, 0, headerBytes.length);
        }
        
        // append new clauses
        try (final PrintWriter pw = new PrintWriter(new FileOutputStream(file, true))) {
            newClauses.forEach(pw::println);
        }

        logger.info("ALTERED DIMACS FILE");
    }
    
    // modifies the info and returns new constraints
    public static List<String> extendDimacs(String limbooleFormula, Logger logger,
            String tmpDimacsFilename, DimacsConversionInfo oldInfo) throws IOException {
        final List<String> result = new ArrayList<>();
        try (DimacsConversionInfo newInfo = toDimacs(limbooleFormula, logger, tmpDimacsFilename)) {
            final List<String> newDimacsLines = new ArrayList<>();
            try (BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(tmpDimacsFilename)))) {
                input.lines().forEach(newDimacsLines::add);
            }

            final Map<Integer, Integer> varMapping = new HashMap<>();
            varMapping.put(0, 0);
            // map primary vars to old numbers
            for (Map.Entry<Integer, Integer> entry : newInfo.dimacsNumberToLimboole.entrySet()) {
                final int dimacsBeforeMapping = entry.getKey();
                final int limboole = entry.getValue();
                final Integer oldDimacsValue = oldInfo.limbooleNumberToDimacs.get(limboole);
                if (oldDimacsValue != null) {
                    // old primary var
                    varMapping.put(dimacsBeforeMapping, oldDimacsValue);
                } else {
                    // new primary var
                    oldInfo.varNumber++;
                    varMapping.put(dimacsBeforeMapping, oldInfo.varNumber);
                    oldInfo.dimacsNumberToLimboole.put(oldInfo.varNumber, limboole);
                    oldInfo.limbooleNumberToDimacs.put(limboole, oldInfo.varNumber);
                }
            }
            // map auxiliary vars to new values
            for (int i = 1; i <= newInfo.varNumber(); i++) {
                if (!newInfo.dimacsNumberToLimboole.containsKey(i)) {
                    oldInfo.varNumber++;
                    varMapping.put(i, oldInfo.varNumber);
                }
            }

            for (String line : newDimacsLines) {
                if (line.startsWith("p")) {
                    continue;
                }
                final String[] tokens = line.split(" ");
                assert tokens[tokens.length - 1].equals("0");
                final List<String> assList = new ArrayList<>();
                for (String token : tokens) {
                    boolean isPositive = !token.startsWith("-");
                    if (!isPositive) {
                        token = token.substring(1);
                    }
                    final int var = Integer.parseInt(token);
                    final int mappedVar = varMapping.get(var);
                    assList.add((isPositive ? "" : "-") + mappedVar);
                }
                result.add(String.join(" ", assList));
            }
        }
        return result;
    }
    
    private static int SOLVER_SEED = 0;
    
    public static SolveAsSatResult solveDimacs(Logger logger, int timeoutSeconds, SatSolver solver,
            DimacsConversionInfo info) throws IOException {
        long time = System.currentTimeMillis();
        final Map<String, Assignment> list = new LinkedHashMap<>();
        String solverParams = "";
        if (solver == SatSolver.LINGELING) {
            solverParams = " --seed=" + SOLVER_SEED;
        } else if (solver == SatSolver.CRYPTOMINISAT) {
            solverParams = " --random=" + SOLVER_SEED;
        }
        timeoutSeconds = Math.max(1, timeoutSeconds);
        SOLVER_SEED++;
        final String solverStr = solver.command + timeoutSeconds + " " + solverParams + " " + DIMACS_FILENAME;
        logger.info(solverStr);
        final Process p = Runtime.getRuntime().exec(solverStr);

        try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            input.lines().filter(s -> s.startsWith("v")).forEach(certificateLine ->
                Arrays.stream(certificateLine.split(" ")).skip(1).forEach(token ->
                    fromDimacsToken(token, info).ifPresent(ass -> list.put(ass.var.name, ass))
                )
            );
        }

        time = System.currentTimeMillis() - time;
        return new SolveAsSatResult(new ArrayList<>(list.values()), time, info);
    }
    
    public static SolveAsSatResult solveAsSat(String formula, Logger logger,
            int timeoutSeconds, SatSolver solver) throws IOException {
        logger.info("Final SAT formula length: " + formula.length());
        final DimacsConversionInfo info = BooleanFormula.toDimacs(formula, logger, DIMACS_FILENAME);
        info.close();
        logger.info("CREATED DIMACS FILE");
        return solveDimacs(logger, timeoutSeconds, solver, info);
    }
    
    public static DimacsConversionInfo actionSpecToDimacs(Logger logger, String dimacsFilename,
            String actionSpec) throws IOException {
        final BooleanVariable v = BooleanVariable.getVarByNumber(1);
        final String limbooleString = actionSpec == null
                ? v.or(v.not()).toLimbooleString()
                : actionSpec;
        return toDimacs(limbooleString, logger, dimacsFilename);
    }

    public static void transformConstraints(List<int[]> cnfConstraints, DimacsConversionInfo info) {
        for (final int[] terms : cnfConstraints) {
            for (int j = 0; j < terms.length; j++) {
                final int term = terms[j];
                final int var = Math.abs(term);

                Integer transformedNum = info.limbooleNumberToDimacs.get(var);
                if (transformedNum == null) {
                    info.varNumber++;
                    info.limbooleNumberToDimacs.put(var, info.varNumber);
                    info.dimacsNumberToLimboole.put(info.varNumber, var);
                    transformedNum = info.varNumber;
                }
                terms[j] = (term < 0 ? -1 : 1) * transformedNum;
            }
        }
    }

    public static void appendConstraints(List<int[]> cnfConstraints, DimacsConversionInfo info,
                                         DataOutputStream constraintWriter) throws IOException {
        final int initialVarNumber = info.varNumber;
        transformConstraints(cnfConstraints, info);
        final int newVars = info.varNumber - initialVarNumber;
        if (newVars > 0) {
            constraintWriter.writeInt(1);
            constraintWriter.writeInt(newVars);
        }
        for (int[] constraint : cnfConstraints) {
            constraintWriter.writeInt(0);
            for (int i : constraint) {
                constraintWriter.writeInt(i);
            }
            constraintWriter.writeInt(0);
        }
    }
    
    public static class DimacsConversionInfo implements AutoCloseable {
        private final Map<Integer, Integer> limbooleNumberToDimacs = new HashMap<>();
        private final Map<Integer, Integer> dimacsNumberToLimboole = new HashMap<>();
        private String title;
        private Integer varNumber;
        private PrintWriter pw;
        
        DimacsConversionInfo(String filename) throws FileNotFoundException {
             pw = new PrintWriter(new File(filename));
        }
        
        void acceptLine(String line) {
            if (line.startsWith("c")) {
                String[] tokens = line.split(" ");
                assert tokens.length == 3;
                int first = Integer.parseInt(tokens[1]);
                int second = Integer.parseInt(tokens[2]);
                assert !limbooleNumberToDimacs.containsKey(second);
                assert !dimacsNumberToLimboole.containsKey(first);
                limbooleNumberToDimacs.put(second, first);
                dimacsNumberToLimboole.put(first, second);
            } else if (line.startsWith("p")) {
                title = line;
                String[] tokens = line.split(" ");
                assert tokens.length == 4;
                varNumber = Integer.parseInt(tokens[2]);
                final int clauseNumber = Integer.parseInt(tokens[3]);
                pw.println(fixedSizedDimacsHeader(varNumber, clauseNumber));
            } else {
                pw.println(line);
            }
        }
        
        String title() {
            assert title != null;
            return title;
        }
        
        Optional<Integer> toDimacsNumber(int num) {
            return Optional.of(limbooleNumberToDimacs.get(num));
        }
        
        public int varNumber() {
            return varNumber;
        }
        
        public Optional<Integer> toLimbooleNumber(int num) {
            return Optional.ofNullable(dimacsNumberToLimboole.get(num));
        }

        @Override
        public void close() {
            pw.close();
        }
    }
    
    public abstract String toLimbooleString();
    
    private static DimacsConversionInfo toDimacs(String limbooleFormula, Logger logger, String dimacsFilename)
            throws IOException {
        final String beforeLimbooleFilename = "_tmp.limboole";
        final String afterLimbooleFilename = "_tmp.after.limboole.dimacs";
        
        try (PrintWriter pw = new PrintWriter(beforeLimbooleFilename)) {
            pw.print(limbooleFormula);
        }
        
        final String limbooleStr = "limboole -d -s -o " + afterLimbooleFilename + " " + beforeLimbooleFilename;
        logger.info(limbooleStr);
        final Process limboole = Runtime.getRuntime().exec(limbooleStr);
        try {
            limboole.waitFor();
        } catch (InterruptedException e) {
            throw new AssertionError();
        }
        
        final DimacsConversionInfo info = new DimacsConversionInfo(dimacsFilename);
        try (BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(afterLimbooleFilename)))) {
            input.lines().forEach(info::acceptLine);
        }
        info.close();

        return info;
    }
    
    DimacsConversionInfo toDimacs(Logger logger, String dimacsFilename) throws IOException {
        return toDimacs(simplify().toLimbooleString(), logger, dimacsFilename);
    }
    
    public BooleanFormula not() {
        return new NotOperation(this);
    }
    
    public BooleanFormula and(BooleanFormula other) {
        return BinaryOperation.and(this, other);
    }
    
    public BooleanFormula or(BooleanFormula other) {
        return BinaryOperation.or(this, other);
    }
    
    public BooleanFormula implies(BooleanFormula other) {
        return BinaryOperation.implies(this, other);
    }
    
    public BooleanFormula equivalent(BooleanFormula other) {
        return BinaryOperation.equivalent(this, other);
    }
    
    public abstract BooleanFormula multipleSubstitute(Map<BooleanVariable, BooleanFormula> replacement);
    
    /*
     * Removes TRUE and FALSE.
     */
    public abstract BooleanFormula simplify();

    @Override
    public boolean equals(Object other) {
        return other.getClass() == getClass() && toString().equals(other.toString());
    }
    
    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
