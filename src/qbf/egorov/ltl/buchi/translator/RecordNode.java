/**
 * RecordNode.java, 19.03.2008
 */
package qbf.egorov.ltl.buchi.translator;

import qbf.egorov.ltl.grammar.LtlNode;

import java.util.*;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class RecordNode {
    Set<RecordNode> incoming = new HashSet<>();
    Set<LtlNode> oldForm = new HashSet<>();
    Queue<LtlNode> newForm = new LinkedList<>();
    Set<LtlNode> nextForm = new HashSet<>();
}
