package webpdecoderjn;

import java.io.IOException;
import java.net.URL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WebPDecoderTest {
    @BeforeAll
    static void setUp() throws IOException {
        WebPLoader.init();
    }

    @Test
    void decode() throws IOException {
        // GIVEN
        final URL imageUrl = WebPDecoderTest.class.getResource("/images/test.webp");
        final byte[] imageData = WebPDecoder.getBytesFromURL(imageUrl);

        // WHEN
        WebPImage image = WebPDecoder.decode(imageData);

        // THEN
        assertEquals(16, image.canvasWidth);
        assertEquals(16, image.canvasHeight);
        assertEquals(2, image.frameCount);
        assertEquals(2, image.frames.size());
        assertEquals(1, image.loopCount);
        assertEquals(480, image.frames.get(0).delay);
        assertEquals(1280, image.frames.get(1).delay);
    }
}
