package krypto;

import java.util.Random;

public class Signature {
    //private
    private String key;
    int len;
    long a,b,c,d,e,f,g,h;
    Random rand = new Random();
    //public
    public void generateKey() {
        int r = 512 + (int)rand.nextFloat()*512;
        boolean wartosc = false;
        while(wartosc == true) {
            if (r%64 == 0)
                wartosc = true;
            else
                r++;
        }
        len = r;
    }
}
