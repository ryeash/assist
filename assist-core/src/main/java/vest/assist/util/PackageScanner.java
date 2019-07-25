package vest.assist.util;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * Used to 'discover' classes on the classpath.
 */
public final class PackageScanner {

    public static final String CLASS_EXT = ".class";
    public static final char PACKAGE_SEPARATOR = '.';
    public static final char PATH_SEPARATOR = '/';

    private PackageScanner() {
    }

    /**
     * Using the current thread's ClassLoader, scan the classpath searching for all classes (recursively) under the
     * given package name.
     *
     * @param packageName The base package name to start the classpath scan in
     * @return A Stream of all Classes found.
     * @throws RuntimeException for any errors found while performing the classpath scan
     */
    public static Stream<Class<?>> scan(String packageName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        return scan(packageName, classLoader);
    }

    /**
     * Using the provided ClassLoader, scan the classpath (recursively) under the given package name.
     *
     * @param packageName The base package name to start the classpath scan in
     * @param classLoader The ClassLoader to use to enumerate the classes
     * @return A Stream of all Classes found.
     * @throws RuntimeException for any errors found while performing the classpath scan
     */
    public static Stream<Class<?>> scan(String packageName, ClassLoader classLoader) {
        return scanClassNames(packageName, classLoader)
                .map(className -> {
                    try {
                        return classLoader.loadClass(className);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("unscannable class path", e);
                    }
                });
    }

    /**
     * Using the provided ClassLoader, scan the classpath (recursively) under the given package name.
     *
     * @param packageName The base package name to start the classpath scan in
     * @param classLoader The ClassLoader to use to enumerate the class names
     * @return A Stream of all the names of the classes found. The class names will be in their canonical form,
     * i.e. The names returned can be used in {@link Class#forName(String)} or {@link ClassLoader#loadClass(String)}.
     */
    public static Stream<String> scanClassNames(String packageName, ClassLoader classLoader) {
        if (packageName == null || packageName.isEmpty()) {
            throw new IllegalArgumentException("invalid base package, must be non-null and non-empty");
        }
        String path = toPathNotation(packageName);
        Enumeration<URL> resources;
        try {
            resources = classLoader.getResources(path);
        } catch (IOException e) {
            throw new UncheckedIOException("error getting resources for path: " + path, e);
        }
        return Collections.list(resources)
                .stream()
                .flatMap(url -> {
                    try {
                        if (url.getProtocol().equals("jar")) {
                            return findClassesJarFile(url, path);
                        } else {
                            return findClassesClassPath(new File(url.toURI()), packageName);
                        }
                    } catch (URISyntaxException | IOException e) {
                        throw new RuntimeException("un-scannable class path", e);
                    }
                });
    }

    private static Stream<String> findClassesClassPath(File directory, String packageName) {
        if (!directory.exists()) {
            return Stream.empty();
        }
        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            return Stream.empty();
        }
        return Stream.of(files)
                .flatMap(file -> {
                    if (file.isDirectory()) {
                        if (file.getName().contains(".")) {
                            throw new RuntimeException("un-scannable class path, found a dot in a directory name?");
                        }
                        return findClassesClassPath(file, packageName + PACKAGE_SEPARATOR + file.getName());
                    } else if (file.getName().endsWith(CLASS_EXT)) {
                        String className = packageName + PACKAGE_SEPARATOR + trimClassFile(file.getName());
                        return Stream.of(className);
                    } else {
                        return Stream.empty();
                    }
                });
    }

    private static Stream<String> findClassesJarFile(URL jarUrl, String packageName) throws IOException {
        String path = toPathNotation(packageName);
        String jarFileName = URLDecoder.decode(jarUrl.getFile(), "UTF-8");
        jarFileName = jarFileName.substring(5, jarFileName.indexOf("!"));
        JarFile jf = new JarFile(jarFileName);
        return Collections.list(jf.entries())
                .stream()
                .map(ZipEntry::getName)
                .filter(name -> name.startsWith(path) && name.endsWith(CLASS_EXT) && !name.contains("$"))
                .map(PackageScanner::trimClassFile);
    }

    private static String trimClassFile(String name) {
        String temp = toPackageNotation(name.trim());
        if (temp.endsWith(CLASS_EXT)) {
            temp = temp.substring(0, temp.length() - CLASS_EXT.length());
        }
        return temp;
    }

    private static String toPackageNotation(String str) {
        return str.replace(PATH_SEPARATOR, PACKAGE_SEPARATOR);
    }

    private static String toPathNotation(String str) {
        return str.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
    }

}
