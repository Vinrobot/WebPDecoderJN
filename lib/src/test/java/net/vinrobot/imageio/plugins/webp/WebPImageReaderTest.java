package net.vinrobot.imageio.plugins.webp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import webpdecoderjn.TestResources;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebPImageReaderTest {
	@Test
	void isRegisteredForFormat() {
		// GIVEN
		final String formatName = "webp";

		// WHEN
		final ImageReader imageReader = ImageIO.getImageReadersByFormatName(formatName).next();

		// THEN
		assertEquals(WebPImageReader.class, imageReader.getClass());
	}

	private static WebPImageReader newImageReader() {
		return (WebPImageReader) ImageIO.getImageReadersByFormatName("webp").next();
	}

	@ParameterizedTest
	@MethodSource("webpdecoderjn.TestResources#getTestImages")
	void read(final TestResources.TestImage testData) throws IOException {
		// GIVEN
		final WebPImageReader imageReader = newImageReader();
		try (final InputStream inputStream = testData.resource().openStream();
		     final ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {
			imageReader.setInput(imageInputStream);

			// WHEN
			final List<BufferedImage> actualFrames = new ArrayList<>();
			for (int imageIndex = 0; ; ++imageIndex) {
				try {
					actualFrames.add(imageReader.read(imageIndex));
				} catch (IndexOutOfBoundsException e) {
					break;
				}
			}

			// THEN
			final TestResources.TestFrame[] expectedFrames = testData.frames();
			assertEquals(expectedFrames.length, actualFrames.size());

			for (int i = 0; i < expectedFrames.length; ++i) {
				final TestResources.TestFrame expectedFrame = expectedFrames[i];
				final BufferedImage actualFrame = actualFrames.get(i);

				assertEquals(expectedFrame.width(), actualFrame.getWidth());
				assertEquals(expectedFrame.height(), actualFrame.getHeight());
			}
		} finally {
			imageReader.dispose();
		}
	}
}
