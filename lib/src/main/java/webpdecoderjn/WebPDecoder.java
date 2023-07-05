package webpdecoderjn;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import webpdecoderjn.internal.LibWebP;
import webpdecoderjn.internal.Size_T;
import webpdecoderjn.internal.WebPAnimInfo;
import webpdecoderjn.internal.WebPData;

/**
 * Decode a WebP image using native libraries.
 *
 * <p>
 * The native library {@code libwebp_animdecoder} (custom compiled for this from
 * the libwebp project) needs to be made available before any decoding attempts.
 * For supported platforms they are packed in the JAR and can be extracted using
 * the {@link #init()} function, which must be run before any of the decode or
 * test functions. When not using {@link #init()} JNA will look for the library
 * in various places (consult the JNA docs for details).
 *
 * <p>
 * The functions using the native libraries may throw an
 * {@code UnsatisfiedLinkError}. Since this is an error it is recommended to
 * catch it explicitly instead of catching {@code Error} or {@code Throwable}.
 *
 * @author tduva
 */
public class WebPDecoder {

    private static final Logger LOGGER = Logger.getLogger(WebPDecoder.class.getName());

    //==========================
    // Library Loading
    //==========================
    private static final String LIB_NAME = "libwebp_animdecoder";

    private static boolean initialized = false;
    private static Path libPath = null;

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

    private static LibWebP libWebPInstance;

    private static synchronized LibWebP lib() {
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
     * This will have to be changed if JNA changes the prefix.
     */
    private static final String JNA_TMPLIB_PREFIX = "jna";

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

    //==========================
    // Decoding
    //==========================

    /**
     * Same as {@link #testEx()} but instead of throwing an exception it only
     * return {@code true} or {@code false}.
     *
     * @return {@code true} if everything seems ok, {@code false} otherwise
     */
    public static boolean test() {
        try {
            testEx();
            return true;
        } catch (IOException | UnsatisfiedLinkError ex) {
            // Just catch everything, any exception or error counts as a failure
            return false;
        }
    }

    /**
     * Decode a test image included in the JAR to check if the native libraries
     * are properly loaded and if everything seems to work.
     *
     * @throws IOException          If loading the test image fails
     * @throws WebPDecoderException If the decoder encounters an error
     * @throws UnsatisfiedLinkError When there was an issue loading the native
     *                              libraries (note that this is an error, not an exception)
     */
    public static void testEx() throws IOException, WebPDecoderException,
            UnsatisfiedLinkError {
        URL url = WebPDecoder.class.getResource("/image/test.webp");
        WebPImage image = decode(getBytesFromURL(url));
        boolean expectedResult = image.canvasWidth == 16 && image.canvasHeight == 16
                && image.frameCount == 2 && image.frames.size() == 2
                && image.loopCount == 1 && image.frames.get(0).delay == 480
                && image.frames.get(1).delay == 1280;
        if (!expectedResult) {
            throw new WebPDecoderException("Unexpected decode result");
        }
    }

    /**
     * Decode a WebP image based on the url in the given String.
     *
     * @param url The url
     * @return A decoded {@link WebPImage}
     * @throws IOException          When loading the data from the url fails
     * @throws WebPDecoderException When the decoder encounters an issue (e.g.
     *                              if it's not a valid WebP file)
     * @throws UnsatisfiedLinkError When there was an issue loading the native
     *                              libraries (note that this is an error, not an exception)
     */
    public static WebPImage decodeUrl(String url) throws IOException,
            WebPDecoderException,
            UnsatisfiedLinkError {
        byte[] rawData = getBytesFromURL(new URL(url));
        return decode(rawData);
    }

    /**
     * Decode a WebP image.
     *
     * @param rawData The raw bytes of the image
     * @return A decoded {@link WebPImage}
     * @throws WebPDecoderException When the decoder encounters an issue (e.g.
     *                              if it's not a valid WebP file)
     * @throws UnsatisfiedLinkError When there was an issue loading the native
     *                              libraries (note that this is an error, not an exception)
     */
    public static WebPImage decode(byte[] rawData) throws WebPDecoderException,
            UnsatisfiedLinkError {
        List<WebPImageFrame> frames = new ArrayList<>();
        Pointer bytes = null;
        Pointer decoder = null;
        WebPAnimInfo info;
        try {
            bytes = lib().WebPMalloc(rawData.length);
            bytes.write(0, rawData, 0, rawData.length);

            WebPData data = new WebPData();
            data.bytes = bytes;
            data.length = new Size_T(rawData.length);

            decoder = lib().WebPAnimDecoderNewInternal(data, null, LibWebP.WEBP_DEMUX_ABI_VERSION);
            if (decoder == null) {
                throw new WebPDecoderException("Failed creating decoder, invalid image?");
            }

            info = new WebPAnimInfo();
            if (lib().WebPAnimDecoderGetInfo(decoder, info) == 0) {
                throw new WebPDecoderException("Failed getting decoder info");
            }

            int prevTimestamp = 0;
            while (lib().WebPAnimDecoderHasMoreFrames(decoder) == 1) {
                PointerByReference buf = new PointerByReference();
                IntByReference timestamp = new IntByReference();

                if (lib().WebPAnimDecoderGetNext(decoder, buf, timestamp) == 0) {
                    throw new WebPDecoderException("Error decoding next frame");
                }

                int delay = timestamp.getValue() - prevTimestamp;
                prevTimestamp = timestamp.getValue();

                BufferedImage image = createImage(buf.getValue(), info.canvas_width, info.canvas_height);
                frames.add(new WebPImageFrame(image, timestamp.getValue(), delay));
            }
        } finally {
            if (decoder != null) {
                lib().WebPAnimDecoderDelete(decoder);
            }
            if (bytes != null) {
                lib().WebPFree(bytes);
            }
        }
        return new WebPImage(frames, info.canvas_width, info.canvas_height,
                info.loop_count, Color.BLACK, info.frame_count);
    }

    private static BufferedImage createImage(Pointer pixelData, int width, int height) {
        if (pixelData != null) {
            int[] pixels = pixelData.getIntArray(0, width * height);

            ColorModel colorModel = new DirectColorModel(32, 0x000000ff, 0x0000ff00, 0x00ff0000, 0xff000000);

            SampleModel sampleModel = colorModel.createCompatibleSampleModel(width, height);
            DataBufferInt db = new DataBufferInt(pixels, width * height);
            WritableRaster raster = WritableRaster.createWritableRaster(sampleModel, db, null);

            return new BufferedImage(colorModel, raster, false, new Hashtable<Object, Object>());
        }
        return null;
    }

    //==========================
    // General Helpers
    //==========================

    /**
     * Returns the current platform architecture as interpreted by JNA.
     *
     * @return String containing the current arch
     */
    public static String getArch() {
        return Platform.ARCH;
    }

    /**
     * Read all bytes from the given URL.
     *
     * @param url The URL
     * @return A byte array
     * @throws IOException If loading the bytes fails
     */
    public static byte[] getBytesFromURL(URL url) throws IOException {
        URLConnection c = url.openConnection();
        try (InputStream input = c.getInputStream()) {
            byte[] imageData = readAllBytes(input);
            return imageData;
        }
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = input.read(buffer, 0, buffer.length)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toByteArray();
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
