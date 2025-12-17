package codes.rayacode.ProgrammingFundamentals.Unit5;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;

public class GenerateMassiveData {

    public static void main(String[] args) {
        String fileName = "massive_data_2gb.txt";

        long targetSize = 2L * 1024 * 1024 * 1024;

        int bufferSize = 64 * 1024 * 1024;

        System.out.println("Generating " + (targetSize / (1024 * 1024 * 1024)) + "GB file...");
        long startTime = System.nanoTime();

        try (FileChannel channel = FileChannel.open(Path.of(fileName),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {

            ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
            fillWithRandomUppercase(buffer);

            long bytesWritten = 0;
            while (bytesWritten < targetSize) {

                long remaining = targetSize - bytesWritten;
                if (remaining < bufferSize) {
                    buffer.limit((int) remaining);
                }

                while (buffer.hasRemaining()) {
                    bytesWritten += channel.write(buffer);
                }

                buffer.clear();
            }


            System.out.println("Data written. Injecting lowercase char at the end...");

            channel.position(targetSize - 1);

            ByteBuffer lastByte = ByteBuffer.wrap(new byte[]{'z'});
            channel.write(lastByte);

            long endTime = System.nanoTime();
            System.out.println("Success! File: " + fileName);
            System.out.println("Time taken: " + (endTime - startTime) / 1_000_000 + " ms");
            System.out.println("Speed: ~" + (targetSize / 1024 / 1024 / ((endTime - startTime) / 1_000_000_000.0)) + " MB/s");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void fillWithRandomUppercase(ByteBuffer buffer) {
        Random random = new Random();
        byte[] temp = new byte[buffer.capacity()];
        for (int i = 0; i < temp.length; i++) {
            temp[i] = (byte) (random.nextInt(26) + 'A');
        }
        buffer.put(temp);
        buffer.flip();
    }
}