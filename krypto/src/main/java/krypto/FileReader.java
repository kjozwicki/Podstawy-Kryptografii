package krypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class FileReader {
    private String path;
    private File file;
    private FileInputStream fileInputStream = null;
    private byte[] bytes;

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public void read(String name) {
        try {
            file = new File(path);
            bytes = new byte[(int) file.length()];
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bytes);
            fileInputStream.close();

            for(int i = 0; i < bytes.length; i++)
                System.out.print((char) bytes[i]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save(byte[] bytes) {
        try {
            file = new File("nowy.txt");
            OutputStream os = new FileOutputStream(file);
            os.write(bytes);
            System.out.println("Plik zapisany");
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
