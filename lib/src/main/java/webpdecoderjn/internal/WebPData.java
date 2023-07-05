package webpdecoderjn.internal;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

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
public class WebPData extends Structure {
    public Pointer bytes;
    public Size_T length;
}
