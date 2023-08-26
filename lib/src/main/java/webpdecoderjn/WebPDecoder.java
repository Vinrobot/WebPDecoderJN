package webpdecoderjn;


import webpdecoderjn.internal.LibWebP;
import webpdecoderjn.internal.WebPAnimDecoder;
import webpdecoderjn.internal.WebPAnimInfo;
import webpdecoderjn.internal.WebPData;
import webpdecoderjn.internal.WebPFrame;

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
import java.util.List;

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
    public static WebPImage decode(final URL url) throws IOException, UnsatisfiedLinkError {
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
    public static WebPImage decode(final InputStream inputStream) throws IOException, UnsatisfiedLinkError {
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
    public static WebPImage decode(final byte[] rawData) throws IOException, UnsatisfiedLinkError {
        final LibWebP lib = WebPLoader.lib();
        try (final WebPData data = new WebPData(lib, rawData); final WebPAnimDecoder decoder = new WebPAnimDecoder(lib, data)) {
            final WebPAnimInfo info = decoder.getInfo();

            final List<WebPImageFrame> frames = new ArrayList<>();
            int prevTimestamp = 0;
            while (decoder.hasMoreFrames()) {
                final WebPFrame frame = decoder.getNext(info);

                final int timestamp = frame.timestamp();
                final int delay = timestamp - prevTimestamp;
                prevTimestamp = timestamp;

                BufferedImage image = createImage(frame.pixels(), info.canvasWidth(), info.canvasHeight());
                frames.add(new WebPImageFrame(image, timestamp, delay));
            }
            return new WebPImage(frames, info.canvasWidth(), info.canvasHeight(), info.loopCount(), Color.BLACK, info.frameCount());
        }
    }

    private static BufferedImage createImage(final int[] pixels, final int width, final int height) {
        assert pixels.length == width * height;
        final ColorModel colorModel = new DirectColorModel(32, 0x000000ff, 0x0000ff00, 0x00ff0000, 0xff000000);
        final SampleModel sampleModel = colorModel.createCompatibleSampleModel(width, height);
        final DataBufferInt dataBufferInt = new DataBufferInt(pixels, width * height);
        final WritableRaster writableRaster = WritableRaster.createWritableRaster(sampleModel, dataBufferInt, null);
        return new BufferedImage(colorModel, writableRaster, false, null);
    }
}
