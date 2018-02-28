package vest.assist.aop;

public final class AspectList extends Aspect {

    private final Aspect[] aspects;
    private final Aspect last;

    public AspectList(Aspect[] aspects) {
        if (aspects.length == 0) {
            throw new IllegalArgumentException("empty aspect list");
        }
        this.aspects = aspects;
        this.last = aspects[aspects.length - 1];
    }

    @Override
    public void init(Object instance) {
        super.init(instance);
        for (Aspect aspect : aspects) {
            aspect.init(instance);
        }
    }

    @Override
    public void pre(Invocation invocation) throws Throwable {
        // pre happens first to last
        for (Aspect aspect : aspects) {
            aspect.pre(invocation);
        }
    }

    @Override
    public void exec(Invocation invocation) throws Throwable {
        // only the last aspect gets the exec call
        last.exec(invocation);
    }

    @Override
    public void post(Invocation invocation) throws Throwable {
        // post happens last to first
        for (int i = aspects.length - 1; i >= 0; i--) {
            aspects[i].post(invocation);
        }
    }
}
