package vest.assist;


public class DefaultPlaceholderLookup implements PlaceholderLookup {

    private static final String MACRO_OPEN = "${";
    private static final String MACRO_CLOSE = "}";


    @Override
    public String lookup(String expression) {
        // TODO circular reference detection
        if (expression == null || expression.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();

        int prev = 0;
        int i;
        while ((i = expression.indexOf(MACRO_OPEN, prev)) >= 0) {
            sb.append(expression, prev, i);
            i += MACRO_OPEN.length();
            prev = i;
            i = expression.indexOf(MACRO_CLOSE, i);
            String subName = expression.substring(prev, i);
            String subValue = valueFor(subName);
            sb.append(subValue);
            i += MACRO_CLOSE.length();
            prev = i;
        }
        sb.append(expression, prev, expression.length());
        return sb.toString();
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE;
    }

    protected String valueFor(String name) {
        String envValue = System.getenv(name);
        if (envValue != null) {
            return envValue.trim();
        }
        String propertyValue = System.getProperty(name);
        if (propertyValue != null) {
            return propertyValue.trim();
        }
        throw new IllegalArgumentException("failed to look up placeholder value: " + name);
    }
}
