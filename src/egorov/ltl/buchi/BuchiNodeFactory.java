/* 
 * Developed by eVelopers Corporation, 2009
 */
package egorov.ltl.buchi;

/**
 * @author kegorov
 *         Date: Apr 6, 2009
 */
public class BuchiNodeFactory {
    private int id;

    public BuchiNode createBuchiNode() {
        return new BuchiNode(id++);
    }
}
