package ru.ifmo.ctddev.genetic.transducer.algorithm.util;

public class Sequence {
	private final String[] data;
	
	public Sequence(String[] data) {
		this.data = data.clone();
	}
	
	public String[] toArray() {
		return data.clone();
	}
	
	@Override
	public int hashCode() {
		int res = 0;
		int x = 1;
		for (String s : data) {
			res += s.hashCode() * x;
			x *= 17;
		}
		return res;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Sequence)) {
			return false;
		}
		Sequence that = (Sequence) o;
		if (this.data.length != that.data.length) {
			return false;
		}
		for (int i = 0; i < this.data.length; i++) {
			if (!this.data[i].equals(that.data[i])) {
				return false;
			}
		}
		return true;
	}
	
}
