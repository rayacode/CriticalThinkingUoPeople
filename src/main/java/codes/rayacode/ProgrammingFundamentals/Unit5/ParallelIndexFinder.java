package codes.rayacode.ProgrammingFundamentals.Unit5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ParallelIndexFinder {

    private static final AtomicLong foundIndex = new AtomicLong(-1);

    public static void main(String[] args) throws IOException {
        String filePath = "massive_data_2gb.txt";

        try (RandomAccessFile file = new RandomAccessFile(filePath, "r");
             FileChannel channel = file.getChannel()) {

            long fileSize = channel.size();
            long CHUNK_SIZE = 1L << 30;

            List<ChunkTask> tasks = new ArrayList<>();
            long position = 0;

            while (position < fileSize) {
                long size = Math.min(CHUNK_SIZE, fileSize - position);
                tasks.add(new ChunkTask(channel, position, size));
                position += size;
            }

            System.out.println("Processing " + fileSize + "\n");
            System.out.println("Reading system hardware info...");

            String cpuName = getCpuName();
            int cores = Runtime.getRuntime().availableProcessors();
            long memory = Runtime.getRuntime().totalMemory();

            System.out.println("------------------------------------------------");
            System.out.println("Processing " + memory + " bytes on:");
            System.out.println("CPU Model: " + cpuName);
            System.out.println("Cores:     " + cores + " Logical Processors");
            System.out.println("------------------------------------------------");
            long start = System.currentTimeMillis();
            tasks.parallelStream().forEach(task -> {
                try {
                    scanChunk(task);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            long end = System.currentTimeMillis();

            if (foundIndex.get() != -1) {
                System.out.println("FOUND at Index: " + foundIndex.get());
            } else {
                System.out.println("Not found.");
            }
            System.out.println("Time: " + (end - start) + "ms");
        }
    }

    record ChunkTask(FileChannel channel, long start, long size) {}

    private static void scanChunk(ChunkTask task) throws IOException {
        if (foundIndex.get() != -1) return;
        MappedByteBuffer buffer = task.channel.map(FileChannel.MapMode.READ_ONLY, task.start, task.size);
        int limit = buffer.limit();
        int pos = 0;
        while (pos < limit - 8) {
            if (foundIndex.get() != -1) return;
            long word = buffer.getLong(pos);
            if (hasLowerCase(word)) {
                for (int i = 0; i < 8; i++) {
                    byte b = buffer.get(pos + i);
                    if (b >= 'a' && b <= 'z') {
                        foundIndex.set(task.start + pos + i);
                        return;
                    }
                }
            }
            pos += 8;
        }

        while (pos < limit) {
            if (foundIndex.get() != -1) return;
            byte b = buffer.get(pos);
            if (b >= 'a' && b <= 'z') {
                foundIndex.set(task.start + pos);
                return;
            }
            pos++;
        }
    }

    private static boolean hasLowerCase(long word) {
        return isLower((byte)word) || isLower((byte)(word >> 8)) ||
                isLower((byte)(word >> 16)) || isLower((byte)(word >> 24)) ||
                isLower((byte)(word >> 32)) || isLower((byte)(word >> 40)) ||
                isLower((byte)(word >> 48)) || isLower((byte)(word >> 56));
    }

    private static boolean isLower(byte b) {
        return b >= 'a' && b <= 'z';
    }



    private static String getCpuName() {
        String os = System.getProperty("os.name").toLowerCase();
        String command = "";


        if (os.contains("win")) {

            command = "wmic cpu get name";
        } else if (os.contains("mac")) {

            command = "sysctl -n machdep.cpu.brand_string";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {

            String[] cmd = {
                    "/bin/sh", "-c", "grep -m 1 'model name' /proc/cpuinfo | awk -F: '{print $2}'"
            };
            return runCommand(cmd).trim();
        } else {
            return "Unknown CPU (OS: " + os + ")";
        }

        return runCommand(command.split(" ")).trim();
    }

    private static String runCommand(String[] command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                String line;
                StringBuilder output = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
       
                    if (!line.isEmpty() && !line.equalsIgnoreCase("Name")) {
                        output.append(line);
 
                        break;
                    }
                }
                return output.toString();
            }
        } catch (Exception e) {
            return "Unknown CPU (Error reading info)";
        }
    }
}