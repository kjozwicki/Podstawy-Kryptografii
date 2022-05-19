package krypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Algorithm {
    //setter, getter
    public static byte[] getShift() {
        return shift;
    }

    public static byte[] getSbox() {
        return sBox;
    }

    public byte[][] getSubkeys()
    {
        final byte[] PC1 = {57, 49, 41, 33, 25, 17, 9, 1, 58, 50, 42, 34, 26, 18, 10, 2, 59, 51, 43, 35, 27, 19, 11, 3, 60, 52, 44, 36, 63, 55, 47, 39, 31, 23, 15, 7, 62, 54, 46, 38, 30, 22, 14, 6, 61, 53, 45, 37, 29, 21, 13, 5, 28, 20, 12, 4};
        final byte[] PC2 = {14, 17, 11, 24, 1, 5, 3, 28, 15, 6, 21, 10, 23, 19, 12, 4, 26, 8, 16, 7, 27, 20, 13, 2, 41, 52, 31, 37, 47, 55, 30, 40, 51, 45, 33, 48, 44, 49, 39, 56, 34, 53, 46, 42, 50, 36, 29, 32};
        final byte[] SHIFTS = {1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1};
        byte[] activeKey = new byte[0];
        for (int k = 0; k < 16; k++)
            activeKey = Converter.selectBits(this.b_key, PC1);
        int halfKeySize = 28;
        byte[] c = Converter.selectBits(activeKey, 0, halfKeySize);
        byte[] d = Converter.selectBits(activeKey, halfKeySize, halfKeySize);
        byte[][] subKeysLocal = new byte[16][];
        for (int k = 0; k < 16; k++)
        {
            c = Converter.rotateLeft(c, halfKeySize, SHIFTS[k]);
            d = Converter.rotateLeft(d, halfKeySize, SHIFTS[k]);
            byte[] cd = Converter.joinBlocks(c, halfKeySize, d, halfKeySize);
            subKeysLocal[k] = Converter.selectBits(cd, PC2);
        }
        return subKeysLocal;
    }
    //public
    public void setKeyHex(String key)
    {
        this.b_key = Converter.hexToBytes(key);
        if (testKey())
        {   this.s_key = key;
            subKeys = getSubkeys();
        }
    }

    public boolean testKey()
    {
        if (this.b_key == null)
            l.warn("Klucz jest za krótki");
        else
        {
            int l = this.b_key.length;
            if (l < 8)
            {
                this.b_key = null;
            }
            else if (l > 8)
            {
                this.b_key = null;
            }
            return true;
        }
        return false;
    }

    private byte[] encrypt(byte[] theMsg) throws Exception
    {
        if (theMsg.length != 8)
        {
            throw new Exception("Część wiadomości nie ma 8 bajtów długości");
        }

        byte[] r = Converter.computeRightBlock(theMsg);
        byte[] l = Converter.computeLeftBlock(theMsg);
        for (int k = 0; k < 16; k++)
        {
            byte[] rBackup = r;
            r = Converter.computeExtendedBlock(r);
            r = Converter.XORBytes(r, subKeys[k]);
            r = Converter.sBlocks(r);
            r = Converter.selectBits(r, pBlock);
            r = Converter.XORBytes(l, r);
            l = rBackup;
        }
        byte[] lr = Converter.computeResultBlock(l, r);
        return lr;
    }

    private byte[] decrypt(byte[] theMsg) throws Exception
    {
        if (theMsg.length != 8)
        {
            throw new Exception("Część wiadomości nie ma 8 bajtów długości");
        }

        byte[] r = Converter.computeRightBlock(theMsg);
        byte[] l = Converter.computeLeftBlock(theMsg);
        int numOfSubKeys = subKeys.length;
        for (int k = 0; k < numOfSubKeys; k++)
        {
            byte[] rBackup = r;
            r = Converter.computeExtendedBlock(r);
            r = Converter.XORBytes(r, subKeys[numOfSubKeys - k - 1]);
            r = Converter.sBlocks(r);
            r = Converter.selectBits(r, pBlock);
            r = Converter.XORBytes(l, r);
            l = rBackup;
        }
        byte[] lr = Converter.computeResultBlock(l, r);
        return lr;
    }

    public byte[] encodeBlock(byte[] data, int beginIndex) throws Exception
    {
        byte[] msg = new byte[8];
        System.arraycopy(data, beginIndex, msg, 0, 8);
        return encrypt(msg);
    }

    public byte[] decodeBlock(byte[] data, int beginIndex) throws Exception
    {
        byte[] msg = new byte[8];
        System.arraycopy(data, beginIndex, msg, 0, 8);
        return decrypt(msg);
    }

    public byte[] encode(byte[] message)
    {
        int len;
        //zapisanie dlugosc, w przypadku
        if ((message.length / 2 % 4) != 0)
            len = (message.length / 8 + 1) * 8;
        else
            len = message.length;
        byte[] result = new byte[len];
        byte[] tempBlock = new byte[8];
        byte[] rawData = null;
        try {
            rawData = message;
            for (int i = 0; i < (rawData.length / 8); i++)
            {
                tempBlock = encodeBlock(rawData, i * 8);
                System.arraycopy(tempBlock, 0, result, i * 8, 8);
            }
            if (message.length / 2 % 4 != 0)
            {
                for (int i = 0; i < 8; i++)
                {
                    if (i + (rawData.length / 8) * 8 < rawData.length)
                        tempBlock[i] = rawData[i + (rawData.length / 8) * 8];
                    else
                        tempBlock[i] = 0;
                }
                tempBlock = encodeBlock(tempBlock, 0);
                System.arraycopy(tempBlock, 0, result, (rawData.length / 8) * 8, 8);
            }
            return result;
        } catch (Exception ex) {};
        return null;
    }

    public byte[] decode(byte[] encrypted)
    {
        byte[] tmpResult = new byte[encrypted.length];
        byte[] tempBlock = new byte[8];
        byte[] rawData = null;
        try {
            rawData = encrypted;
            for (int i = 0; i < (rawData.length / 8); i++)
            {
                tempBlock = decodeBlock(rawData, i * 8);
                System.arraycopy(tempBlock, 0, tmpResult, i * 8, tempBlock.length);
            }
            int cnt = 0;
            for (int i = 1; i < 9; i += 2)
            {
                if (tmpResult[tmpResult.length - i] == 0 && tmpResult[tmpResult.length - i - 1] == 0)
                    cnt += 2;
                else
                    break;
            }
            byte[] result = new byte[tmpResult.length - cnt];
            System.arraycopy(tmpResult, 0, result, 0, tmpResult.length - cnt);
            return result;
        } catch (Exception ex) { };
        return null;
    }

    //wykonuje 3 razy algorytm DES za pomoca trzech roznych kluczy
    public byte[] encode3DES(byte[] message)
    {
        setKeyHex(s_key1);
        byte[] result = encode(message);
        setKeyHex(s_key2);
        result = decode(result);
        setKeyHex(s_key3);
        result = encode(result);
        return result;
    }

    //wykonuje 3 razy algorytm DES za pomoca trzech roznych kluczy
    public byte[] decode3DES (byte[] cipher)
    {
        setKeyHex(s_key3);
        byte[]result = decode(cipher);
        setKeyHex(s_key2);
        result = encode(result);
        setKeyHex(s_key1);
        result =decode(result);
        return result;
    }
    //private
    private static final Logger l = LoggerFactory.getLogger(App.class.getName());
    private String s_key = "000AAAFFF999555", s_key1 = "123123123123123", s_key2 = "ABCABCABCABCABC", s_key3 = "FEDCBA9876543210";
    private byte[] b_key;
    private byte subKeys[][];
    private static byte[] shift = {1, 3, 5, 7, 0, 2, 4, 6};
    private final byte[] pBlock = {16, 7, 20, 21, 29, 12, 28, 17, 1, 15, 23, 26, 5, 18, 31, 10, 2, 8, 24, 14, 32, 27, 3, 9, 19, 13, 30, 6, 22, 11, 4, 25};
    private static byte[] sBox =
            {
                    14, 4, 13, 1, 2, 15, 11, 8, 3, 10, 6, 12, 5, 9, 0, 7, // S1
                    0, 15, 7, 4, 14, 2, 13, 1, 10, 6, 12, 11, 9, 5, 3, 8,
                    4, 1, 14, 8, 13, 6, 2, 11, 15, 12, 9, 7, 3, 10, 5, 0,
                    15, 12, 8, 2, 4, 9, 1, 7, 5, 11, 3, 14, 10, 0, 6, 13,
                    15, 1, 8, 14, 6, 11, 3, 4, 9, 7, 2, 13, 12, 0, 5, 10, // S2
                    3, 13, 4, 7, 15, 2, 8, 14, 12, 0, 1, 10, 6, 9, 11, 5,
                    0, 14, 7, 11, 10, 4, 13, 1, 5, 8, 12, 6, 9, 3, 2, 15,
                    13, 8, 10, 1, 3, 15, 4, 2, 11, 6, 7, 12, 0, 5, 14, 9,
                    10, 0, 9, 14, 6, 3, 15, 5, 1, 13, 12, 7, 11, 4, 2, 8, // S3
                    13, 7, 0, 9, 3, 4, 6, 10, 2, 8, 5, 14, 12, 11, 15, 1,
                    13, 6, 4, 9, 8, 15, 3, 0, 11, 1, 2, 12, 5, 10, 14, 7,
                    1, 10, 13, 0, 6, 9, 8, 7, 4, 15, 14, 3, 11, 5, 2, 12,
                    7, 13, 14, 3, 0, 6, 9, 10, 1, 2, 8, 5, 11, 12, 4, 15, // S4
                    13, 8, 11, 5, 6, 15, 0, 3, 4, 7, 2, 12, 1, 10, 14, 9,
                    10, 6, 9, 0, 12, 11, 7, 13, 15, 1, 3, 14, 5, 2, 8, 4,
                    3, 15, 0, 6, 10, 1, 13, 8, 9, 4, 5, 11, 12, 7, 2, 14,
                    2, 12, 4, 1, 7, 10, 11, 6, 8, 5, 3, 15, 13, 0, 14, 9, // S5
                    14, 11, 2, 12, 4, 7, 13, 1, 5, 0, 15, 10, 3, 9, 8, 6,
                    4, 2, 1, 11, 10, 13, 7, 8, 15, 9, 12, 5, 6, 3, 0, 14,
                    11, 8, 12, 7, 1, 14, 2, 13, 6, 15, 0, 9, 10, 4, 5, 3,
                    12, 1, 10, 15, 9, 2, 6, 8, 0, 13, 3, 4, 14, 7, 5, 11, // S6
                    10, 15, 4, 2, 7, 12, 9, 5, 6, 1, 13, 14, 0, 11, 3, 8,
                    9, 14, 15, 5, 2, 8, 12, 3, 7, 0, 4, 10, 1, 13, 11, 6,
                    4, 3, 2, 12, 9, 5, 15, 10, 11, 14, 1, 7, 6, 0, 8, 13,
                    4, 11, 2, 14, 15, 0, 8, 13, 3, 12, 9, 7, 5, 10, 6, 1, // S7
                    13, 0, 11, 7, 4, 9, 1, 10, 14, 3, 5, 12, 2, 15, 8, 6,
                    1, 4, 11, 13, 12, 3, 7, 14, 10, 15, 6, 8, 0, 5, 9, 2,
                    6, 11, 13, 8, 1, 4, 10, 7, 9, 5, 0, 15, 14, 2, 3, 12,
                    13, 2, 8, 4, 6, 15, 11, 1, 10, 9, 3, 14, 5, 0, 12, 7, // S8
                    1, 15, 13, 8, 10, 3, 7, 4, 12, 5, 6, 11, 0, 14, 9, 2,
                    7, 11, 4, 1, 9, 12, 14, 2, 0, 6, 10, 13, 15, 3, 5, 8,
                    2, 1, 14, 7, 4, 10, 8, 13, 15, 12, 9, 0, 3, 5, 6, 11
            };
}
