package vest.assist.conf;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The default implementation of {@link ConfigurationFacade}. Will be used by {@link Builder} when creating
 * facades.
 */
public class DefaultConfigurationFacade implements ConfigurationFacade {

    private static final List<String> CONVERSION_METHOD_NAMES = Arrays.asList("valueOf", "forString", "forName");

    private final List<ConfigurationSource> sources;
    private final Map<Class, Function<String, Object>> converterCache;
    private char listDelimiter = ',';

    /**
     * Create a new instance using the given source list.
     *
     * @param sources The list of sources to get properties from
     */
    public DefaultConfigurationFacade(List<ConfigurationSource> sources) {
        this.sources = Objects.requireNonNull(sources);
        this.converterCache = new HashMap<>();
        // default converters
        this.converterCache.put(String.class, str -> str);
        this.converterCache.put(CharSequence.class, str -> str);
        this.converterCache.put(StringBuilder.class, StringBuilder::new);
        this.converterCache.put(Byte.class, Byte::valueOf);
        this.converterCache.put(Byte.TYPE, Byte::valueOf);
        this.converterCache.put(Short.class, Short::valueOf);
        this.converterCache.put(Short.TYPE, Short::valueOf);
        this.converterCache.put(Integer.class, Integer::valueOf);
        this.converterCache.put(Integer.TYPE, Integer::valueOf);
        this.converterCache.put(Long.class, Long::valueOf);
        this.converterCache.put(Long.TYPE, Long::valueOf);
        this.converterCache.put(Float.class, Float::valueOf);
        this.converterCache.put(Float.TYPE, Float::valueOf);
        this.converterCache.put(Double.class, Double::valueOf);
        this.converterCache.put(Double.TYPE, Double::valueOf);
        this.converterCache.put(Boolean.class, Boolean::valueOf);
        this.converterCache.put(Boolean.TYPE, Boolean::valueOf);
        this.converterCache.put(BigInteger.class, BigInteger::new);
        this.converterCache.put(BigDecimal.class, BigDecimal::new);
    }

    /**
     * Set the delimiter character to use for collections values.
     *
     * @param delimiter the delimiter, e.g. ','
     * @default ','
     */
    public void setListDelimiter(char delimiter) {
        this.listDelimiter = delimiter;
    }

    @Override
    public List<ConfigurationSource> sources() {
        return sources;
    }

    @Override
    public <T> T get(String propertyName, T fallback, Class<T> type) {
        return Optional.ofNullable(get(propertyName))
                .map(getConverter(type))
                .orElse(fallback);
    }

    @Override
    public <T> Stream<T> getStream(String propertyName, Class<T> genericType, Stream<T> fallback) {
        String value = get(propertyName);
        if (value == null) {
            return fallback;
        } else {
            return split(get(propertyName), listDelimiter)
                    .stream()
                    .map(getConverter(genericType));
        }
    }

    @Override
    public String toString() {
        return sources.stream()
                .map(ConfigurationSource::toString)
                .collect(Collectors.joining(", ", getClass().getSimpleName() + "[", "]"));
    }

    @SuppressWarnings("unchecked")
    private <T> Function<String, T> getConverter(Class<T> type) {
        if (type == null) {
            return str -> (T) str;
        }
        return (Function<String, T>) converterCache.computeIfAbsent(type, DefaultConfigurationFacade::buildMapper);
    }

    @SuppressWarnings("unchecked")
    private static Function<String, Object> buildMapper(Class<?> type) {
        for (String methodName : CONVERSION_METHOD_NAMES) {
            try {
                Method method = type.getMethod(methodName, String.class);
                if (Modifier.isStatic(method.getModifiers())) {
                    return new ExecutableConverter(method, type);
                }
            } catch (NoSuchMethodException e) {
                // ignore
            }
        }

        try {
            Constructor constructor = type.getConstructor(String.class);
            return new ExecutableConverter(constructor, type);
        } catch (NoSuchMethodException e) {
            // ignore
        }

        throw new IllegalArgumentException("no string conversion method found for type: " + type);
    }

    private static final class ExecutableConverter implements Function<String, Object> {
        private final Executable executable;
        private final Class<?> targetType;

        private ExecutableConverter(Executable executable, Class<?> targetType) {
            this.executable = executable;
            this.targetType = targetType;
        }

        @Override
        public Object apply(String s) {
            try {
                if (executable instanceof Method) {
                    return ((Method) executable).invoke(null, s);
                } else if (executable instanceof Constructor) {
                    return ((Constructor) executable).newInstance(s);
                } else {
                    throw new IllegalArgumentException("unknown executable type: " + executable);
                }
            } catch (Exception e) {
                throw new RuntimeException("error converting string value to " + targetType, e);
            }
        }
    }

    private static List<String> split(String str, char delimiter) {
        if (str == null || str.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>(3);
        int prev = 0;
        int i = 0;
        while ((i = str.indexOf(delimiter, i)) >= 0) {
            list.add(str.substring(prev, i).trim());
            i++;
            prev = i;
        }
        list.add(str.substring(prev).trim());
        list.removeIf(String::isEmpty);
        return list;
    }
}
