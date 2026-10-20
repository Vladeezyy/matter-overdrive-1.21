import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/** Links every class of the mod that the server can load, on a classpath without Minecraft's client classes: lists the classes whose bytecode needs a client class to verify */
public class ServerLinkCheck {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        List<String> skip = List.of("com/github/alexthe666/rats/client/", "com/github/alexthe666/rats/compat/jei/");
        Map<String, Set<String>> problems = new TreeMap<>();
        Map<String, Integer> other = new TreeMap<>();
        int checked = 0;
        try (Stream<Path> files = Files.walk(root)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".class")).toList()) {
                String rel = root.relativize(p).toString();
                if (skip.stream().anyMatch(rel::startsWith)) continue;
                String name = rel.substring(0, rel.length() - 6).replace('/', '.');
                checked++;
                try {
                    Class.forName(name, true, ServerLinkCheck.class.getClassLoader());
                } catch (Throwable t) {
                    String msg = String.valueOf(t);
                    Throwable cause = t;
                    while (cause.getCause() != null) cause = cause.getCause();
                    msg = msg + " / " + cause;
                    if (msg.contains("net/minecraft/client") || msg.contains("net.minecraft.client") || msg.contains("blaze3d") || msg.contains("rats/client") || msg.contains("rats.client")) {
                        problems.computeIfAbsent(name, k -> new TreeSet<>()).add(msg.replaceAll(".*?(NoClassDefFoundError|ClassNotFoundException): ", "").replaceAll(" / .*", ""));
                    } else {
                        other.merge(t.getClass().getSimpleName() + ": " + msg.replaceAll(" / .*", ""), 1, Integer::sum);
                    }
                }
            }
        }
        System.out.println("checked " + checked + " classes; client-dependent: " + problems.size());
        problems.forEach((k, v) -> System.out.println("CLIENT " + k + " -> " + v));
        other.forEach((k, v) -> System.out.println("OTHER x" + v + " " + k));
    }
}
