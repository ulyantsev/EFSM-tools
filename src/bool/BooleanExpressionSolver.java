/*
 * This file is part of Boolean Expression Solver. Boolean Expression Solver is
 * a program to build a truth table for a Boolean expression.
 * 
 * Copyright (C) 2007 Chris Bouchard
 * 
 * Boolean Expression Solver is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or (at your
 * option) any later version.
 * 
 * Boolean Expression Solver is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Boolean Expression Solver; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package bool;
import java.io.*;
import java.text.ParseException;
import java.util.*;

/**
 * A class to input Boolean expressions from standard input and display truth
 * tables of the expressions. Accepts the following command-line arguments:
 * 
 * <dl>
 * <dt><code>-b</code></dt>
 * <dt><code>--break</code></dt>
 * <dd>Break the expression into subexpressions.<br/><br/></dd>
 * 
 * <dt><code>-d</code> <i>var1</i><code>=</code><i>value1</i>[<code>,</code><i>var2</i><code>=</code><i>value2</i>...[<code>,</code><i>varN</i><code>=</code><i>valueN</i>]</dt>
 * <dt><code>--define</code> <i>var1</i><code>=</code><i>value1</i>[<code>,</code><i>var2</i><code>=</code><i>value2</i>...[<code>,</code><i>varN</i><code>=</code><i>valueN</i>]</dt>
 * <dd>Define the constants var1 through varN with the values var1 through
 * varN.<br/><br/></dd>
 * 
 * <dt><code>-e</code> <i>expression1</i>[ <i>expression2</i>...[
 * <i>expressionN</i>]]</dt>
 * <dt><code>--expressions</code> <i>expression1</i>[ <i>expression2</i>...[
 * <i>expressionN</i>]]</dt>
 * <dd>Build truth tables for the given expressions. Do not take input from
 * stdin. NOTE: This argument must come last as all remaining arguments will be
 * parsed as expressions.<br/><br/></dd>
 * 
 * <dt><code>-f</code> <i>filename</i></dt>
 * <dt><code>--help</code> <i>filename</i></dt>
 * <dd>Input from filename instead of standard input.<br/><br/></dd>
 * 
 * <dt><code>-h</code></dt>
 * <dt><code>--help</code></dt>
 * <dd>Display a message listing accepted arguments.<br/><br/></dd>
 * 
 * <dt><code>-j</code></dt>
 * <dt><code>--join</code></dt>
 * <dd>Join all truth tables into one truth table.</dd>
 * 
 * <dt><code>-v</code></dt>
 * <dt><code>--version</code></dt>
 * <dd>Display version and copyright information.</dd>
 * </dl>
 * 
 * @author Chris Bouchard
 * @version 0.1 Beta
 */
public class BooleanExpressionSolver
{
    /**
     * The main method to execute this class
     * 
     * @param args
     *            arguments from the command line
     */
    public static void main(final String[] args)
    {
        boolean takeInput = true;
        boolean breakApart = false;
        boolean joinTables = false;
        
        String filename = null;
        
        final Map<String, Boolean> userDict = new HashMap<String, Boolean>();
        final List<List<BooleanExpression>> inputsList = new ArrayList<List<BooleanExpression>>();
        
        final Comparator<BooleanExpression> comp = new Comparator<BooleanExpression>()
        {
            public int compare(final BooleanExpression o1, final BooleanExpression o2)
            {
                if (o1.toString().length() == o2.toString().length())
                {
                    return o1.toString().compareTo(o2.toString());
                }
                
                else
                {
                    return o1.toString().length() - o2.toString().length();
                }
            }
        };
        
        if (args.length > 0)
        {
            boolean parseFlags = true;
            
            for (int i = 0; i < args.length; i++)
            {
                if (parseFlags)
                {
                    try
                    {
                        if (args[i].equals("-b") || args[i].equals("--break"))
                        {
                            breakApart = true;
                        }
                        
                        else if (args[i].equals("-e") || args[i].equals("--expression"))
                        {
                            parseFlags = false;
                            takeInput = false;
                        }
                        
                        else if (args[i].equals("-f") || args[i].equals("--file"))
                        {
                            filename = args[++i];
                            
                            if (!new File(filename).isAbsolute())
                            {
                                filename = new File("").getAbsoluteFile().getPath() + File.separatorChar + filename;
                            }
                        }
                        
                        else if (args[i].equals("-h") || args[i].equals("--help"))
                        {
                            System.out.println("net.jbouchard.bool.BooleanExpressionSolver [{-b|--break}] "
                                    + "[{-d|--define} var1=value1[,var2=value2...[,varN=valueN]] [{-f|--file} filename] [{-h|--help}] [{-j|--join}] "
                                    + "[{-v|--version}] [{-e|--expression} expression1[ expression2...[expressionN]]]");
                            System.out.println();
                            
                            System.out.println("{-b|--break}");
                            System.out.println("\tBreak the expression into subexpressions.");
                            System.out.println();
                            
                            System.out.println("{-d|--define} var1=value1[,var2=value2...[,varN=valueN]");
                            System.out.println("\tDefine the constants var1 through varN with the values var1 through varN.");
                            System.out.println();
                            
                            System.out.println("{-f|--file} filename");
                            System.out.println("\tInput from filename instead of standard input.");
                            System.out.println();
                            
                            System.out.println("{-h|--help}");
                            System.out.println("\tDisplay this help message.");
                            System.out.println();
                            
                            System.out.println("{-j|--join}");
                            System.out.println("\tJoin all truth tables into one truth table.");
                            System.out.println();
                            
                            System.out.println("{-v|--version}");
                            System.out.println("\tDisplay version and copyright information.");
                            System.out.println();
                            
                            System.out.println("{-e|--expression} expression1[ expression2...[expressionN]]");
                            System.out.println("\tBuild truth tables for the given expressions. Do not take input from stdin. "
                                    + "NOTE: This argument must come last as all remaining arguments will be parsed as expressions.");
                            
                            return;
                        }
                        
                        else if (args[i].equals("-v") || args[i].equals("--version"))
                        {
                            System.out.println("BooleanExpressionSolver 1.0");
                            System.out.println("Copyright (C) 2007 Chris Bouchard");
                            System.out.println("Released under the GNU General Public License (GPL) version 3.");
                            
                            return;
                        }
                        
                        else if (args[i].equals("-j") || args[i].equals("--join"))
                        {
                            joinTables = true;
                        }
                        
                        else if (args[i].equals("-d") || args[i].equals("--define"))
                        {
                            final String dictStr = args[i + 1];
                            
                            for (final String str : dictStr.split(","))
                            {
                                final String[] setting = str.split("=", 2);
                                
                                if (setting.length == 2)
                                {
                                    if (!Variables.isVariable(setting[0], new HashSet<Collection<String>>()))
                                    {
                                        throw new ParseException("Invalid constant name: " + setting[0], i);
                                    }
                                    
                                    if (setting[1].equals("0"))
                                    {
                                        userDict.put(setting[0], false);
                                    }
                                    
                                    else if (setting[1].equals("1"))
                                    {
                                        userDict.put(setting[0], true);
                                    }
                                    
                                    else if (userDict.containsKey(setting[1]))
                                    {
                                        userDict.put(setting[0], userDict.get(setting[1]));
                                    }
                                    
                                    else
                                    {
                                        throw new ParseException("Unknown value: " + setting[1], i);
                                    }
                                }
                                
                                else
                                {
                                    throw new ParseException("Error parsing user dictionary: " + dictStr, i);
                                }
                            }
                            
                            i++;
                        }
                        
                        else
                        {
                            throw new ParseException("Unknown flag: " + args[i], i);
                        }
                    }
                    catch (final ParseException e)
                    {
                        System.out.println("At flag " + e.getErrorOffset() + ": " + e.getMessage());
                        System.exit(1);
                    }
                }
                
                else
                {
                    try
                    {
                        final BooleanExpression expr = new BooleanExpression(args[i], userDict);
                        
                        if (breakApart)
                        {
                            final List<BooleanExpression> exprList = new ArrayList<BooleanExpression>(expr.getSubExpressions());
                            final List<BooleanExpression> l;
                            
                            if (joinTables)
                            {
                                if (inputsList.size() == 0)
                                {
                                    inputsList.add(new ArrayList<BooleanExpression>());
                                }
                                
                                l = inputsList.get(0);
                            }
                            else
                            {
                                inputsList.add(new ArrayList<BooleanExpression>());
                                l = inputsList.get(inputsList.size() - 1);
                            }
                            
                            Collections.sort(exprList, comp);
                            l.addAll(exprList);
                        }
                        
                        else
                        {
                            final List<BooleanExpression> l;
                            
                            if (joinTables)
                            {
                                if (inputsList.size() == 0)
                                {
                                    inputsList.add(new ArrayList<BooleanExpression>());
                                }
                                
                                l = inputsList.get(0);
                            }
                            else
                            {
                                inputsList.add(new ArrayList<BooleanExpression>());
                                l = inputsList.get(inputsList.size() - 1);
                            }
                            
                            l.add(expr);
                        }
                    }
                    
                    catch (final ParseException e)
                    {
                        System.out.println("At character " + e.getErrorOffset() + ": " + e.getMessage());
                        System.exit(1);
                    }
                }
            }
        }
        
        try
        {
            final BufferedReader stdin = new BufferedReader(filename == null ? new InputStreamReader(System.in) : new FileReader(filename));
            String input;
            
            while (takeInput && (input = stdin.readLine()) != null)
            {
                for (final String str : Arrays.asList(input.split("\\s+")))
                {
                    final BooleanExpression expr = new BooleanExpression(str, userDict);
                    
                    if (breakApart)
                    {
                        final List<BooleanExpression> exprList = new ArrayList<BooleanExpression>(expr.getSubExpressions());
                        final List<BooleanExpression> l;
                        
                        if (joinTables)
                        {
                            if (inputsList.size() == 0)
                            {
                                inputsList.add(new ArrayList<BooleanExpression>());
                            }
                            
                            l = inputsList.get(0);
                        }
                        else
                        {
                            inputsList.add(new ArrayList<BooleanExpression>());
                            l = inputsList.get(inputsList.size() - 1);
                        }
                        
                        Collections.sort(exprList, comp);
                        l.addAll(exprList);
                    }
                    
                    else
                    {
                        final List<BooleanExpression> l;
                        
                        if (joinTables)
                        {
                            if (inputsList.size() == 0)
                            {
                                inputsList.add(new ArrayList<BooleanExpression>());
                            }
                            
                            l = inputsList.get(0);
                        }
                        else
                        {
                            inputsList.add(new ArrayList<BooleanExpression>());
                            l = inputsList.get(inputsList.size() - 1);
                        }
                        
                        l.add(expr);
                    }
                }
            }
            
            for (final Iterator<List<BooleanExpression>> iter = inputsList.iterator(); iter.hasNext();)
            {
                final List<BooleanExpression> list = iter.next();
                
                System.out.println(new TruthTable(list));
                
                if (iter.hasNext())
                {
                    System.out.println();
                }
            }
        }
        
        catch (final ParseException e)
        {
            System.out.println("At character " + e.getErrorOffset() + ": " + e.getMessage());
            System.exit(1);
        }
        
        catch (final IOException e)
        {
            e.printStackTrace();
            System.exit(2);
        }
        
    }
}
