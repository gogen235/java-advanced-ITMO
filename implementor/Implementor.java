package info.kgeorgiy.ja.goge.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.reflect.Modifier.*;
import static java.nio.file.Files.newBufferedWriter;

/**
 * Implementation of {@link JarImpler}, that generates implementation of class or interface.
 *
 * @author Goge Anastasiia (gogen_235@mail.ru)
 */
public class Implementor implements JarImpler {

    /**
     * Tabulation.
     */
    private static final String TAB = " ".repeat(4);

    /**
     * Creates new instance of {@link Implementor}.
     */
    public Implementor() {
    }

    /**
     * Produces code or <var>.jar</var> file implementing class or interface specified by provided name.
     * If the first passed argument is the flag <var>-jar</var>, function will produce <var>.jar</var> file. Second
     * argument must be class name, third must be target <var>.jar</var> file.
     * If there is no flag <var>-jar</var>, function will produce code. First argument must be class name,
     * second must be path, where generated source code should be placed. Function will produce code.
     * Generated class name should be the same as the class name of the name argument
     * with <var>Impl</var> suffix added.
     *
     * @param args arguments.
     * @throws IllegalArgumentException if arguments do not match conditions.
     * @throws ClassNotFoundException if class name is illegal.
     * @throws ImplerException if was thrown exception by implementing class.
     */
    public static void main(String[] args) throws IllegalArgumentException, ClassNotFoundException, ImplerException {
        if (args.length == 2) {
            Implementor implementor = new Implementor();
            implementor.implement(Class.forName(args[0]), Path.of(args[1]));
        } else if (args.length == 3 && args[0].equals("-jar")) {
            Implementor implementor = new Implementor();
            implementor.implementJar(Class.forName(args[1]), Path.of(args[2]));
        } else {
            throw new IllegalArgumentException("Illegal number of arguments"); // :NOTE: more info
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void implement(final Class<?> token, Path root) throws ImplerException {

        checkClass(token);

        String packageName = token.getPackageName();
        String name = new String((token.getSimpleName() + "Impl").getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        Path path = getPath(token, root);
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
        } catch (IOException ignore) {
        }

        StringBuilder clazz = new StringBuilder();

        String fullName = token.getCanonicalName();
        String action = isInterface(token.getModifiers()) ? "implements" : "extends";

        clazz.append(String.format("""
                package %s;
                             
                """, packageName));
        clazz.append(String.format("""
                public class %s %s %s {
                                
                """, name, action, fullName));

        constructors(clazz, token, name);
        methods(clazz, token);

        clazz.append("""
                }
                """);
        try (BufferedWriter writer = newBufferedWriter(path)) {
            String string = clazz.toString().chars()
                    // :NOTE: 127 eto who?
                    .<String>mapToObj(x -> x > 127 ? String.format("\\u%04x", x) : String.valueOf((char) x))
                    .collect(Collectors.joining());
            writer.write(string);
        } catch (IOException ignore) {
        }
    }

    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path dir;
        try {
            dir = Files.createTempDirectory(Path.of(""), "dir");
            try {
                implement(token, dir);

                final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                if (compiler == null) {
                    throw new ImplerException("Could not find java compiler");
                }
                Path path = getPath(token, dir);
                final String[] args = {"-cp", getClassPath(token),
                        "-encoding", "UTF-8",
                        "-sourcepath", dir.toString(),
                        path.toString()};
                final int exitCode = compiler.run(null, null, null, args);
                if (exitCode != 0) {
                    throw new ImplerException("Compilation error");
                }

                final Path classFilePath = path.getParent().resolve(token.getSimpleName() + "Impl.class");
                final Manifest manifest = new Manifest();
                manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
                manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VENDOR, "Goge Anastasiia");
                try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
                    out.putNextEntry(new JarEntry(
                            token.getPackageName().replace(".", "/")
                                    + "/"
                                    + token.getSimpleName()
                                    + "Impl.class"
                    ));
                    Files.copy(classFilePath, out);
                } catch (IOException e) {
                    throw new ImplerException("Can not write to jar file" + e, e);
                }
            } finally {
                // :NOTE: clean from tests
                Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (IOException e) {
            throw new ImplerException("Can not create directory" + e, e);
        }
    }

    /**
     * Returns a path consisting of {@code root}, {@code token} package and {@code token} name
     * with suffix <var>Impl.java</var>.
     *
     * @param token type token.
     * @param root  root directory.
     * @return path consisting of {@code root}, {@code token} package and {@code token} name
     * with suffix <var>Impl.java</var>.
     */
    private static Path getPath(final Class<?> token, Path root) {
        String packageName = token.getPackageName();
        String name = token.getSimpleName() + "Impl.java";

        String fileName = packageName.replace(".", File.separator) + File.separator + name;
        return new File(root.toString(), fileName).toPath();
    }

    /**
     * Returns classpath for {@code token}. If location of {@code token} code source is unavailable,
     * returns empty string.*
     *
     * @param token type token.
     * @return classpath or empty string.
     */
    private static String getClassPath(final Class<?> token) {
        try {
            return Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (URISyntaxException | IllegalArgumentException e) {
            return "";
        }
    }

    /**
     * Checks that a {@code token} class can be implemented. It seems that {@code token} is not primitive,
     * is not final, is not private, is not Enum or Record and has at least one non-private constructor.
     *
     * @param token type token.
     * @throws ImplerException if a {@code token} class can not be implemented.
     */
    private static void checkClass(final Class<?> token) throws ImplerException {
        if (token.isPrimitive()) {
            throw new ImplerException("Can not extend primitive type");
        }
        int modifiers = token.getModifiers();
        if (isFinal(modifiers)) {
            throw new ImplerException("Can not extend final classes");
        }
        if (isPrivate(modifiers)) {
            throw new ImplerException("Can not extend private classes");
        }
        if (token.equals(Enum.class)) {
            throw new ImplerException("Can not extend enum");
        }
        if (token.equals(Record.class)) {
            throw new ImplerException("Can not extend record");
        }
        if (haveOnlyPrivateConstructors(token)) {
            throw new ImplerException("Can not extend class without not private constructors");
        }
    }

    /**
     * Determines whether {@code token} has at least one non-private constructor.
     *
     * @param token type token.
     * @return true if {@code token} has at least one non-private constructor; otherwise, false.
     */
    private static boolean haveOnlyPrivateConstructors(final Class<?> token) {
        return !isInterface(token.getModifiers()) && token.getConstructors().length == 0
                && Arrays.stream(token.getDeclaredConstructors()).allMatch(x -> isPrivate(x.getModifiers()));
    }

    /**
     * Append to {@code clazz} implementation of all constructors of class named {@code name}
     * that implements {@code token}.
     *
     * @param clazz source to append.
     * @param token type token.
     * @param name  implemented class name.
     */
    private static void constructors(StringBuilder clazz, final Class<?> token, String name) {
        Arrays.stream(token.getDeclaredConstructors())
                .filter(x -> !isPrivate(x.getModifiers()))
                .forEach(x -> constructor(clazz, x, name));
    }

    /**
     * Append to {@code clazz} implementation of {@code constructor} of class named {@code name}.
     *
     * @param clazz       source to append.
     * @param constructor constructor to append.
     * @param name        implemented class name.
     */
    private static void constructor(final StringBuilder clazz, final Constructor<?> constructor, String name) {
        int parametersLength = constructor.getParameters().length;
        String body = String.format("super(%s)",
                IntStream.range(0, parametersLength)
                        .mapToObj(x -> "arg" + x)
                        .collect(Collectors.joining(", ")));
        writeMethod(clazz, constructor, name, body);
    }

    /**
     * Checks that each method in {@code methods} has not private class as arguments types or return types.
     *
     * @param methods list of methods which need to be checked.
     * @throws ImplerException if type of argument or return type is private class
     */
    private static void checkPrivateTypes(List<Method> methods) throws ImplerException {
        if (methods.stream().map(Method::getParameterTypes).anyMatch(Implementor::checkParameterTypes)) {
            throw new ImplerException("Type of argument is private class");
        }
        if (methods.stream().map(Method::getReturnType).anyMatch(x -> isPrivate(x.getModifiers()))) {
            throw new ImplerException("Return type is private class");
        }
    }

    /**
     * Determines whether each parameter in {@code parameters} has not private class.
     *
     * @param parameters list of parameters which need to be checked.
     * @return true if each parameter in {@code parameters} has not private class; otherwise, false.
     */
    private static boolean checkParameterTypes(Class<?>[] parameters) {
        return Arrays.stream(parameters).anyMatch(x -> isPrivate(x.getModifiers()));
    }

    /**
     * Stores the name and parameters of the method.
     *
     * @param name       of method which signature is stored.
     * @param parameters of method which signature is stored.
     */
    private record MethodSignature(String name, String parameters) {
        /**
         * Creates a new signature of {@code method}.
         *
         * @param method which signature will be stored.
         */
        public MethodSignature(Method method) {
            this(method.getName(), Arrays.toString(method.getParameterTypes()));
        }

    }

    /**
     * Returns a {@link Stream<Method>} that contains methods from abstract classes that class
     * implementing {@code token} need to define. Recursively traverses all parent classes of the token
     * and collects all abstract methods. If methods have the same signature, then the one found in the youngest
     * parent is selected. If there are several of them, both return.
     *
     * @param token type token.
     * @return methods from abstract classes, that class need to define.
     */
    private static Stream<Method> getMethods(final Class<?> token) {
        Stream<Method> stream = Stream.of();
        if (token.getSuperclass() != null) {
            stream = Stream.concat(stream, getMethods(token.getSuperclass()));
        }
        Set<MethodSignature> methods = Arrays.stream(token.getDeclaredMethods())
                .map(MethodSignature::new)
                .collect(Collectors.toSet());
        stream = stream.filter(x -> !methods.contains(new MethodSignature(x)));
        stream = Stream.concat(stream, Arrays.stream(token.getDeclaredMethods()));
        return stream.filter(x -> isAbstract(x.getModifiers()));
    }

    /**
     * Append to {@code clazz} implementation of all methods {@code token} that need implementation.
     *
     * @param clazz source to append.
     * @param token type token.
     * @throws ImplerException if type of argument or return type is private class
     */
    private static void methods(StringBuilder clazz, final Class<?> token) throws ImplerException {
        List<Method> methodList = getMethods(token).toList();
        methodList = Stream.concat(methodList.stream(),
                Arrays.stream(token.getMethods())
                        .filter(x -> isAbstract(x.getModifiers()))).toList();

        checkPrivateTypes(methodList);
        Map<MethodSignature, List<Method>> methodsMap = methodList.stream().collect(Collectors.groupingBy(MethodSignature::new));
        methodsMap.forEach((key, value) -> method(clazz, key, value));
    }

    /**
     * Append to {@code clazz} implementation of method with signature {@code signature}.
     * The return type is chosen as the narrowest among all return types of methods from {@code methods}.
     *
     * @param clazz     source to append.
     * @param signature signature of method
     * @param methods   methods with signature {@code signature}.
     */
    private static void method(StringBuilder clazz, MethodSignature signature, List<Method> methods) {
        Class<?> returnType = methods.stream().map(Method::getReturnType).min(Implementor::leastClass).orElseThrow();

        String body = "return" + defaultValue(returnType);
        writeMethod(clazz, methods.getFirst(), returnType.getCanonicalName() + " " + signature.name(), body);
    }

    /**
     * Append to {@code clazz} implementation of method with return type and name written in {@code name}
     * and body {@code body}. Parameters and exceptions are taken from {@code method}.
     *
     * @param clazz  source to append.
     * @param method method to implement.
     * @param name   method return type and name.
     * @param body   method body.
     */
    private static void writeMethod(StringBuilder clazz, Executable method, String name, String body) {
        clazz.append(String.format("%spublic %s", TAB, name));
        parameters(clazz, method);
        clazz.append(String.format("""
                {%s%s;
                %s}

                """, TAB, body, TAB));
    }

    /**
     * Append to {@code clazz} parameters and exceptions taken from {@code method}.
     *
     * @param clazz  source to append.
     * @param method method to get parameters.
     */
    private static void parameters(StringBuilder clazz, Executable method) {
        Parameter[] parameters = method.getParameters();
        clazz.append(String.format("(%s) ", join(parameters, x -> x.getType().getCanonicalName() + " " + x.getName())));

        Class<?>[] exceptions = method.getExceptionTypes();
        if (exceptions.length > 0) {
            clazz.append("throws ");
        }
        clazz.append(String.format("%s ", join(exceptions, Class::getCanonicalName)));
    }

    /**
     * Returns a default value of token as a String. If type is {@link Class<Void>} empty string returns, else
     * default value with white space prefix.
     *
     * @param token type token.
     * @return a default value of token as a String.
     */
    private static String defaultValue(Class<?> token) {
        if (token.equals(void.class)) {
            return "";
        }
        if (token.equals(char.class) || token.equals(float.class)) {
            return " 0";
        }
        return " " + Array.get(Array.newInstance(token, 1), 0);
    }

    /**
     * Compares two classes based on which one extend by other class.
     *
     * @param fst first class.
     * @param snd second class.
     * @return a value greater than 0 if {@code fst} is assignable from {@code snd}; otherwise a value less than 0.
     */
    private static int leastClass(Class<?> fst, Class<?> snd) {
        if (fst.isAssignableFrom(snd)) {
            return 1;
        }
        return -1;
    }

    /**
     * Apply to elements of {@code array} {@code function} and join them in {@link String} via comma.
     *
     * @param array    array of elements.
     * @param function function that will be applied to elements.
     * @param <T>      {@code array} elements type.
     * @return result of applying {@code function} to elements of {@code array} and joining them via comma.
     */
    private static <T> String join(T[] array, Function<T, String> function) {
        return Arrays.stream(array).map(function).collect(Collectors.joining(", "));
    }

}
