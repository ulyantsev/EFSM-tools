/* 
 * Developed by eVelopers Corporation, 2009
 */
package ru.ifmo.ctddev.genetic.transducer.io;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import java.io.*;
import java.net.URLClassLoader;
import java.net.URL;
import java.util.*;

import ru.ifmo.ctddev.genetic.transducer.algorithm.Parameters;
import ru.ifmo.ctddev.genetic.transducer.scenario.Path;
import ru.ifmo.ctddev.genetic.transducer.scenario.TestGroup;

/**
 * @author kegorov
 *         Date: Jun 18, 2009
 */
public class TestsReader implements ITestsReader {
    private final static String SCHEMA_LOCATION = "program.xsd";

    private enum Tag {
        PROGRAM("program"),
        INPUT_SET("inputSet"),
        OUTPUT_SET("outputSet"),
        GROUP("group"),
        FORMULAS("formulas"),
        TESTS("tests"),
        LTL("ltl"),
        TEST("test"),
        PTEST("ptest"),
        NTEST("ntest"),
        INPUT("input"),
        OUTPUT("output"),
        PARAMETERS("parameters"),
        FIXED_OUTPUT("fixedOutput"),
        POPULATION_SIZE("populationSize"),
        DESIRED_FITNESS("desiredFitness"),
	    GENERATION_SIZE("generationSize"),
        STATE_NUMBER("stateNumber"),
        PART_STAY("partStay"),
        TIME_SMALL_MUTATION("timeSmallMutation"),
        TIME_BIG_MUTATION("timeBigMutation"),
        MUTATION_PROBABILITY("mutationProbability");

        private String element;

        private Tag(String element) {
            this.element = element;
        }

        public String getName() {
            return element;
        }
    }

    private List<TestGroup> groups;
    private String[] setOfInputs;
    private String[] setOfOutputs;
    private Set<String> inputs = new HashSet<String>();
    private Set<String> outputs = new HashSet<String>();
    private Parameters parameters;

    public TestsReader(File file) throws IOException, FileFormatException {
        this(file, true);
    }

    public TestsReader(File file, boolean validate) throws IOException, FileFormatException {
        InputStream input = new BufferedInputStream(new FileInputStream(file));
        try {
            readAll(input, validate);
        } finally {
            input.close();
        }
    }

    public List<TestGroup> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    public String[] getSetOfInputs() {
        return setOfInputs;
    }

    public String[] getSetOfOutputs() {
        return setOfOutputs;
    }

    public Parameters getAlgorithmParameters() {
        return parameters;
    }

    protected void readAll(InputStream input, boolean validate) throws IOException, FileFormatException {
        try {
            DocumentBuilder builder = createDocumentBuider();
            Document document = builder.parse(input);

            if (validate) {
                //validate document
                URLClassLoader urlLoader = (URLClassLoader) getClass().getClassLoader();
                URL schemaLoc = urlLoader.findResource(SCHEMA_LOCATION);
                SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = schemaFactory.newSchema(schemaLoc);
                Validator validator = schema.newValidator();
                // check DOM-tree
                validator.validate(new DOMSource(document));
            }
            parameters = readParameters(document);
            inputs = readInputs(document);
            outputs = readOutputs(document);
            groups = readGroups(document);
            initIOs();
        } catch (ParserConfigurationException e) {
            throw new FileFormatException("Can't configure xml parser", e);
        } catch (SAXException e) {
            throw new FileFormatException("Parse exception", e);
        }
    }

    private void initIOs() {
        setOfInputs = inputs.toArray(new String[inputs.size()]);
        setOfOutputs = outputs.toArray(new String[outputs.size()]);
    }

    protected Parameters readParameters(Document document) {
        NodeList list = document.getElementsByTagName(Tag.PARAMETERS.getName());
        if ((list == null) || (list.getLength() == 0)) {
            return null;
        }
        Element e = (Element) list.item(0);
        Parameters p = new Parameters();

        p.setFixedOutput(Boolean.parseBoolean(parseSimpleElement(e, Tag.FIXED_OUTPUT)));
        p.setPopulationSize(Integer.parseInt(parseSimpleElement(e, Tag.POPULATION_SIZE)));
        p.setDesiredFitness(Double.parseDouble(parseSimpleElement(e, Tag.DESIRED_FITNESS)));
        p.setMutationProbability(Double.parseDouble(parseSimpleElement(e, Tag.MUTATION_PROBABILITY)));
        p.setPartStay(Double.parseDouble(parseSimpleElement(e, Tag.PART_STAY)));
        p.setStateNumber(Integer.parseInt(parseSimpleElement(e, Tag.STATE_NUMBER)));
        p.setTimeBigMutation(Integer.parseInt(parseSimpleElement(e, Tag.TIME_BIG_MUTATION)));
        p.setTimeSmallMutation(Integer.parseInt(parseSimpleElement(e, Tag.TIME_SMALL_MUTATION)));

        return p;
    }

    protected Set<String> readInputs(Document document) {
        return readSet(document, Tag.INPUT_SET);
    }

    protected Set<String> readOutputs(Document document) {
        return readSet(document, Tag.OUTPUT_SET);
    }

    protected Set<String> readSet(Document document, Tag tag) {
        String str = parseSimpleElement(document.getDocumentElement(), tag);
        String[] arr = str.split(",");
        Set<String> set = new HashSet<String>();

        for (String e : arr) {
            set.add(e.trim());
        }
        return set;
    }

    protected List<TestGroup> readGroups(Document document) {
        NodeList nodeList = document.getElementsByTagName(Tag.GROUP.getName());
        if (nodeList == null) {
            return Collections.emptyList();
        }

        List<TestGroup> groups = new ArrayList<TestGroup>(nodeList.getLength());

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element e = (Element) nodeList.item(i);
            List<Path> tests = readTests(e);
            List<Path> nTests = readNegativeTests(e);
            List<String> formulas = readFormulas(e);

            TestGroup g = new TestGroup(tests, nTests, formulas);
            groups.add(g);
        }

        return groups;
    }

    protected List<Path> readTests(Element element) {
        NodeList nodeList = element.getElementsByTagName(Tag.TEST.getName());
        if (nodeList == null) {
            return Collections.emptyList();
        }
        List<Path> tests = new ArrayList<Path>(nodeList.getLength());

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element e = (Element) nodeList.item(i);
            Path p;
            if (parameters.isFixedOutput()) {
                String ptest = parseSimpleElement(e, Tag.PTEST);
                p = parsePositiveTest(ptest);
            } else {
                String in = parseSimpleElement(e, Tag.INPUT);
                String out = parseSimpleElement(e, Tag.OUTPUT);
                p = new Path();
    
                p = parseInput(p, in);
                p = parseOutput(p, out);
            }
            
			tests.add(p);
        }

        return tests;
    }
    
    private Path parsePositiveTest(String test) {
        Path p = new Path();
        
        for (String t : test.split(";")) {
            int slash = t.indexOf('/');
            String e = (slash >= 0) ? t.substring(0, slash).trim() : t.trim();
            
            //validate input
            if (!inputs.contains(e)) {
                throw new RuntimeException("Unexpected input: " + e);
            }
            p = p.appendInput(e);
            if ((slash >=0) && (slash < t.length())) {
                String actions = (t.substring(slash + 1)).replaceAll("\\s", "");
                
                //validate outputs
                String[] output = actions.split(",");
                for (String a : output) {
                    if (!outputs.contains(a)) {
                        throw new RuntimeException("Unexpected output: " + a);
                    }
                }
                p = p.appendFixedOutput(actions.isEmpty() ? null : actions);
            } else {
                p = p.appendFixedOutput(null);
            }
        }
        
        assert (p.getInput().length == p.getFixedOutput().length);
        
        return p;
    }

    private Path parseInput(Path p, String in) {
        String[] input = in.split(",");

        for (String st : input) {
            st = st.trim();
            if (!inputs.contains(st)) {
                throw new RuntimeException("Unexpected input: " + st);
            }
            p = p.appendInput(st);
        }

        return p;
    }

    private Path parseOutput(Path p, String out) {
        String[] output = out.split(",");

        for (String st : output) {
            st = st.trim();
            if (!outputs.contains(st)) {
                throw new RuntimeException("Unexpected output: " + st);
            }
            p = p.appendOutput(st);
        }
        return p;
    }

    protected List<Path> readNegativeTests(Element element) {
        NodeList nodeList = element.getElementsByTagName(Tag.NTEST.getName());
        if (nodeList == null) {
            return Collections.emptyList();
        }
        List<Path> nTests = new ArrayList<Path>(nodeList.getLength());

        for (int i = 0; i < nodeList.getLength(); i++) {
            Path p = new Path();

            p = parseInput(p, nodeList.item(i).getTextContent());
            nTests.add(p);
        }

        return nTests;
    }

    protected List<String> readFormulas(Element element) {
        NodeList nodeList = element.getElementsByTagName(Tag.LTL.getName());
        if (nodeList == null) {
            return Collections.emptyList();
        }
        List<String> formulas = new ArrayList<String>(nodeList.getLength());

        for (int i = 0; i < nodeList.getLength(); i++) {
            String formula = nodeList.item(i).getTextContent();
            formulas.add(formula);
        }

        return formulas;
    }

    private String parseSimpleElement(Element element, Tag tag) {
        return element.getElementsByTagName(tag.getName()).item(0).getTextContent();
    }

    private DocumentBuilder createDocumentBuider() throws ParserConfigurationException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);

        return factory.newDocumentBuilder();
    }
}
