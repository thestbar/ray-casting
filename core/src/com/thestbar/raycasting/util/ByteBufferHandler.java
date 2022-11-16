package com.thestbar.raycasting.util;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class ByteBufferHandler {
    static final int bytes_per_datum = 4;

    public static void put_ints(ByteBuffer byteBuffer, int[] data) {
        IntBuffer intBuffer = byteBuffer.asIntBuffer(); // created IntBuffer starts only from the ByteBuffer's relative position
        // if you plan to reuse this IntBuffer, be mindful of its position
        intBuffer.put(data); // position of this IntBuffer changes by +data.length;
    } // this IntBuffer goes out of scope

    public static void print(ByteBuffer byteBuffer) { // prints from start to limit
        ByteBuffer byteBuffer2 = byteBuffer.duplicate(); // shares backing content, but has its own capacity/limit/position/mark (equivalent to original buffer at initialization)
        byteBuffer2.rewind();
        for (int x = 0, xx = byteBuffer2.limit(); x < xx; ++x) {
            System.out.print((byteBuffer2.get() & 0xFF) + " "); // 0xFF for display, since java bytes are signed
            if ((x + 1) % bytes_per_datum == 0) {
                System.out.print("\n");
            }
        }
    }
}
