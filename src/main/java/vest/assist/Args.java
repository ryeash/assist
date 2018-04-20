package vest.assist;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An immutable wrapper around the arguments passed into the main method.
 */
public class Args {

    public static final String FLAG = "-";
    public static final String VERBOSE_FLAG = "--";

    private final List<String> args;

    public Args(String[] args) {
        this.args = Stream.of(args).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    /**
     * Get the value of the i'th argument.
     *
     * @param i The index of the argument to get
     * @return The value of i'th argument
     * @throws ArrayIndexOutOfBoundsException if the given i is greater than the number of arguments
     */
    public String get(int i) {
        if (i >= args.size()) {
            throw new ArrayIndexOutOfBoundsException(args.size() + " argument(s) passed in, can not get position " + i + " for: " + args);
        }
        return args.get(i);
    }

    /**
     * Get the value of the first arg (index 0).
     */
    public String first() {
        return get(0);
    }

    /**
     * Get the value of the second arg (index 1).
     */
    public String second() {
        return get(1);
    }

    /**
     * Get the value of the third arg (index 2).
     */
    public String third() {
        return get(2);
    }

    /**
     * Get the value of the fourth arg (index 3).
     */
    public String fourth() {
        return get(3);
    }

    /**
     * Get the value of the fifth arg (index 4).
     */
    public String fifth() {
        return get(4);
    }

    /**
     * Get the total number of arguments passed in.
     */
    public int length() {
        return args.size();
    }

    /**
     * Look for the flag, for example: args like "-e", flag("e") => true.
     *
     * @param flag The flag to search for
     * @return true if the flag is present in the arguments, else false
     */
    public boolean flag(String flag) {
        for (String arg : args) {
            if (arg.startsWith(FLAG) && !arg.startsWith(VERBOSE_FLAG) && arg.contains(flag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Looks for the value of a flag, for example: args like "-e dev", flagValue("e") => "dev"
     *
     * @param flag     The flag to get the value for
     * @param fallback The fallback value to use
     * @return The value of the flag, or the fallback if it's not present
     */
    public String flagValue(String flag, String fallback) {
        String value = flagValue(flag);
        return value != null ? value : fallback;
    }

    /**
     * Looks for the value of a flag, for example: args like "-e dev", flagValue("e") => "dev"
     *
     * @param flag The flag to get the value for
     * @return The value of the flag, or null if it's not present
     */
    public String flagValue(String flag) {
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.startsWith(FLAG) && !arg.startsWith(VERBOSE_FLAG) && arg.contains(flag)) {
                if ((i + 1) >= args.size()) {
                    throw new ArrayIndexOutOfBoundsException("the flag '" + flag + "' did not have a value after it");
                }
                return get(i + 1);
            }
        }
        return null;
    }

    /**
     * Look for a verbose flag, for example: args like "--debug", verboseFlag("debug") => true.
     *
     * @param flag The verbose flag name to look for
     * @return true if the flag is present in the arguments, else false
     */
    public boolean verboseFlag(String flag) {
        for (String arg : args) {
            if (arg.startsWith(VERBOSE_FLAG) && Objects.equals(arg.substring(2), flag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Look for the value of a verbose flag,
     * for example: args like "--properties=myprops.props", verboseFlagValue("properties") => "myprops.props".
     *
     * @param flag     The flag to get the value for
     * @param fallback The fallback value to use
     * @return The value of the flag, or the fallback if it's not present
     */
    public String verboseFlagValue(String flag, String fallback) {
        String value = verboseFlagValue(flag);
        return value != null ? value : fallback;
    }

    /**
     * Look for the value of a verbose flag,
     * for example: args like "--properties=myprops.props", verboseFlagValue("properties") => "myprops.props".
     *
     * @param flag The flag to get the value for
     * @return The value of the flag, or null if it's not present
     */
    public String verboseFlagValue(String flag) {
        for (String arg : args) {
            if (arg.startsWith(VERBOSE_FLAG)) {
                String sub = arg.substring(2);
                int eq = sub.indexOf('=');
                if (eq < 0) {
                    continue;
                }
                String name = sub.substring(0, eq).trim();
                if (Objects.equals(name, flag)) {
                    return sub.substring(eq + 1).trim();
                }
            }
        }
        return null;
    }

    /**
     * Returns true if the arguments contain the given value.
     */
    public boolean contains(String arg) {
        return args.contains(arg);
    }

    @Override
    public String toString() {
        return args.toString();
    }
}
