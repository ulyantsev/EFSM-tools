/* 
 * Developed by eVelopers Corporation, 2009
 */
package ru.ifmo.ctddev.genetic.transducer.io;

import ru.ifmo.ctddev.genetic.transducer.scenario.TestGroup;
import ru.ifmo.ctddev.genetic.transducer.scenario.Path;


import java.util.*;
import java.io.*;

import org.w3c.dom.Document;

/**
 * @author kegorov
 *         Date: Jul 6, 2009
 */
public class OneGroupTestsReader extends TestsReader {

    public OneGroupTestsReader(File file) throws IOException, FileFormatException {
        super(file, true);
    }

    public OneGroupTestsReader(File file, boolean validate) throws IOException, FileFormatException {
        super(file, validate);
    }

    protected List<TestGroup> readGroups(Document document) {
        List<TestGroup> tmp = super.readGroups(document);

        List<Path> tests = new ArrayList<Path>();
        List<Path> nTests = new ArrayList<Path>();
        List<String> formulas = new ArrayList<String>();

        for (TestGroup g : tmp) {
            tests.addAll(g.getTests());
            nTests.addAll(g.getNegativeTests());
            formulas.addAll(g.getFormulas());
        }

        return Collections.singletonList(new TestGroup(tests, nTests, formulas));
    }
}