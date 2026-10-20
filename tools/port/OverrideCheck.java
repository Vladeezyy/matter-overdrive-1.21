import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Lost overrides after a Minecraft update: methods of the mod that have the name of a method of a non-mod supertype but
 * none of its signatures, so they no longer override it and nothing calls them (javac only notices with @Override).
 * java -cp <compile classpath>:tools/port OverrideCheck <classes dir> [package prefix of the mod, default com.github.alexthe666.rats]
 * Run it on the classes a loader module compiled (they have the loader's bridge methods): common classes compiled
 * without the loader report its generic extension methods (NeoForge getRenderBoundingBox) although they override them.
 */
public class OverrideCheck {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        String mod = args.length > 1 ? args[1] : "com.github.alexthe666.rats";
        List<String> found = new ArrayList<>();
        int checked = 0;
        try (Stream<Path> files = Files.walk(root)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".class")).toList()) {
                String rel = root.relativize(p).toString();
                String name = rel.substring(0, rel.length() - 6).replace('/', '.');
                if (name.contains(".mixin.")) continue;
                Class<?> type;
                try {
                    type = Class.forName(name, false, OverrideCheck.class.getClassLoader());
                } catch (Throwable t) {
                    continue;
                }
                checked++;
                Method[] methods;
                try {
                    methods = type.getDeclaredMethods();
                } catch (Throwable t) {
                    continue;
                }
                Set<String> bridged = Arrays.stream(methods).filter(Method::isBridge).map(Method::getName).collect(Collectors.toSet());
                for (Method m : methods) {
                    int mods = m.getModifiers();
                    if (Modifier.isStatic(mods) || Modifier.isPrivate(mods) || m.isSynthetic() || m.isBridge()) continue;
                    // a generic override has a bridge with the erased signature
                    if (bridged.contains(m.getName())) continue;
                    List<Method> sameName = new ArrayList<>();
                    boolean overrides = false;
                    for (Class<?> s : supertypes(type)) {
                        boolean modType = s.getName().startsWith(mod) || s.getName().startsWith("dev.vladeezyy");
                        Method[] superMethods;
                        try {
                            superMethods = s.getDeclaredMethods();
                        } catch (Throwable t) {
                            continue;
                        }
                        for (Method sm : superMethods) {
                            if (!sm.getName().equals(m.getName()) || Modifier.isPrivate(sm.getModifiers()) || Modifier.isStatic(sm.getModifiers())) continue;
                            if (Arrays.equals(sm.getParameterTypes(), m.getParameterTypes())) overrides = true;
                            else if (!modType) sameName.add(sm);
                        }
                    }
                    if (!overrides && !sameName.isEmpty()) {
                        found.add(type.getName() + "." + m.getName() + Arrays.toString(m.getParameterTypes()) + "\n      super: "
                                + sameName.stream().map(sm -> sm.getDeclaringClass().getSimpleName() + Arrays.toString(sm.getParameterTypes())).distinct().collect(Collectors.joining("; ")));
                    }
                }
            }
        }
        System.out.println("checked " + checked + " classes; possible lost overrides: " + found.size());
        found.forEach(f -> System.out.println("  " + f));
    }

    private static List<Class<?>> supertypes(Class<?> type) {
        List<Class<?>> out = new ArrayList<>();
        Deque<Class<?>> todo = new ArrayDeque<>();
        if (type.getSuperclass() != null) todo.add(type.getSuperclass());
        todo.addAll(Arrays.asList(type.getInterfaces()));
        Set<Class<?>> seen = new HashSet<>();
        while (!todo.isEmpty()) {
            Class<?> c = todo.poll();
            if (c == Object.class || !seen.add(c)) continue;
            out.add(c);
            if (c.getSuperclass() != null) todo.add(c.getSuperclass());
            todo.addAll(Arrays.asList(c.getInterfaces()));
        }
        return out;
    }
}
