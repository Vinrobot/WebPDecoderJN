package net.vinrobot.imageio.plugins.webp;

import webpdecoderjn.internal.LibWebP;
import webpdecoderjn.internal.Loader;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Locale;
import java.util.Objects;

public final class WebPImageReaderSpi extends ImageReaderSpi {
	public WebPImageReaderSpi() {
		super(
				"Vinrobot",
				"1.0",
				new String[]{"webp", "WEBP", "wbp", "WBP"},
				new String[]{"wbp", "webp"},
				new String[]{"image/webp", "image/x-webp"},
				WebPImageReader.class.getName(),
				new Class[]{ImageInputStream.class, byte[].class},
				null,
				false,
				null,
				null,
				null,
				null,
				false,
				null,
				null,
				null,
				null
		);
	}

	@Override
	public boolean canDecodeInput(final Object source) throws IOException {
		if (source instanceof ImageInputStream) {
			return canDecodeInput((ImageInputStream) source);
		} else if (source instanceof byte[]) {
			return canDecodeInput((byte[]) source);
		} else {
			return false;
		}
	}

	public boolean canDecodeInput(final ImageInputStream stream) throws IOException {
		final ByteOrder originalOrder = stream.getByteOrder();
		stream.mark();

		try {
			// RIFF native order is Little Endian
			stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);

			// Check file header
			// https://developers.google.com/speed/webp/docs/riff_container#webp_file_header

			if (stream.readInt() != WebP.RIFF_MAGIC) {
				return false;
			}

			stream.readInt(); // Skip file size

			if (stream.readInt() != WebP.WEBP_MAGIC) {
				return false;
			}

			// Check first chunk type
			switch (stream.readInt()) {
				case WebP.CHUNK_VP8L, WebP.CHUNK_VP8X, WebP.CHUNK_VP8_:
					break;
				default:
					return false;
			}

			try {
				// The reader needs the native library to work
				return Loader.getInstance() != null;
			} catch (Exception ex) {
				// Unable to load native library
				return false;
			}
		} finally {
			stream.setByteOrder(originalOrder);
			stream.reset();
		}
	}

	public boolean canDecodeInput(final byte[] data) {
		// Check file header
		// https://developers.google.com/speed/webp/docs/riff_container#webp_file_header

		if (data.length < 16 || readInt(data, 0) != WebP.RIFF_MAGIC || readInt(data, 8) != WebP.WEBP_MAGIC) {
			return false;
		}

		switch (readInt(data, 12)) {
			case WebP.CHUNK_VP8L, WebP.CHUNK_VP8X, WebP.CHUNK_VP8_:
				break;
			default:
				return false;
		}

		try {
			// The reader needs the native library to work
			return Loader.getInstance() != null;
		} catch (Exception ex) {
			// Unable to load native library
			return false;
		}
	}

	private static int readInt(final byte[] buffer, final int offset) {
		return buffer[offset] | buffer[offset + 1] << 8 | buffer[offset + 2] << 16 | buffer[offset + 3] << 24;
	}

	@Override
	public ImageReader createReaderInstance(final Object extension) {
		final LibWebP lib = Objects.requireNonNull(Loader.getInstance(), "Unable to load native library");
		return new WebPImageReader(this, lib);
	}

	@Override
	public String getDescription(final Locale locale) {
		return "Google WebP File Format (WebP) Reader";
	}
}
