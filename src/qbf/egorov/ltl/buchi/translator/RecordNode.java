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
    final Set<RecordNode> incoming = new HashSet<>();
    final Set<LtlNode> oldForm = new HashSet<>();
    final Queue<LtlNode> newForm = new LinkedList<>();
    final Set<LtlNode> nextForm = new HashSet<>();
}
