package vest.assist;

public interface PlaceholderLookup extends Prioritized {

    String lookup(String expression);
}
