import org.apache.log4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class FileCellBlock {
    private static final Logger LOGGER = Logger.getLogger(FileCellBlock.class);

    private static final char NULL = '\0';

    private final int blockSize;
    private final File file;

    FileCellBlock(String file, int blockSize) throws IOException {
        this.blockSize = blockSize;
        this.file = new File(file);
        createFile();

    }

    private void createFile() throws IOException {
        if (!file.exists() && !file.createNewFile() && !file.exists()) {
            if (!file.createNewFile()) {
                if (!file.exists()) {
                    LOGGER.fatal("Cannot create file " + file.toString());
                    throw new IOException("Cannot create file " + file.toString());
                }
            }
            LOGGER.info(String.format("Created file DB: \"%s\"",file.toString()));
        }
    }

    private byte[] generateNullBlock() {
        // Default NULL Block.
        byte[] block = new byte[blockSize];
        Arrays.fill(block, (byte) NULL);
        return block;
    }

    private byte[] toBlock(String s) {
        byte[] string = s.replaceAll(String.valueOf(NULL),"").getBytes();
        if (string.length > blockSize) {
            return null;
        }
        byte[] block = generateNullBlock();
        System.arraycopy(string, 0, block, 0, string.length);
        return block;
    }

    private String toString(byte[] block) {
        if (Arrays.equals(block, generateNullBlock())) {
            return "<<FREE BLOCK>>";
        }
        String str = new String(block);
        int indexOfEnd = str.indexOf('\0');
        return indexOfEnd != -1 ? str.substring(0, str.indexOf(NULL)) : str;
    }

    private void writeBlockInEnd(final String s) {
        // Write s in the end
        byte[] block = toBlock(s);
        if (block == null) {
            return;
        }
        try (FileOutputStream out = new FileOutputStream(file,true)) {
            FileLock lock = out.getChannel().lock(out.getChannel().position(), blockSize, false);
                out.write(block);
            lock.release();
        } catch (IOException e) {
            LOGGER.error(String.format("Cannot write file '%s'",file.toString()));
        }
    }

    public void clean() {
        try (FileOutputStream ignored = new FileOutputStream(file,false)) {
            LOGGER.info(String.format("Cleaned file '%s'",file.toString()));
        } catch (IOException e) {
            LOGGER.error(String.format("Cannot clean file '%s'",file.toString()));
        }
    }

    private void writeBlock(final String s, int position) {
        // Write Block in position
        // NOTE: The file was block from position*blocSize a blockSize length
        // If position < 0. Write on end
        if (position < 0) {
            writeBlockInEnd(s);
            return;
        }
        byte[] block = toBlock(s);
        if (block == null) {
            return;
        }
        try (FileChannel out = FileChannel.open(file.toPath(), StandardOpenOption.WRITE)) {
            long offset = position * blockSize;
            FileLock lock = out.lock(position, blockSize, false);
                out.write(ByteBuffer.wrap(block), offset);
            lock.release();
        } catch (IOException e) {
            LOGGER.error(String.format("Cannot write file '%s'",file.toString()));
        }
    }

    public void iterFile(final BiPredicate<Integer, String> handleBlock) {
        // Iter file lockin to read per block
        // If handleBlock return true. Stop iter.
        try (FileInputStream in = new FileInputStream(file)) {
            int byteCount = 0;
            long offset = 0L;
            int position = 0;
            boolean stop = false;
            while (byteCount != -1 && !stop) {
                FileLock lock = in.getChannel().lock(offset, blockSize, true);
                byte[] bytes = new byte[blockSize];
                byteCount = in.read(bytes);
                if (byteCount != -1) {
                    stop = handleBlock.test(position, toString(bytes));
                }
                lock.release();
                offset += blockSize;
                position += 1;
            }
        } catch (IOException e) {
            LOGGER.error(String.format("Cannot write file '%s'",file.toString()));
            LOGGER.debug(e);
        }
    }

    public void iterFile(final Consumer<String> handleBlock) {
        // Iter file. Pass handler to handled al String per block
        iterFile((pos, string) -> {
            handleBlock.accept(string);
            return false;
        });
    }

    private int getPosition(String s, Comparator<String> comparator) {
        AtomicInteger position = new AtomicInteger(-1);
        iterFile((pos, string) -> {
            if ( comparator.compare(s, string) == 0) {
                position.set(pos);
                return true;
            }
            return false;
        });
        return position.get();
    }

    public void update(String defaultValue, Function<String, String> updater) {
        // Iter all block and collect position who updater change value of block
        // If collected position is null. Write in the end default value
        Map<Integer, String> newPositionsStrings = new HashMap<>();
        Map<Integer, String> oldPositionsStrings = new HashMap<>();
        iterFile((pos, string) -> {
            String newValue = updater.apply(string);
            if (!string.equalsIgnoreCase(newValue)) {
                newPositionsStrings.put(pos, newValue);
                oldPositionsStrings.put(pos, string);
            }
            return false;
        });
        if (!newPositionsStrings.isEmpty()) {
            newPositionsStrings.forEach((key, value) -> {
                writeBlock(value, key);
                LOGGER.debug(String.format("Updated block %d.\n'%s' -> '%s'", key, oldPositionsStrings.get(key), value));
            });
        } else {
            writeBlockInEnd(defaultValue);
            LOGGER.debug(String.format("Update not find block to update. Write '%s' on the end", defaultValue));
        }

    }

    public void insert(String s) {
        // Write block in first free position.
        // Is no exist free position. write in the end
        String nullString = toString(generateNullBlock());
        writeBlock(s, getPosition(nullString, String::compareTo));
    }

    public void delete(String s, Comparator<String> comparator) {
        // Delete first block who match with s
        // Do nothing if not exist
        String nullString = toString(generateNullBlock());
        int position = getPosition(s, comparator);
        if (position != -1) {
            writeBlock(nullString, position);
        }
    }

    public void delete(Predicate<String> predicate) {
        // Delete blocks who predicate(stringBlock) returns true
        List<Integer> positionsToDelete = new ArrayList<>();
        iterFile((pos, string) -> {
            if (predicate.test(string)) {
                positionsToDelete.add(pos);
            }
            return false;
        });
        positionsToDelete.forEach(pos -> writeBlock(toString(generateNullBlock()), pos));
    }

    private boolean isNotNull(String s) {
        return !s.equalsIgnoreCase(toString(generateNullBlock()));
    }

    public List<String> find(Predicate<String> comparator) {
        // Iter al blocks and collect who predicate return true
        List<String> collect = new ArrayList<>();
        iterFile((pos, string) -> {
            if (isNotNull(string) && comparator.test(string)) {
                collect.add(string);
            }
            return false;
        });
        return collect;
    }

    public File getFile() {
        return this.file;
    }
}
