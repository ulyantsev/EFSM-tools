/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.ltl.buchi;

/**
 * @author kegorov
 *         Date: Apr 6, 2009
 */
public interface IBuchiNodeFactory<N extends IBuchiNode> {

    N createBuchiNode();
}
