package net.byteflux.libby.relocation;

import net.byteflux.libby.Library;
import net.byteflux.libby.LibraryManager;
import net.byteflux.libby.Repositories;
import net.byteflux.libby.classloader.IsolatedClassLoader;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A reflection-based helper for relocating library jars. It automatically
 * downloads and invokes Luck's Jar Relocator to perform jar relocations.
 *
 * @see <a href="https://github.com/lucko/jar-relocator">Luck's Jar Relocator</a>
 */
public class RelocationHelper {
    /**
     * Reflected constructor for creating new jar relocator instances
     */
    private final Constructor<?> jarRelocatorConstructor;

    /**
     * Reflected method for running a jar relocator
     */
    private final Method jarRelocatorRunMethod;

    /**
     * Reflected constructor for creating relocation instances
     */
    private final Constructor<?> relocationConstructor;

    /**
     * Creates a new relocation helper using the provided library manager to
     * download the dependencies required for runtime relocation.
     *
     * @param libraryManager the library manager used to download dependencies
     */
    public RelocationHelper(LibraryManager libraryManager) {
        requireNonNull(libraryManager, "libraryManager");

        try (IsolatedClassLoader classLoader = new IsolatedClassLoader()) {
            // ObjectWeb ASM Commons
            classLoader.addPath(libraryManager.downloadLibrary(
                Library.builder()
                    .groupId("org.ow2.asm")
                    .artifactId("asm-commons")
                    .version("9.9")
                    .hexChecksum("db2f6f26150bbe7c126606b4a1151836bcc22a1e05a423b3585698bece995ff8")
                    .repository(Repositories.MAVEN_CENTRAL)
                    .build()
            ));

            // ObjectWeb ASM
            classLoader.addPath(libraryManager.downloadLibrary(
                Library.builder()
                    .groupId("org.ow2.asm")
                    .artifactId("asm")
                    .version("9.9")
                    .hexChecksum("03d99a74ad1ee5c71334ef67437f4ef4fe3488caa7c96d8645abc73c8e2017d4")
                    .repository(Repositories.MAVEN_CENTRAL)
                    .build()
            ));

            // Luck's Jar Relocator
            classLoader.addPath(libraryManager.downloadLibrary(
                Library.builder()
                    .groupId("me.lucko")
                    .artifactId("jar-relocator")
                    .version("1.7")
                    .checksum("b30RhOF6kHiHl+O5suNLh/+eAr1iOFEFLXhwkHHDu4I=")
                    .repository(Repositories.MAVEN_CENTRAL)
                    .build()
            ));

            Class<?> jarRelocatorClass = classLoader.loadClass("me.lucko.jarrelocator.JarRelocator");
            Class<?> relocationClass = classLoader.loadClass("me.lucko.jarrelocator.Relocation");

            // me.lucko.jarrelocator.JarRelocator(File, File, Collection)
            jarRelocatorConstructor = jarRelocatorClass.getConstructor(File.class, File.class, Collection.class);

            // me.lucko.jarrelocator.JarRelocator#run()
            jarRelocatorRunMethod = jarRelocatorClass.getMethod("run");

            // me.lucko.jarrelocator.Relocation(String, String, Collection, Collection)
            relocationConstructor = relocationClass.getConstructor(String.class, String.class, Collection.class, Collection.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Invokes the jar relocator to process the input jar and generate an
     * output jar with the provided relocation rules applied.
     *
     * @param in          input jar
     * @param out         output jar
     * @param relocations relocations to apply
     */
    public void relocate(Path in, Path out, Collection<Relocation> relocations) {
        requireNonNull(in, "in");
        requireNonNull(out, "out");
        requireNonNull(relocations, "relocations");

        try {
            List<Object> rules = new LinkedList<>();
            for (Relocation relocation : relocations) {
                rules.add(relocationConstructor.newInstance(
                        relocation.getPattern(),
                        relocation.getRelocatedPattern(),
                        relocation.getIncludes(),
                        relocation.getExcludes()
                ));
            }

            jarRelocatorRunMethod.invoke(jarRelocatorConstructor.newInstance(in.toFile(), out.toFile(), rules));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
