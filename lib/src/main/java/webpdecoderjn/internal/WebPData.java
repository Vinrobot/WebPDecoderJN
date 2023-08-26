package webpdecoderjn.internal;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.io.Closeable;
import java.util.Objects;

public final class WebPData implements Closeable {
	private final LibWebP lib;
	private Struct struct;

	public WebPData(final LibWebP lib, final byte[] rawData) {
		if (rawData == null) {
			throw new NullPointerException("rawData == null");
		} else if (rawData.length == 0) {
			throw new IllegalArgumentException("rawData.length == 0");
		}

		this.lib = Objects.requireNonNull(lib, "lib == null");

		final Pointer bytes = lib.WebPMalloc(rawData.length);
		if (bytes == null) {
			throw new NullPointerException("Failed to allocate memory for WebPData");
		}
		bytes.write(0, rawData, 0, rawData.length);

		this.struct = new Struct();
		this.struct.bytes = bytes;
		this.struct.length = new Size_T(rawData.length);
	}

	Struct getStruct() {
		return this.struct;
	}

	@Override
	public void close() {
		if (this.struct != null) {
			this.lib.WebPFree(this.struct.bytes);
			this.struct.bytes = null;
			this.struct = null;
		}
	}

	/*
	[webp/mux_types.h]
	    // Data type used to describe 'raw' data, e.g., chunk data
	    // (ICC profile, metadata) and WebP compressed image data.
	    // 'bytes' memory must be allocated using WebPMalloc() and such.
	    struct WebPData {
	      const uint8_t* bytes;
	      size_t size;
	    };
	*/
	@Structure.FieldOrder({"bytes", "length"})
	public static class Struct extends Structure {
		public Pointer bytes;
		public Size_T length;
	}
}
