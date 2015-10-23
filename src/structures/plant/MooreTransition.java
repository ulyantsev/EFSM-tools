package structures.plant;

public class MooreTransition {
	private final String event;
	private final MooreNode src;
	private final MooreNode dst;

	public MooreTransition(MooreNode src, MooreNode dst, String event) {
		this.src = src;
		this.dst = dst;
		this.event = event;
	}

	public String getEvent() {
		return event;
	}

	public MooreNode getSrc() {
		return src;
	}

	public MooreNode getDst() {
		return dst;
	}

	@Override
	public String toString() {
		return src + " >" + event + "> " + dst + "  " + event;
	}
}
