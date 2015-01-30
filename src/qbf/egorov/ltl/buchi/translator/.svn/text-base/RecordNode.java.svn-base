/**
 * RecordNode.java, 19.03.2008
 */
package ru.ifmo.ltl.buchi.translator;

import ru.ifmo.ltl.grammar.LtlNode;

import java.util.*;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class RecordNode {
    Set<RecordNode> incoming;
    Set<LtlNode> oldForm;
    Queue<LtlNode> newForm;
    Set<LtlNode> nextForm;

    public RecordNode() {
        incoming = new HashSet<RecordNode>();
        oldForm = new HashSet<LtlNode>();
        newForm = new LinkedList<LtlNode>();
        nextForm = new HashSet<LtlNode>();
    }
}
