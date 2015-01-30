package qbf.egorov.transducer.algorithm;

public class CanonicalCachedData {
	private FST fst;
	private int[] newId;

	public CanonicalCachedData(FST fst, int[] newId) {
		this.fst = fst;
		this.newId = newId;
	}
	
	public FST getFST() {
		return fst;
	}
	
	public int[] getNewId() {
		return newId;
	}
}
