package com.android.volley.toolbox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class IOUtils {

    private static final int BUFFER_SIZE = 4096;

    public static byte[] byteArrayFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int bytesRead;
        byte[] data = new byte[BUFFER_SIZE];

        while((bytesRead=inputStream.read(data,0,data.length)) !=-1) {
            buffer.write(data, 0, bytesRead);
        }

        buffer.flush();

        return buffer.toByteArray();
    }


}
