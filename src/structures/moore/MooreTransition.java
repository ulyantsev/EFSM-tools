package structures.moore;

public class MooreTransition {
    private final String event;
    private final MooreNode src;
    private final MooreNode dst;

    public MooreTransition(MooreNode src, MooreNode dst, String event) {
        this.src = src;
        this.dst = dst;
        this.event = event;
    }

    public String event() {
        return event;
    }

    public MooreNode src() {
        return src;
    }

    public MooreNode dst() {
        return dst;
    }

    @Override
    public String toString() {
        return src + " >" + event + "> " + dst + "  " + event;
    }
}
