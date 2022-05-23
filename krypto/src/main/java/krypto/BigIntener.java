package krypto;

import java.util.Arrays;
import java.util.Objects;


public class BigIntener implements Comparable<BigIntener> {

    final int sign; //1 - dodatnie, -1 - ujemne, 0 - zero
    final int[] mag; //modul (system zapisu znak-modul

    //const
    static final long MASK = 0xffffffffL; //potrzebne przy konwersji z int do long

    public static final BigIntener ZERO = new BigIntener(new int[0], 0);

    private static final int MAG_MAX_LEN = (Integer.MAX_VALUE / Integer.SIZE + 1);

    private BigIntener(long n) {
        if (n < 0) {
            n = -n;
            sign = -1;
        }
        else {
            sign = 1;
        }

        int high = (int)(n >>> 32);
        if (high == 0) {
            mag = new int[1];
            mag[0] = (int)n;
        }
        else {
            mag = new int [2];
            mag[0] = high;
            mag[1] = (int)n;
        }
    }

    BigIntener(int[] magnitude, int signum) {
        this.sign = (magnitude.length == 0 ? 0 : signum); //check if zero, if not set sign
        this.mag = magnitude;
        if (mag.length >= MAG_MAX_LEN) {
            checkRange();
        }
    }

    public static BigIntener valueOf(long n) {
        if (n == 0) {
            return ZERO;
        }
        return new BigIntener(n);
    }

    private void checkRange() {
        if (mag.length > MAG_MAX_LEN || mag.length == MAG_MAX_LEN && mag[0] < 0) {
            throw new ArithmeticException("Supported range overflow");
        }
    }

    @SuppressWarnings("empty-statement")
    private static int[] sourceStripLeadingZeros(int[] val) {
        int valLen = val.length;
        int nonZeroByte;
        for (nonZeroByte = 0; nonZeroByte < valLen && val[nonZeroByte] == 0; nonZeroByte++);
        return nonZeroByte == 0 ? val : Arrays.copyOfRange(val, nonZeroByte, valLen);
    }

    public BigIntener multiply(BigIntener n) {
        return multiply(n, false);
    }

    private BigIntener multiply(BigIntener n, boolean recursive) {
        if (n.sign == 0 || sign == 0) {
            return ZERO;
        }

        int lenX = mag.length;

        if  (n == this && lenX > 20) {
            return square();
        }

        int lenY = n.mag.length;

        if ((lenX < 80) || (lenY < 80)) {
            int resSign = sign == n.sign ? 1 : -1;
            if (n.mag.length == 1) {
                return multiplyInt(mag, n.mag[0], resSign);
            }
            if (mag.length == 1) {
                return multiplyInt(n.mag, mag[0], resSign);
            }

            int[] res = multiplyLen(mag, lenX, n.mag, lenY, null);
            res = sourceStripLeadingZeros(res);
            return new BigIntener(res, resSign);
        }
        else {
            if (!recursive) {
                if (bitLength(mag, mag.length) + bitLength(n.mag, n.mag.length) > 32L * MAG_MAX_LEN) {
                    throw new ArithmeticException("Supported range overflow");
                }
            }
            return karatsuba(this, n);
        }
    }

    private static BigIntener multiplyInt(int[] x, int y, int signum) {
        if (Integer.bitCount(y) == 1) {
            return new BigIntener(leftShift(x, Integer.numberOfTrailingZeros(y)), signum);
        }
        int lenX = x.length;
        int[] magR = new int [lenX +1];
        long carry = 0;
        long yLong = y & MASK;
        int startR = magR.length - 1;
        for (int i = lenX - 1; i >= 0; i--) {
            long prod = (x[i] & MASK) * yLong + carry;
            magR[startR--] = (int)prod;
            carry = prod >>> 32;
        }
        if (carry == 0L) {
            magR = Arrays.copyOfRange(magR, 1, magR.length);
        }
        else {
            magR[startR] = (int)carry;
        }
        return new BigIntener(magR, signum);
    }

    private static int bitLength(int[] mag, int length) {
        if (length == 0) {
            return 0;
        }
        return ((length - 1) << 5) + intBitLength(mag[0]);
    }

    static int intBitLength(int n) {
        return 32 - Integer.numberOfLeadingZeros(n);
    }

    private static int[] leftShift(int[] mag, int n) {
        int ints = n >>> 5;
        int bits = n & 0x1f; //0x1f = 31
        int magLen = mag.length;
        int magNew[] = null;
        if (bits == 0) {
            magNew = new int [magLen + ints];
            System.arraycopy(mag, 0, magNew, 0, magLen);
        }
        else {
            int i = 0;
            int bits2 = 32 - bits;
            int highBits = mag[0] >>> bits2;
            if (highBits == 0) {
                magNew = new int[magLen + ints];
            }
            else {
                magNew = new int[magLen + ints + 1];
                magNew[i++] = highBits;
            }
            int iters = magLen - 1;
            Objects.checkFromToIndex(0, iters + 1, mag.length);
            Objects.checkFromToIndex(1, iters + i + 1, magNew.length);
            leftShiftHelper(magNew, mag, i, bits, iters);
            magNew[iters + i] = mag[iters] << bits;
        }
        return magNew;
    }

    private static void leftShiftHelper(int[] arrNew, int[] arrOld, int iNew, int shifts, int iters) {
        int rightShifts = 32 - shifts;
        int iOld = 0;
        while (iOld < iters) {
            arrNew[iNew++] = (arrOld[iOld++] << shifts) | (arrOld[iOld] >>> rightShifts);
        }
    }

    private static int[] multiplyLen(int[] x, int lenX, int[] y, int lenY, int[] z) {
        checkMultiplyLen(x, lenX);
        checkMultiplyLen(y, lenY);
        return implMultiplyLen(x, lenX, y, lenY, z);
    }

    private static void checkMultiplyLen(int[] arr, int len) {
        if (len <= 0) {
            return;
        }
        Objects.requireNonNull(arr);
        if (len > arr.length) {
            throw new ArrayIndexOutOfBoundsException(len - 1);
        }
    }

    private static int[] implMultiplyLen(int[] x, int lenX, int[] y, int lenY, int[] z) {
        int startX = lenX - 1;
        int startY = lenY - 1;
        if (z == null || z.length < (lenX + lenY)) {
            z = new int[lenX + lenY];
        }

        long carry = 0;
        for (int i = startY, k = startX + startY + 1; i >= 0; i--, k--) {
            long prod = (y[i] & MASK) * (x[startX] & MASK) + carry;
            z[k] = (int)prod;
            carry = prod >>> 32;
        }
        z[startX] = (int)carry;

        for (int i = startX - 1; i >= 0; i--) {
            carry = 0;
            for (int k = startY, m = startY + i + 1; k >= 0; k-- ,m--) {
                long prod = (y[k]  & MASK) * (x[i] & MASK) + (z[m] & MASK) + carry;
                z[m] = (int)prod;
                carry = prod >>> 32;
            }
            z[i] = (int)carry;
        }
        return z;
    }

    private BigIntener square() {
        return square(false);
    }

    private BigIntener square(boolean recursive) {
        if (sign == 0) {
            return ZERO;
        }
        int len = mag.length;
        if (len < 128) {
            int[] z = squareLen(mag, len, null);
            return new BigIntener(sourceStripLeadingZeros(z), 1);
        }
        else {
            if (!recursive) {
                if (bitLength(mag, mag.length) > 16 * MAG_MAX_LEN) {
                    throw new ArithmeticException("Supported range overflow");
                }
            }
            return karatsubaSquare();
        }
    }

    private static int[] squareLen(int[] x, int len, int[] z) {
        int lenZ = len << 1;
        if (z == null || z.length < lenZ) {
            z = new int[lenZ];
        }
        squareLenChecks(x, len, z, lenZ);
        return implSquareLen(x, len, z, lenZ);
    }

    private static void squareLenChecks(int[] x, int len, int[] z, int lenZ) {
        if (len < 1) {
            throw new IllegalArgumentException("Invalid length: " + len);
        }
        if (len > x.length) {
            throw new IllegalArgumentException("Length out of bounds: " + len + " > " + x.length);
        }
        if (len * 2 > z.length) {
            throw new IllegalArgumentException("Length out of bounds: " + (len * 2) + " > " + z.length);
        }
        if (lenZ < 1) {
            throw new IllegalArgumentException("Invalid length: " + lenZ);
        }
        if (lenZ > z.length) {
            throw new IllegalArgumentException("Length out of bounds: " + lenZ + " > " + z.length);
        }
    }

    private static int[] implSquareLen(int[] x, int len, int[] z, int lenZ) {
        int last = 0;
        for (int i = 0, k = 0; k < len; k++) {
            long piece = (x[k] & MASK);
            long prod = piece * piece;
            z[i++] = (last << 31) | (int)(prod >>> 33);
            z[i++] = (int)(prod >>> 1);
            last = (int)prod;
        }

        for  (int i = len, off = 1; i > 0; i--, off += 2) {
            int tmp = x[i - 1];
            tmp = mulAdd(z, x, off, i - 1, tmp);
            addOne(z, off - 1, i, tmp);
        }
        primitiveShiftLeft(z, lenZ, 1);
        z[lenZ - 1] |= x[len - 1] & 1;
        return z;
    }

    private static int mulAdd(int[] output, int[] input, int off, int len, int t) {
        mulAddCheck(output, input, off, len);
        return implMulAdd(output, input, off, len, t);
    }

    private static void mulAddCheck(int[] output, int[] input, int off, int len) {
        if (len > input.length) {
            throw new IllegalArgumentException("Input out of bound: " + len + " > " + input.length);
        }
        if (off < 0) {
            throw new IllegalArgumentException("Invalid offset: " + off);
        }
        if (off > (output.length - 1)) {
            throw new IllegalArgumentException("Offset out of bounds: " + off + " > " + (output.length - 1));
        }
        if (len > (output.length - off)) {
            throw new IllegalArgumentException("Input out of bound: " + len + " > " + (output.length - off));
        }
    }

    private static int implMulAdd(int[] output, int[] input, int off, int len, int t) {
        long k = t & MASK;
        long carry = 0;
        off = output.length - off - 1;
        for (int i = len - 1; i >= 0; i--) {
            long prod = (input[i] & MASK) * k + (output[off] & MASK) + carry;
            output[off--] = (int)prod;
            carry = prod >>> 32;
        }
        return (int)carry;
    }

    private static int addOne(int[] arr, int off, int len, int carry) {
        off = arr.length - len - off - 1;
        long tmp = (arr[off] & MASK) + (carry & MASK);
        arr[off] = (int) tmp;
        if ((tmp >>> 32) == 0) {
            return 0;
        }

        while (--len >= 0) {
            if (--off < 0) {
                return 1;
            }
            else {
                arr[off]++;
                if (arr[off] != 0) {
                    return 0;
                }
            }
        }
        return 1;
    }

    private static void primitiveShiftLeft(int[] arr, int len, int n) {
        if (len == 0 || n == 0) {
            return;
        }
        Objects.checkFromToIndex(0, len, arr.length);
        leftShiftHelper(arr, arr, 0, n, len - 1);
        arr[len - 1] <<= n;
    }

    private BigIntener remainder(BigIntener n) {
        MutBigInteger q = new MutBigInteger(),
                a = new MutBigInteger(this.mag),
                b = new MutBigInteger(n.mag);
        return a.divide(b, q).toBigInt(this.sign);
    }

    private BigIntener karatsuba(BigIntener x, BigIntener y) {
        int lenX = x.mag.length;
        int lenY = y.mag.length;
        int half = (Math.max(lenX, lenY) + 1) / 2;
        BigIntener xLow = x.getLower(half);
        BigIntener yLow = y.getLower(half);
        BigIntener xHigh = x.getUpper(half);
        BigIntener yHigh = y.getUpper(half);
        BigIntener p1 = xHigh.multiply(yHigh);
        BigIntener p2 = xLow.multiply(yLow);
        BigIntener p3 = xHigh.add(xLow).multiply(yHigh.add(yLow));
        BigIntener res = p1.leftShift(32 * half).add(p3.subtract(p1).subtract(p2)).leftShift(32 * half).add(p2);
        if (x.sign != y.sign) {
            return res.negate();
        }
        else {
            return res;
        }
    }

    private BigIntener getLower(int n) {
        int len = mag.length;
        if (len <= n) {
            return abs();
        }
        int[] lower = new int[n];
        System.arraycopy(mag, len - n, lower, 0, n);
        return new BigIntener(sourceStripLeadingZeros(lower), 1);
    }

    private BigIntener getUpper(int n) {
        int len = mag.length;
        if (len <= n) {
            return ZERO;
        }
        int lenUp = len - n;
        int upper[] = new int [lenUp];
        System.arraycopy(mag, 0, upper, 0, lenUp);
        return new BigIntener(sourceStripLeadingZeros(upper), 1);

    }

    public BigIntener abs() {
        return (sign >= 0 ? this : this.negate());
    }

    public BigIntener negate() {
        return new BigIntener(this.mag, -this.sign);
    }

    public BigIntener leftShift(int n) {
        if (sign == 0) {
            return ZERO;
        }
        if (n > 0) {
            return new BigIntener(leftShift(mag, n), sign);
        }
        else if (n == 0) {
            return this;
        }
        else {
            return rightShiftImpl(-n);
        }
    }

    private BigIntener rightShiftImpl(int n) {
        int ints = n >>> 5;
        int bits = n & 0x1f;
        int magLen = mag.length;
        int[] magNew = null;
        if (ints >= magLen) {
            return (sign >= 0 ? ZERO : valueOf(-1));
        }
        if (bits == 0) {
            int magLenNew = magLen - ints;
            magNew = Arrays.copyOf(mag, magLenNew);
        }
        else {
            int i = 0;
            int high = mag[0] >>> bits;
            if (high == 0) {
                magNew = new int[magLen - ints - 1];
            }
            else {
                magNew = new int[magLen - ints];
                magNew[i++] = high;
            }
            int iters = magLen - ints - 1;
            Objects.checkFromToIndex(0, iters + 1, mag.length);
            Objects.checkFromToIndex(i, iters + i, magNew.length);
            rightShiftImplHelper(magNew, mag, i, bits, iters);
        }

        if (sign < 0) {
            boolean lostOnes = false;
            for (int i = magLen - 1, k = magLen - ints; i >= k && !lostOnes; i--) {
                lostOnes = (mag[i] != 0);
            }
            if (!lostOnes && bits != 0) {
                lostOnes = (mag[magLen - ints - 1] << (32 - bits) != 0);
            }

            if (lostOnes) {
                magNew = incrementJava(magNew);
            }
        }
        return new BigIntener(magNew, sign);
    }

    private static void rightShiftImplHelper(int[] arrNew, int[] arrOld, int i, int shifts, int iters) {
        int shiftCountLeft = 32 - shifts;
        int idx = iters;
        int nidx = (i == 0) ? iters - 1 : iters;
        while (nidx >= i) {
            arrNew[nidx--] = (arrOld[idx--] >>> shifts) | (arrOld[idx] << shiftCountLeft);
        }
    }

    int[] incrementJava(int[] val) {
        int last = 0;
        for (int i = val.length - 1;  i >= 0 && last == 0; i--) {
            last = (val[i] += 1);
        }
        if (last == 0) {
            val = new int[val.length + 1];
            val[0] = 1;
        }
        return val;
    }

    private BigIntener karatsubaSquare() {
        int half = (mag.length+1) / 2;
        BigIntener xLow = getLower(half);
        BigIntener xHigh = getUpper(half);
        BigIntener xHighSquare = xHigh.square();
        BigIntener xLowSquare = xLow.square();
        return xHighSquare.leftShift(half * 32).add(xLow.add(xHigh).square().subtract(xHighSquare.add(xLowSquare))).leftShift(half * 32).add(xLowSquare);
    }

    public BigIntener add(BigIntener n) {
        if (n.sign == 0) {
            return this;
        }
        if (sign == 0) {
            return n;
        }
        if (sign == n.sign) {
            return new BigIntener(add(mag, n.mag), sign);
        }
        int cmp = compareMag(n);
        if (cmp == 0) {
            return ZERO;
        }
        int[] magRes = (cmp > 0 ? subtract(mag, n.mag) : subtract(n.mag, mag));
        magRes = sourceStripLeadingZeros(magRes);
        return new BigIntener(magRes, cmp == sign ? 1 : -1);
    }

    private int compareMag(BigIntener n) {
        int[] m1 = mag;
        int[] m2 = n.mag;
        int len1 = m1.length;
        int len2 = m2.length;
        if (len1 > len2) {
            return 1;
        }
        if (len1 < len2) {
            return -1;
        }

        for (int i = 0; i < len1; i++) {
            int a = m1[i];
            int b = m2[i];
            if (a != b) {
                return ((a & MASK) < (b & MASK)) ? -1 : 1;
            }
        }
        return 0;
    }

    /********************/

    private int[] add(int[] x, int[] y) {
        if (x.length < y.length) {
            int[] tmp = x;
            x = y;
            y = tmp;
        }

        int indexX = x.length;
        int indexY = y.length;
        int res[] = new int[indexX];
        long sum = 0;
        if (indexY == 1) {
            sum = (x[--indexX] & MASK) + (y[0] & MASK) ;
            res[indexX] = (int)sum;
        } else {
            while (indexY > 0) {
                sum = (x[--indexX] & MASK) + (y[--indexY] & MASK) + (sum >>> 32);
                res[indexX] = (int)sum;
            }
        }
        boolean carry = (sum >>> 32 != 0);
        while (indexX > 0 && carry) {
            carry = ((res[--indexX] = x[indexX] + 1) == 0);
        }
        while (indexX > 0) {
            res[--indexX] = x[indexX];
        }
        if (carry) {
            int greater[] = new int[res.length + 1];
            System.arraycopy(res, 0, greater, 1, res.length);
            greater[0] = 0x01;
            return greater;
        }
        return res;
    }

    public BigIntener subtract(BigIntener n) {
        if (n.sign == 0) {
            return this;
        }
        if (sign == 0) {
            return n.negate();
        }
        if (n.sign != sign) {
            return new BigIntener(add(mag, n.mag), sign);
        }
        int cmp = compareMag(n);
        if (cmp == 0) {
            return ZERO;
        }
        int [] res = (cmp > 0 ? subtract(mag, n.mag) : subtract(n.mag, mag));
        res = sourceStripLeadingZeros(res);
        return new BigIntener(res, cmp == sign ? 1 : -1);

    }

    private int[] subtract(int[] x, int[] y) {
        int indexX = x.length;
        int indexY = y.length;
        int[] res = new int[indexX];
        long diff = 0;
        while (indexY > 0) {
            diff = (x[--indexX] & MASK) - (y[--indexY] & MASK) + (diff >> 32);
            res[indexX] = (int)diff;
        }

        boolean borrow = (diff >> 32 != 0);
        while (indexX > 0 && borrow) {
            borrow = ((res[--indexX] = x[indexX] - 1) == -1);
        }

        while (indexX > 0) {
            res[--indexX] = x[indexX];
        }
        return res;
    }

    @Override
    public int compareTo(BigIntener n) {
        if (sign == n.sign) {
            return switch (sign) {
                case 1 ->  compareMag(n);
                case -1 -> n.compareMag(this);
                default -> 0;
            };
        }
        return sign > n.sign ? 1 : -1;
    }

    @Override
    public boolean equals(Object x) {
        if (x == this) {
            return true;
        }
        if (!(x instanceof BigIntener xInt)) {
            return false;
        }
        if (xInt.sign != sign) {
            return false;
        }
        int[] m = mag;
        int len = m.length;
        int[] xm = xInt.mag;
        if (len != xm.length) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (xm[i] != m[i]) {
                return false;
            }
        }
        return true;
    }
}
