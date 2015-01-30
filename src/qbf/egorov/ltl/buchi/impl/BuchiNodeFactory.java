/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.ltl.buchi.impl;

import qbf.egorov.ltl.buchi.IBuchiNodeFactory;

/**
 * @author kegorov
 *         Date: Apr 6, 2009
 */
public class BuchiNodeFactory implements IBuchiNodeFactory<BuchiNode> {
    int id;

    public BuchiNode createBuchiNode() {
        return new BuchiNode(id++);
    }
}
