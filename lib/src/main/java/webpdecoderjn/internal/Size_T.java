package webpdecoderjn.internal;

import com.sun.jna.IntegerType;
import com.sun.jna.Native;

public class Size_T extends IntegerType {
    public static final Size_T ZERO = new Size_T();
    private static final long serialVersionUID = 1L;

    public Size_T() {
        this(0);
    }

    public Size_T(long value) {
        super(Native.SIZE_T_SIZE, value, true);
    }
}
