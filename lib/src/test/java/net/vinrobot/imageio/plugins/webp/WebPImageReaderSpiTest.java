package net.vinrobot.imageio.plugins.webp;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import webpdecoderjn.TestResources;

import javax.imageio.ImageIO;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WebPImageReaderSpiTest {
	@ParameterizedTest
	@MethodSource("webpdecoderjn.TestResources#getTestImages")
	void canDecodeByteArray(final TestResources.TestImage testData) throws IOException {
		// GIVEN
		final ImageReaderSpi serviceProvider = new WebPImageReaderSpi();
		final byte[] imageData;
		try (final InputStream inputStream = testData.resource().openStream()) {
			imageData = inputStream.readAllBytes();
		}

		// WHEN
		final boolean canDecode = serviceProvider.canDecodeInput(imageData);

		// THEN
		assertTrue(canDecode);
	}

	@ParameterizedTest
	@MethodSource("webpdecoderjn.TestResources#getTestImages")
	void canDecodeImageInputStream(final TestResources.TestImage testData) throws IOException {
		// GIVEN
		try (final InputStream inputStream = testData.resource().openStream();
		     final ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {
			final ImageReaderSpi imageReaderSpi = new WebPImageReaderSpi();

			// WHEN
			final boolean canDecode = imageReaderSpi.canDecodeInput(imageInputStream);

			// THEN
			assertTrue(canDecode);
		}
	}
}
