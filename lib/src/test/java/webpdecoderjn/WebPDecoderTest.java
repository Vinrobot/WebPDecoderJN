package webpdecoderjn;

import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

public class WebPDecoderTest {
    @BeforeAll
    static void setUp() throws IOException {
        WebPLoader.init();
    }

    @ParameterizedTest
    @MethodSource("webpdecoderjn.TestResources#getTestImages")
    void decode(TestResources.TestImage testData) throws IOException {
        // GIVEN
        final byte[] imageData = WebPDecoder.getBytesFromURL(testData.resource());

        // WHEN
        WebPImage image = WebPDecoder.decode(imageData);

        // THEN
        assertEquals(testData.width(), image.canvasWidth);
        assertEquals(testData.height(), image.canvasHeight);
        assertEquals(testData.loopCount(), image.loopCount);

        final TestResources.TestFrame[] expectedFrames = testData.frames();
        assertEquals(expectedFrames.length, image.frameCount);
        assertEquals(expectedFrames.length, image.frames.size());

        for (int i = 0; i < expectedFrames.length; ++i) {
            final TestResources.TestFrame expectedFrame = expectedFrames[i];
            final WebPImageFrame actualFrame = image.frames.get(i);

            assertEquals(expectedFrame.width(), actualFrame.img.getWidth());
            assertEquals(expectedFrame.height(), actualFrame.img.getHeight());
            assertEquals(expectedFrame.delay(), actualFrame.delay);
        }
    }
}
