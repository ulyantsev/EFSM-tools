package qbf.reduction;

public enum BinaryOperations {
	EQ, IMPLIES, AND, OR;
	
	@Override
	public String toString() {
		switch (this) {
		case EQ: return "<->";
		case IMPLIES: return "->";
		case AND: return "&";
		case OR: return "|";
		}
		assert false;
		return null;
	}
}