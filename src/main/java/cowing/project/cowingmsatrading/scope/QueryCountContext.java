package cowing.project.cowingmsatrading.scope;

public final class QueryCountContext {
    private static final ThreadLocal<Integer> COUNT = ThreadLocal.withInitial(() -> 0);

    private QueryCountContext() {}

    public static void reset() { COUNT.set(0); }

    public static void increase() { COUNT.set(COUNT.get() + 1); }

    public static int get() { return COUNT.get(); }

    public static void clear() { COUNT.remove(); }
}

