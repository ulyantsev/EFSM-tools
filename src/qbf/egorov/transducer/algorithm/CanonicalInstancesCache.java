package qbf.egorov.transducer.algorithm;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class CanonicalInstancesCache<T> {
	  private int maxCacheSize;
      private Map<Long, T> canonicalInstances;
      private Deque<Long> instanceHashDeque;

      public CanonicalInstancesCache(int maxCacheSize) {
              this.maxCacheSize = maxCacheSize;
              canonicalInstances = new HashMap<Long, T>(2 * maxCacheSize);
              instanceHashDeque = new ArrayDeque<Long>(2 * maxCacheSize);
      }

      public void add(T nonCanonicalInstance, FST canonicalInstance) {
              long hash = canonicalInstance.computeStringHash();
              canonicalInstances.put(hash, nonCanonicalInstance);
              instanceHashDeque.addLast(hash);
              if (instanceHashDeque.size() > maxCacheSize) {
                      canonicalInstances.remove(instanceHashDeque.getFirst());
                      instanceHashDeque.pollFirst();
              }
      }

      public boolean contains(FST canonicalInstance) {
              return canonicalInstances.containsKey(canonicalInstance.computeStringHash());
      }

      public T getFirstNonCanonicalInstance(FST canonicalInstance) {
              return canonicalInstances.get(canonicalInstance.computeStringHash());
      }

      public void clear() {
              canonicalInstances.clear();
              instanceHashDeque.clear();
      }

}
