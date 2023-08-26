package webpdecoderjn.internal;

import com.sun.jna.Structure;

public record WebPAnimInfo(
		int canvasWidth,
		int canvasHeight,
		int loopCount,
		int frameCount
) {
	public static WebPAnimInfo copy(final Struct struct) {
		return new WebPAnimInfo(struct.canvas_width, struct.canvas_height, struct.loop_count, struct.frame_count);
	}

	/*
	[webp/demux.h]
	    // Global information about the animation..
	    struct WebPAnimInfo {
	      uint32_t canvas_width;
	      uint32_t canvas_height;
	      uint32_t loop_count;
	      uint32_t bgcolor;
	      uint32_t frame_count;
	      uint32_t pad[4];   // padding for later use
	    };
	*/
	@Structure.FieldOrder({"canvas_width", "canvas_height", "loop_count", "bgcolor", "frame_count", "pad"})
	public static class Struct extends Structure {
		public int canvas_width;
		public int canvas_height;
		public int loop_count;
		public int bgcolor;
		public int frame_count;
		public int[] pad = new int[4];
	}
}
