package webpdecoderjn;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.logging.Logger;
import webpdecoderjn.internal.LibWebP;

public final class WebPLoader {
    private static final Logger LOGGER = Logger.getLogger(WebPLoader.class.getName());

    private static final String LIB_NAME = "libwebp_animdecoder";
    /**
     * This will have to be changed if JNA changes the prefix.
     */
    private static final String JNA_TMPLIB_PREFIX = "jna";
    private static boolean initialized = false;
    private static Path libPath = null;
    private static LibWebP libWebPInstance;

    /**
     * Returns the current platform architecture as interpreted by JNA.
     *
     * @return String containing the current arch
     */
    public static String getArch() {
        return Platform.ARCH;
    }

    /**
     * This function is intended to be used before decoding is attempted (or
     * {@link #test()} is used). Extracts the platform dependent library from
     * the JAR to a temp folder chosen by JNA.
     *
     * <p>
     * When this function is not used the regular native library discovery
     * mechanism of JNA will be used.
     *
     * @throws IOException When extracting a library fails, in which case it may
     *                     not be possible to decode images
     * @see #init(boolean)
     */
    public static synchronized void init() throws IOException {
        init(false);
    }

    /**
     * This function is intended to be used before decoding is attempted (or
     * {@link #test()} is used). Extracts the platform dependent library from
     * the JAR to a temp folder chosen by JNA.
     *
     * <p>
     * When this function is not used the regular native library discovery
     * mechanism of JNA will be used.
     *
     * @param nextToJar Check if the native library can be found next to the JAR
     *                  (same directory) this class is contained in, otherwise extract from the
     *                  JAR as normal
     * @throws IOException When extracting a library fails, in which case it may
     *                     not be possible to decode images
     */
    public static synchronized void init(boolean nextToJar) throws IOException {
        if (!initialized) {
            if (nextToJar) {
                libPath = findNextToJar();
            }
            if (libPath == null) {
                libPath = extractLib(LIB_NAME);
            }
            initialized = true;
        }
    }

    static synchronized LibWebP lib() {
        if (libWebPInstance == null) {
            libWebPInstance = Native.load(libPath != null ? libPath.toString() : LIB_NAME, LibWebP.class);
            removeLibrary(libPath);
        }
        return libWebPInstance;
    }

    private static Path findNextToJar() {
        Path jarPath = getJarPath();
        if (jarPath == null) {
            if (debugEnabled()) {
                LOGGER.info("Find library next to JAR: path not found");
            }
            return null;
        }
        if (debugEnabled()) {
            LOGGER.info("Find library next to JAR: Checking " + jarPath);
        }
        String fileName = null;
        if (Platform.isWindows()) {
            fileName = LIB_NAME + ".dll";
        } else if (Platform.isLinux()) {
            fileName = LIB_NAME + ".so";
        } else if (Platform.isMac()) {
            fileName = LIB_NAME + ".dylib";
        }
        if (fileName != null) {
            Path libPath = jarPath.resolveSibling(fileName);
            if (Files.isRegularFile(libPath)) {
                return libPath;
            }
            libPath = jarPath.resolveSibling("lib" + fileName);
            if (Files.isRegularFile(libPath)) {
                return libPath;
            }
        }
        return null;
    }

    /**
     * Extract the library from the JAR.
     *
     * @param name
     * @return
     * @throws IOException
     */
    private static Path extractLib(String name) throws IOException {
        return Native.extractFromResourcePath(name).toPath();
    }

    /**
     * On Windows JNA can't remove the temp file while it's still in use, so
     * an ".x" file with the same name is created so it can be deleted on the
     * next start. When using the extract function directly that is not done by
     * JNA, so do it here.
     *
     * @param path
     */
    private static void removeLibrary(Path path) {
        if (path == null) {
            return;
        }
        boolean isUnpacked = path.getFileName().toString().startsWith(JNA_TMPLIB_PREFIX);
        if (!isUnpacked || !Files.isRegularFile(path)) {
            return;
        }
        // Delete, if possible (depending on OS if it's in use)
        if (path.toFile().delete()) {
            return;
        }
        try {
            Files.createFile(path.resolveSibling(path.getFileName() + ".x"));
        } catch (IOException ex) {
            if (debugEnabled()) {
                LOGGER.warning("Couldn't create x file for " + path);
            }
        }
    }

    private static boolean debugEnabled() {
        return Objects.equals(System.getProperty("jna.debug_load"), "true");
    }

    public static Path getJarPath() {
        try {
            Path jarPath = Paths.get(WebPDecoder.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!jarPath.toString().endsWith(".jar")
                    || !Files.exists(jarPath)
                    || !Files.isRegularFile(jarPath)) {
                jarPath = null;
            }
            return jarPath;
        } catch (URISyntaxException ex) {
            LOGGER.warning("jar: " + ex);
            return null;
        }
    }
}
