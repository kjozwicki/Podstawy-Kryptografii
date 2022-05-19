package krypto;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileReader {
    private byte[] bytes;

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes_global) {
        this.bytes = bytes_global;
    }

    public void read(Path name) {
        try {
            File file;
            bytes = Files.readAllBytes(name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save(byte[] bytes_global, Path name) {
        try {
            Files.write(name, bytes_global);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
