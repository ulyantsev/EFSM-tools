/**
 * LtlParser.java, 06.04.2008
 */
package qbf.ltl;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import qbf.ognl.GrammarConverter;
import qbf.ognl.Node;
import qbf.ognl.Ognl;
import qbf.ognl.OgnlException;

/**
 * The ILtlparser implementation that use Ognl library
 *
 * @author: Kirill Egorov, Igor Buzhinsky
 */
public class LtlParser {
    public static LtlNode parse(String ltlExpr) throws LtlParseException {
        try {
            return GrammarConverter.convert((Node) Ognl.parseExpression(ltlExpr));
        } catch (OgnlException e) {
            throw new LtlParseException(e);
        }
    }
    
    public static List<LtlNode> loadProperties(String filepath) throws ParseException, FileNotFoundException, LtlParseException {
        List<LtlNode> ans = new ArrayList<>();

        try (Scanner in = new Scanner(new File(filepath))) {
	        while (in.hasNextLine()) {
	        	String input = in.nextLine().trim();
	            if (input.startsWith("#")) {
	            	// comment
	            	continue;
	            }
	            if (input.equals("")) {
	            	continue;
	            }
	            input = input.replaceAll("->", ">>");
	            ans.add(parse(input));
	        }
        }
        return ans;
    }
}
