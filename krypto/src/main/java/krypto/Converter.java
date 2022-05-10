package krypto;

import javax.swing.*;

public class Converter {
    //setter, getter
    public static int getBitAt(byte[] data, int poz)
    {
        int posByte = poz / 8;
        int posBit = poz % 8;
        byte valByte = data[posByte];
        int valInt = valByte >> (7 - posBit) & 1;
        return valInt;
    }

    public static void setBitAt(byte[] data, int pos, int val)
    {
        byte oldByte = data[pos / 8];
        oldByte = (byte) (((0xFF7F >> (pos % 8)) & oldByte) & 0x00FF);
        byte newByte = (byte) ((val << (7 - (pos % 8))) | oldByte);
        data[pos / 8] = newByte;
    }
    //public
    public static byte[] hexToBytes(String tekst)
    {
        if (tekst == null) { return null;}
        else if (tekst.length() < 2) { return null;}
        else { if (tekst.length()%2!=0)tekst+='0';
            int dl = tekst.length() / 2;
            byte[] wynik = new byte[dl];
            for (int i = 0; i < dl; i++)
            { try{
                wynik[i] = (byte) Integer.parseInt(tekst.substring(i * 2, i * 2 + 2), 16);
            }catch(NumberFormatException e){
                JOptionPane.showMessageDialog(null, "Problem z przekonwertowaniem HEX->BYTE.\n Sprawdź wprowadzone dane.", "Problem z przekonwertowaniem HEX->BYTE", JOptionPane.ERROR_MESSAGE); }
            }
            return wynik;
        }
    }

    public static String bytesToHex(byte bytes[])
    {
        byte rawData[] = bytes;
        StringBuilder hexText = new StringBuilder();
        String initialHex = null;
        int initHexLength = 0;

        for (int i = 0; i < rawData.length; i++)
        {
            int positiveValue = rawData[i] & 0x000000FF;
            initialHex = Integer.toHexString(positiveValue);
            initHexLength = initialHex.length();
            while (initHexLength++ < 2)
            {
                hexText.append("0");
            }
            hexText.append(initialHex);
        }
        return hexText.toString();
    }

    public static byte[] XORBytes(byte[] a, byte[] b)
    {
        byte[] out = new byte[a.length];
        for (int i = 0; i < a.length; i++)
        {
            out[i] = (byte) (a[i] ^ b[i]);
        }
        return out;
    }

    public static byte[] joinBlocks(byte[] a, int aLen, byte[] b, int bLen)
    {
        int numOfBytes = (aLen + bLen - 1) / 8 + 1;
        byte[] out = new byte[numOfBytes];
        int j = 0;
        for (int i = 0; i < aLen; i++)
        {
            int val = getBitAt(a, i);
            setBitAt(out, j, val);
            j++;
        }
        for (int i = 0; i < bLen; i++)
        {
            int val = getBitAt(b, i);
            setBitAt(out, j, val);
            j++;
        }
        return out;
    }

    public static byte[] selectBits(byte[] in, int pos, int len)
    {
        int numOfBytes = (len - 1) / 8 + 1;
        byte[] out = new byte[numOfBytes];
        for (int i = 0; i < len; i++) {
            int val = getBitAt(in, pos + i);
            setBitAt(out, i, val);
        }
        return out;
    }

    public static byte[] rotateLeft(byte[] in, int len, int step)
    {
        byte[] out = new byte[(len - 1) / 8 + 1];
        for (int i = 0; i < len; i++)
        {
            int val = getBitAt(in, (i + step) % len);
            setBitAt(out, i, val);
        }
        return out;
    }

    public static byte[] computeRightBlock(byte[] in)
    {
        byte[] out = new byte[4];
        byte current;
        for (byte byteCounter = 0; byteCounter < 4; byteCounter++)
        {
            for (byte bitCounter = 7; bitCounter >= 0; bitCounter--)
            {
                current = (byte) (in[bitCounter] >>> Algorithm.getShift()[byteCounter]);
                current = (byte) (current & 1);
                current = (byte) (current << (bitCounter));
                out[3 - byteCounter] = (byte) (out[3 - byteCounter] | current);
            }
        }
        return out;
    }

    public static byte[] computeLeftBlock(byte[] in)
    {
        byte[] out = new byte[4];
        byte current;
        for (byte byteCounter = 4; byteCounter < 8; byteCounter++)
        {
            for (byte bitCounter = 7; bitCounter >= 0; bitCounter--)
            {
                current = (byte) (in[bitCounter] >>> Algorithm.getShift()[byteCounter]);
                current = (byte) (current & 1);
                current = (byte) (current << (bitCounter));
                out[7 - byteCounter] = (byte) (out[7 - byteCounter] | current);
            }
        }
        return out;
    }

    public static byte[] computeExtendedBlock(byte[] block)
    {
        byte extendedBlock[] = new byte[6];
        short current;
        byte pBit = 31;
        byte changer = 0;
        for (int bit = 0; bit < 48; bit++)
        {
            current = (short) (block[pBit / 8] >> (7 - (pBit % 8)));
            current = (short) (current & 1);
            current = (short) (current << (7 - (bit % 8)));
            extendedBlock[bit / 8] = (byte) (extendedBlock[bit / 8] | (current));
            if (++changer == 6)
            {
                changer = 0;
                pBit--;
            }
            else
                pBit = (byte) ((++pBit) % 32);
        }
        return extendedBlock;
    }

    public static byte[] computeResultBlock(byte[] l, byte[] r)
    {
        byte[] data = new byte[8];
        byte[] result = new byte[8];
        byte current;
        System.arraycopy(l, 0, data, 4, 4);
        System.arraycopy(r, 0, data, 0, 4);
        int currBit = 0;
        for (int readBitPos = 7; readBitPos >= 0; readBitPos--)
        {
            for (int readBytePos = 4; readBytePos < 8; readBytePos++)
            {
                current = (byte) (data[readBytePos] >> (7 - (readBitPos)));
                current = (byte) (current & 1);
                current = (byte) (current << (7 - (currBit % 8)));
                result[currBit / 8] = (byte) (result[currBit / 8] | current);
                currBit += 2;
            }
        }
        currBit = 1;
        for (int readBitPos = 7; readBitPos >= 0; readBitPos--)
        {
            for (int readBytePos = 0; readBytePos < 4; readBytePos++)
            {
                current = (byte) (data[readBytePos] >> (7 - (readBitPos)));
                current = (byte) (current & 1);
                current = (byte) (current << (7 - (currBit % 8)));
                result[currBit / 8] = (byte) (result[currBit / 8] | current);
                currBit += 2;
            }
        }
        return result;
    }

    public static byte[] sBlocks(byte[] data)
    {
        byte row;
        byte col;
        data = create6BitData(data);
        byte[] result = new byte[data.length / 2];
        byte lowerHalfByte = 0;
        byte halfByte;
        for (int b = 0; b < data.length; b++)
        {
            row = (byte) (((data[b] >> 6) & 2) | ((data[b] >> 2) & 1));
            col = (byte) ((data[b] >> 3) & 15);
            halfByte = Algorithm.getSbox()[64 * b + 16 * row + col];
            if (b % 2 == 0)
                lowerHalfByte = halfByte;
            else
                result[b / 2] = (byte) (16 * lowerHalfByte + halfByte);
        }
        return result;
    }

    public static byte[] create6BitData(byte[] data)
    {
        int numOfBytes = (8 * data.length - 1) / 6 + 1;
        byte[] out = new byte[numOfBytes];
        for (int i = 0; i < numOfBytes; i++)
        {
            for (int j = 0; j < 6; j++)
            {
                int val = getBitAt(data, 6 * i + j);
                setBitAt(out, 8 * i + j, val);
            }
        }
        return out;
    }
}
