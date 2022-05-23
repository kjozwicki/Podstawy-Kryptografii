package krypto;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class Signature {
    //private
    BigInteger p,q,h,g,x,y,k,r,s,w,u1,u2,v,pm1,km1;
    MessageDigest digest;
    int keyLen=512; //ta wartość daje długość p=512
    int ilZnHex=keyLen/4;//ilość znaków hex wyświetlanych w polu klucza
    Random random=new Random();
    //konstruktor
    public Signature()
    {
        generateKey();
        try{
            digest=MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {ex.printStackTrace();}
    }
    //metody
    public void generateKey() {   //tworzymy losową liczbę bitów dla p
        int rand = 512 + (int) random.nextFloat() * 512;
        //następnie musimy ją dobić tak aby była wielokrotnością 64
        while (true) {
            if (rand % 64 == 0)
                break;
            else
                rand++;
        }
        keyLen = rand;
        q = BigInteger.probablePrime(160, new Random());
        BigInteger pom1, pom2;
        do {
            pom1 = BigInteger.probablePrime(keyLen, new Random());
            pom2 = pom1.subtract(BigInteger.ONE);
            pom1 = pom1.subtract(pom2.remainder(q));
        } while (!pom1.isProbablePrime(2));
        p = pom1;
        pm1 = p.subtract(BigInteger.ONE);
        h = new BigInteger(keyLen - 2, random);
        while (true){
            if (h.modPow(pm1.divide(q), p).compareTo(BigInteger.ONE) == 1)
                break;
            else
                h = new BigInteger(keyLen - 2, random);
        }
        g=h.modPow(pm1.divide(q),p);
        do
            x=new BigInteger(160-2,random);
        while (x.compareTo(BigInteger.ZERO) != 1);
        y=g.modPow(x,p);
    }
    public BigInteger[] podpisuj(byte[] tekst)
    {
        k=new BigInteger(160-2,random);
        r=g.modPow(k, p).mod(q);
        km1=k.modInverse(q);

        digest.update(tekst);
        BigInteger hash=new BigInteger(1, digest.digest());
        BigInteger pom=hash.add(x.multiply(r));
        s = km1.multiply(pom).mod(q);
        BigInteger podpis[]=new BigInteger[2];
        podpis[0]=r;
        podpis[1]=s;
        return podpis;
    }
    public boolean weryfikujBigInt(byte[] tekstJawny, BigInteger[] podpis)
    {
        digest.update(tekstJawny);
        BigInteger hash = new BigInteger(1, digest.digest());
        w=podpis[1].modInverse(q);
        u1=hash.multiply(w).mod(q);
        u2=podpis[0].multiply(w).mod(q);
        v=g.modPow(u1, p).multiply(y.modPow(u2, p)).mod(p).mod(q);
        if(v.compareTo(podpis[0])==0)
            return true;
        else
            return false;
    }
    //Gettery
    public BigInteger getP() {
        return p;
    }
    public BigInteger getQ() {
        return q;
    }
    public BigInteger getG() {
        return g;
    }
    public BigInteger getX() {
        return x;
    }
    public BigInteger getY() {
        return y;
    }
}
