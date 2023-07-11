package webpdecoderjn;

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
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
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
 * the {@link WebPLoader#init()} function, which must be run before any of the
 * decode or test functions. When not using {@link WebPLoader#init()} JNA will
 * look for the library in various places (consult the JNA docs for details).
 *
 * <p>
 * The functions using the native libraries may throw an
 * {@code UnsatisfiedLinkError}. Since this is an error it is recommended to
 * catch it explicitly instead of catching {@code Error} or {@code Throwable}.
 *
 * @author tduva
 */
public class WebPDecoder {
    /**
     * Decode a WebP image based on an url.
     *
     * @param url The url
     * @return A decoded {@link WebPImage}
     * @throws IOException          When loading the data from the url fails
     * @throws WebPDecoderException When the decoder encounters an issue (e.g.
     *                              if it's not a valid WebP file)
     * @throws UnsatisfiedLinkError When there was an issue loading the native
     *                              libraries (note that this is an error, not an exception)
     */
    public static WebPImage decode(URL url) throws IOException,
            WebPDecoderException,
            UnsatisfiedLinkError {
        final URLConnection c = url.openConnection();
        try (final InputStream inputStream = c.getInputStream()) {
            return decode(inputStream);
        }
    }

    /**
     * Decode a WebP image from an InputStream.
     *
     * @param inputStream The inputstream
     * @return A decoded {@link WebPImage}
     * @throws IOException          When loading the data from the url fails
     * @throws WebPDecoderException When the decoder encounters an issue (e.g.
     *                              if it's not a valid WebP file)
     * @throws UnsatisfiedLinkError When there was an issue loading the native
     *                              libraries (note that this is an error, not an exception)
     */
    public static WebPImage decode(InputStream inputStream) throws IOException,
            WebPDecoderException,
            UnsatisfiedLinkError {
        final byte[] webpData;
        try (final ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            final byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            webpData = result.toByteArray();
        }

        return decode(webpData);
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
        final LibWebP lib = WebPLoader.lib();

        List<WebPImageFrame> frames = new ArrayList<>();
        Pointer bytes = null;
        Pointer decoder = null;
        WebPAnimInfo info;
        try {
            bytes = lib.WebPMalloc(rawData.length);
            bytes.write(0, rawData, 0, rawData.length);

            WebPData data = new WebPData();
            data.bytes = bytes;
            data.length = new Size_T(rawData.length);

            decoder = lib.WebPAnimDecoderNewInternal(data, null, LibWebP.WEBP_DEMUX_ABI_VERSION);
            if (decoder == null) {
                throw new WebPDecoderException("Failed creating decoder, invalid image?");
            }

            info = new WebPAnimInfo();
            if (lib.WebPAnimDecoderGetInfo(decoder, info) == 0) {
                throw new WebPDecoderException("Failed getting decoder info");
            }

            int prevTimestamp = 0;
            while (lib.WebPAnimDecoderHasMoreFrames(decoder) == 1) {
                PointerByReference buf = new PointerByReference();
                IntByReference timestamp = new IntByReference();

                if (lib.WebPAnimDecoderGetNext(decoder, buf, timestamp) == 0) {
                    throw new WebPDecoderException("Error decoding next frame");
                }

                int delay = timestamp.getValue() - prevTimestamp;
                prevTimestamp = timestamp.getValue();

                BufferedImage image = createImage(buf.getValue(), info.canvas_width, info.canvas_height);
                frames.add(new WebPImageFrame(image, timestamp.getValue(), delay));
            }
        } finally {
            if (decoder != null) {
                lib.WebPAnimDecoderDelete(decoder);
            }
            if (bytes != null) {
                lib.WebPFree(bytes);
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
}
