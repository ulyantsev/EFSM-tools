/**
 * RecordNode.java, 19.03.2008
 */
package verification.ltl.buchi.translator;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import verification.ltl.grammar.LtlNode;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class RecordNode {
    final Set<RecordNode> incoming = new LinkedHashSet<>();
    final Set<LtlNode> oldForm = new LinkedHashSet<>();
    final Queue<LtlNode> newForm = new LinkedList<>();
    final Set<LtlNode> nextForm = new LinkedHashSet<>();
}
