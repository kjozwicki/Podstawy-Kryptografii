package krypto;

import java.util.Arrays;

public class MutBigInteger {

    private int[] val;
    private int len;
    private int offset;

    static final long MASK = 0xffffffffL;

    MutBigInteger() {
        val = new int[1];
        len = 0;
    }

    MutBigInteger(int value) {
        val = new int[1];
        val[0] = value;
        len = 1;
    }

    MutBigInteger(int[] value) {
        val = value;
        len = val.length;
    }

    MutBigInteger(MutBigInteger value) {
        len = value.len;
        val = Arrays.copyOfRange(value.val, value.offset, value.offset + len);
    }

    void setValue(int[] value, int length) {
        val = value;
        len = length;
        offset = 0;
    }

    void clear() {
        len = offset = 0;
        for (int i = 0; i < val.length; i++) {
            val[i] = 0;
        }
    }

    final void normalize() {
        if (len == 0) {
            offset = 0;
            return;
        }

        int i = offset;
        if (val[i] != 0) {
            return;
        }

        int bounds = i + len;
        do {
            i++;
        } while(i < bounds && val[i] == 0);

        int zeros = i - offset;
        len -= zeros;
        offset = (len == 0 ? 0 : offset + zeros);
    }

    BigIntener toBigInt(int sign) {
        if (len == 0 || sign == 0) {
            return BigIntener.ZERO;
        }
        return new BigIntener(getMagArr(), sign);
    }

    private int[] getMagArr() {
        if (offset > 0 || val.length != len) {
            int[] tmp = Arrays.copyOfRange(val, offset, offset + len);
            Arrays.fill(val, 0);
            val = tmp;
            len = tmp.length;
            offset = 0;
        }
        return val;
    }

    MutBigInteger divide(MutBigInteger b, MutBigInteger q) {
        return divideKnuth(b, q, true);
    }

    MutBigInteger divideKnuth(MutBigInteger b, MutBigInteger q, boolean remainderNeeded) {
        if (b.len == 0) {
            throw new ArithmeticException("Division by zero");
        }
        if (len == 0) {
            q.len = q.offset = 0;
            return remainderNeeded ? new MutBigInteger() : null;
        }

        int cmp = compare(b);
        if (cmp < 0) {
            q.len = q.offset = 0;
            return remainderNeeded ? new MutBigInteger() : null;
        }
        if (cmp == 0) {
            q.val[0] = q.len = 1;
            q.offset = 0;
            return remainderNeeded ? new MutBigInteger() : null;
        }

        q.clear();
        if (b.len == 1) {
            int r = oneWordDivide(b.val[b.offset], q);
            if (remainderNeeded) {
                if (r == 0) {
                    return new MutBigInteger();
                }
                return new MutBigInteger(r);
            }
            else {
                return null;
            }
        }

        if (len >= 6) {
            int trailingZeros = Math.min(getLowestBits(), b.getLowestBits());
            if (trailingZeros >= 96) {
                MutBigInteger a = new MutBigInteger(this);
                b = new MutBigInteger(b);
                a.shiftRight(trailingZeros);
                b.shiftRight(trailingZeros);
                MutBigInteger r = a.divide(b, q);
                r.shiftLeft (trailingZeros);
                return r;
            }
        }

        return magnitudeDivide(b, q, remainderNeeded);
    }

    final int compare(MutBigInteger b) {
        int lenB = b.len;
        if (len < lenB) {
            return -1;
        }
        if (len > lenB) {
            return 1;
        }

        int[] valB = b.val;
        for (int i = offset, k = b.offset; i < len + offset; i++, k++) {
            int b1 = val[i] + 0x80000000;
            int b2 = valB[k] + 0x80000000;
            if (b1 < b2) {
                return -1;
            }
            if (b1 > b2) {
                return 1;
            }
        }
        return 0;
    }

    @SuppressWarnings("empty-statement")
    private int getLowestBits() {
        if (len == 0) {
            return -1;
        }
        int b, i;
        for (i = len - 1; (i > 0) && (val[offset + i] == 0); i--);
        b = val[offset+i];
        if (b == 0) {
            return -1;
        }
        return ((len - i - 1) << 5) + Integer.numberOfTrailingZeros(b);
    }

    void shiftRight(int n) {
        if (len == 0) {
            return;
        }

        int ints = n >>> 5;
        int bits = n & 0x1f;
        this.len -= ints;
        if (bits == 0) {
            return;
        }

        int highWordBits = BigIntener.intBitLength(val[offset]);
        if (bits >= highWordBits) {
            this.leftShiftPrimitive(32 - bits);
            this.len--;
        }
        else {
            rightShiftPrimitive(bits);
        }
    }

    void shiftLeft(int n) {
        if (len == 0) {
            return;
        }

        int ints = n >>> 5;
        int bits = n & 0x1f;
        int highWordBits = BigIntener.intBitLength(val[offset]);

        if (n <= (32 - highWordBits)) {
            leftShiftPrimitive(bits);
            return;
        }

        int lenNew = len + ints + 1;
        if (bits <= (32 - highWordBits)) {
            lenNew--;
        }
        if (val.length < lenNew) {
            int[] res = new int [lenNew];
            for (int i = 0; i < lenNew; i++) {
                res[i] = val[offset + i];
            }
            setValue(res, lenNew);
        }
        else if (val.length - offset >= lenNew) {
            for (int i = 0; i < lenNew - len; i++) {
                val[offset + len + i] = 0;
            }
        }
        else {
            for (int i = 0; i < len; i++) {
                val[i] = val[offset + i];
            }
            for (int i = len; i < lenNew; i++) {
                val[i] = 0;
            }
            offset = 0;
        }
        len = lenNew;
        if (bits == 0) {
            return;
        }
        if (bits <= (32 - highWordBits)) {
            leftShiftPrimitive(bits);
        }
        else {
            rightShiftPrimitive(32 - bits);
        }
    }

    private void leftShiftPrimitive(int n) {
        int[] value = val;
        int n2 = 32 - n, b;
        for (int i = offset, c = value[i], m = i + len - 1; i < m; i++) {
            b = c;
            c = value[i + 1];
            val[i] = (b << n) | (c >>> n2);
        }
        val [offset + len + 1] <<= n;
    }

    private void rightShiftPrimitive(int n) {
        int[] value = val;
        int n2 = 32 - n, b;
        for (int i = offset + len - 1, c = value[i]; i > offset; i--) {
            b = c;
            c = value[i + 1];
            val[i] = (c << n) | (b >>> n2);
        }
        value[offset] >>>= n;
    }

    int oneWordDivide(int d, MutBigInteger q) {
        long dLong = d & MASK;
        if (len == 1) {
            long dividend = val[offset] & MASK;
            int quotient = (int) (dividend / dLong);
            int r = (int) (dividend - quotient * dLong);
            q.val[0] = quotient;
            q.len = (quotient == 0) ? 0 : 1;
            q.offset = 0;
            return r;
        }

        if (q.val.length < len) {
            q.val = new int[len];
        }
        q.offset = 0;
        q.len = len;

        int shift = Integer.numberOfLeadingZeros(d);
        int rem = val[offset];
        long remLong = rem & MASK;
        if (remLong < dLong) {
            q.val[0] = 0;
        }
        else {
            q.val[0] = (int)(remLong / dLong);
            rem = (int)(remLong - (q.val[0] * dLong));
            remLong = rem & MASK;
        }
        int lenX = len;
        while (--lenX > 0) {
            long estimatedDiv = (remLong << 32) | (val[offset + len - lenX] & MASK);
            int quot;
            if (estimatedDiv >= 0) {
                quot = (int)(estimatedDiv / dLong);
                rem = (int)(estimatedDiv - quot * dLong);
            }
            else {
                long tmp = wordDiv(estimatedDiv, d);
                quot = (int)(tmp & MASK);
                rem = (int)(tmp >>> 32);
            }
            q.val[len - lenX] = quot;
            remLong = rem & MASK;
        }
        q.normalize();
        if (shift > 0) {
            return rem % d;
        }
        else {
            return rem;
        }
    }

    static long wordDiv(long n, int d) {
        long dLong = d & MASK;
        long r, q;
        if (dLong == 1) {
            q = (int) n;
            r = 0;
            return (r << 32) | (q & MASK);
        }

        q = (n >>> 1) / (dLong >>> 1);
        r = n - q * dLong;
        while (r < 0) {
            r += dLong;
            q--;
        }
        while (r >= dLong) {
            r -= dLong;
            q++;
        }
        return (r << 32) | (q & MASK);
    }


    private MutBigInteger magnitudeDivide(MutBigInteger d, MutBigInteger q, boolean remainderNeeded) {
        int shift = Integer.numberOfLeadingZeros(d.val[d.offset]);
        int[] div;
        final int dlen = d.len;
        MutBigInteger rem;
        if (shift > 0) {
            div = new int[dlen];
            shiftCopy(d.val, d.offset, dlen, div, 0, shift);
            if (Integer.numberOfLeadingZeros(val[offset]) >= shift) {
                int[] arrrem = new int[len + 1];
                rem = new MutBigInteger(arrrem);
                rem.len = len;
                rem.offset = 1;
                shiftCopy(val, offset, len, arrrem, 1, shift);
            }
            else {
                int[] arrrem = new int[len + 2];
                rem = new MutBigInteger(arrrem);
                rem.len = len + 1;
                rem.offset = 1;
                int from = offset;
                int b, c = 0;
                int n2 = 32 - shift;
                for (int i = 1; i < len + 1; i++, from++) {
                    b = c;
                    c = val[from];
                    arrrem[i] = (b << shift) | (c >>> n2);
                }
                arrrem[len + 1] = c << shift;
            }
        }
        else {
            div = Arrays.copyOfRange(d.val, d.offset, d.offset + d.len);
            rem = new MutBigInteger(new int[len + 1]);
            System.arraycopy(val, offset, rem.val, 1, len);
            rem.len = len;
            rem.offset = 1;
        }

        int nlen = rem.len;
        final int lim = nlen - dlen + 1;
        if (q.val.length < lim) {
            q.val = new int[lim];
            q.offset = 0;
        }
        q.len = lim;
        int[] quotient = q.val;
        if (rem.len == nlen) {
            rem.val[0] = 0;
            rem.offset = 0;
            rem.len++;
        }

        int dh = div[0];
        int dl = div[1];
        long dhLong = dh & MASK;

        for (int i = 0; i < lim - 1; i++) {
            int qHat = 0;
            int qRem = 0;
            boolean skipCorrect = false;
            int nh = rem.val[i + rem.offset];
            int nh2 = nh  + 0x80000000;
            int nm = rem.val[rem.offset + i + 1];
            if (nh == dh) {
                qHat = ~0;
                qRem = nh + nm;
                skipCorrect = qRem + 0x80000000 < nh2;
            }
            else {
                long chunk = (((long)nh) << 32) | (nm & MASK);
                if (chunk >= 0) {
                    qHat = (int)(chunk / dhLong);
                    qRem = (int)(chunk - (qHat * dhLong));
                }
                else {
                    long tmp = wordDiv(chunk, dh);
                    qHat = (int)(tmp & MASK);
                    qRem = (int)(tmp >>> 32);
                }
            }

            if (qHat == 0) {
                continue;
            }

            if (!skipCorrect) {
                long nl = rem.val[rem.offset + i + 2] & MASK;
                long rs = ((qRem & MASK) << 32 ) | nl;
                long estimatedProd = (dl & MASK) * (qHat & MASK);
                if (ulongCompare(estimatedProd, rs)) {
                    qHat--;
                    qRem = (int)((qRem & MASK) + dhLong);
                    if ((qRem & MASK) >= dhLong) {
                        estimatedProd -= (dl & MASK);
                        rs = ((qRem & MASK) << 32) | nl;
                        if (ulongCompare(estimatedProd, rs)) {
                            qHat--;
                        }
                    }
                }
            }
            rem.val[rem.offset + i] = 0;
            int borrow = mulSub(rem.val, div, qHat, dlen, rem.offset + i);
            if (borrow + 0x80000000 > nh2) {
                divAdd(div, rem.val, rem.offset + i + 1);
                qHat--;
            }
            quotient[i] = qHat;
        }

        int qHat = 0;
        int qRem = 0;
        boolean skipCorrect = false;
        int nh = rem.val[rem.offset + lim - 1];
        int nh2 = nh + 0x80000000;
        int nm = rem.val[rem.offset + lim];
        if (nh == dh) {
            qHat = ~0;
            qRem = nh + nm;
            skipCorrect = qRem + 0x80000000 < nh2;
        }
        else {
            long chunk = (((long) nh) << 32) | (nm & MASK);
            if (chunk >= 0) {
                qHat = (int) (chunk / dhLong);
                qRem = (int) (chunk - (qHat * dhLong));
            } else {
                long tmp = wordDiv(chunk, dh);
                qHat = (int) (tmp & MASK);
                qRem = (int) (tmp >>> 32);
            }
        }
        if (qHat != 0) {
            if (skipCorrect) {
                long nl = rem.val[rem.offset + lim - 1] & MASK;
                long rs = ((qRem & MASK) << 32) | nl;
                long estimatedProd = (dl & MASK) * (qHat & MASK);
                if (ulongCompare(estimatedProd, rs)) {
                    qHat--;
                    qRem = (int)((qRem & MASK) + dhLong);
                    if ((qRem & MASK) >= dhLong) {
                        estimatedProd -= (dl & MASK);
                        rs = ((qRem & MASK) << 32) | nl;
                        if (ulongCompare(estimatedProd, rs)) {
                            qHat--;
                        }
                    }
                }
            }
            int borrow;
            rem.val[rem.offset + lim - 1] = 0;
            if (remainderNeeded) {
                borrow = mulSub(rem.val, div, qHat, dlen, rem.offset + lim - 1);
            }
            else {
                borrow = mulSubBorrow(rem.val, div, qHat, dlen, rem.offset + lim - 1);
            }

            if (borrow + 0x80000000 > nh2) {
                if (remainderNeeded) {
                    divAdd(div, rem.val, rem.offset + lim);
                }
                qHat--;
            }
            quotient[(lim - 1)] = qHat;
        }

        if (remainderNeeded) {
            if (shift > 0)
                rem.shiftRight(shift);
            rem.normalize();
        }
        q.normalize();
        return remainderNeeded ? rem : null;
    }

    private static void shiftCopy(int[] src, int from, int length, int[] target, int targetFrom, int shift) {
        int n2 = 32 - shift;
        int c = src[from];
        for (int i = 0; i < length - 1; i++) {
            int b = c;
            c = src[++from];
            target[targetFrom + i] = (b << shift) | (c >>> n2);
        }
        target[targetFrom + length - 1] = c << shift;
    }

    private boolean ulongCompare(long a, long b) {
        return (a + Long.MIN_VALUE) > (b + Long.MIN_VALUE);
    }

    private int mulSub(int[] q, int[] a, int x, int length, int offset) {
        long xLong = x & MASK;
        long carry = 0;
        offset += length;
        for (int i = length - 1; i >= 0; i--) {
            long prod = (a[i] & MASK) * xLong + carry;
            long diff = q[offset] - prod;
            q[offset--] = (int)diff;
            carry = (prod >>> 32) + (((diff & MASK) > (((~(int)prod) & MASK))) ? 1:0);
        }
        return (int)carry;
    }

    private int mulSubBorrow(int[] q, int[] a, int x, int len, int offset) {
        long xLong = x & MASK;
        long carry = 0;
        offset += len;
        for (int i= len - 1; i >= 0; i--) {
            long prod = (a[i] & MASK) * xLong + carry;
            long diff = q[offset--] - prod;
            carry = (prod >>> 32) + (((diff & MASK) > (((~(int)prod) & MASK))) ? 1:0);
        }
        return (int)carry;
    }

    private int divAdd(int[] a, int[] res, int offset) {
        long carry = 0;
        for (int i = a.length - 1; i >= 0; i--) {
            long sum = (a[i] & MASK) + (res[i + offset] & MASK) + carry;
            res[i+offset] = (int)sum;
            carry = sum >>> 32;
        }
        return (int)carry;
    }

    void multiply(MutBigInteger y, MutBigInteger z) {
        int lenX = len;
        int lenY = y.len;
        int lenNew = lenX + lenY;
        if (z.val.length < lenNew) {
            z.val = new int[lenNew];
        }
        z.len = lenNew;
        z.offset = 0;
        long carry = 0;
        for (int i = lenY - 1, k = lenX + lenY; i >= 0; i--, k--) {
            long prod = (y.val[i + y.offset] & MASK) * (val[offset + lenX - 1] & MASK) + carry;
            z.val[k] = (int)prod;
            carry = prod >>> 32;
        }
        z.val[lenX - 1] = (int)carry;

        for (int i = lenX - 2; i >= 0; i--) {
            carry = 0;
            for (int j = lenY - 1, k = lenY + i; j >= 0; j--, k--) {
                long prod = (y.val[y.offset + j] & MASK) * (val[offset + i] & MASK) + (z.val[k] & MASK) + carry;
                z.val[k] = (int)prod;
                carry = prod >>> 32;
            }
            z.val[i] = (int)carry;
        }
        z.normalize();
    }

    void add(MutBigInteger a) {
        int x = len;
        int y = a.len;
        int resLen = (len > a.len ? len : a.len);
        int[] res = (val.length < resLen ? new int[resLen] : val);
        int rstart = res.length - 1;
        long sum;
        long carry = 0;
        while(x > 0 && y > 0) {
            x--; y--;
            sum = (val[x+offset] & MASK) + (a.val[a.offset + y] & MASK) + carry;
            res[rstart--] = (int)sum;
            carry = sum >>> 32;
        }
        while(x > 0) {
            x--;
            if (carry == 0 && res == val && rstart == (x + offset)) {
                return;
            }
            sum = (val[offset + x] & MASK) + carry;
            res[rstart--] = (int)sum;
            carry = sum >>> 32;
        }
        while(y > 0) {
            y--;
            sum = (a.val[a.offset + y] & MASK) + carry;
            res[rstart--] = (int)sum;
            carry = sum >>> 32;
        }

        if (carry > 0) {
            resLen++;
            if (res.length < resLen) {
                int tmp[] = new int[resLen];
                System.arraycopy(res, 0, tmp, 1, res.length);
                tmp[0] = 1;
                res = tmp;
            }
            else {
                res[rstart--] = 1;
            }
        }
        val = res;
        len = resLen;
        offset = res.length - resLen;
    }

    boolean isOdd() {
        return (len == 0) ? false : ((val[offset + len - 1] & 1) == 1);
    }

    boolean isEven() {
        return (len == 0) || ((val[offset + len - 1] & 1) == 0);
    }

    boolean isOne() {
        return (len == 1) && (val[offset] == 1);
    }

    private MutBigInteger modInverse(MutBigInteger mod) {
        MutBigInteger p = new MutBigInteger(mod);
        MutBigInteger g = new MutBigInteger(p);
        MutBigInteger f = new MutBigInteger(this);
        SignedMutBigIng c = new SignedMutBigIng(1);
        SignedMutBigIng d = new SignedMutBigIng();
        MutBigInteger tmp = null;
        SignedMutBigIng sTmp = null;
        int k = 0;
        if (f.isEven()) {
            int zeros = f.getLowestBits();
            f.shiftRight(zeros);
            d.shiftLeft(zeros);
            k = zeros;
        }
        while (!f.isOne()) {
            if (f.len == 0)
                throw new ArithmeticException("MutBigInt cannot be inverted.");
            if (f.compare(g) < 0) {
                tmp = f; f = g;
                g = tmp;
                sTmp = d; d = c;
                c = sTmp;
            }
            if (((f.val[f.offset + f.len - 1] ^ g.val[g.offset + g.len - 1]) & 3) == 0) {
                f.subtract(g);
                c.subtractSigned(d);
            }
            else {
                f.add(g);
                c.addSigned(d);
            }

            // Right shift f k times until odd, left shift d k times
            int trailingZeros = f.getLowestBits();
            f.shiftRight(trailingZeros);
            d.shiftLeft(trailingZeros);
            k += trailingZeros;
        }
        if (c.compare(p) >= 0) { // c has a larger magnitude than p
            MutBigInteger remainder = c.divide(p, new MutBigInteger());
            c.valueCopy(remainder);
        }
        if (c.sign < 0) {
            c.addSigned(p);
        }

        return fixUp(c, p, k);
    }

    int subtract(MutBigInteger b) {
        MutBigInteger a = this;
        int[] res = val;
        int sign = a.compare(b);
        if (sign == 0) {
            offset = len = 0;
            return 0;
        }
        if (sign < 0) {
            MutBigInteger tmp = a;
            a = b;
            b = tmp;
        }
        int resLen = a.len;
        if (res.length < resLen) {
            res = new int[resLen];
        }
        long diff = 0;
        int x = a.len;
        int y = b.len;
        int rstart = res.length - 1;
        while (y > 0) {
            x--;
            y--;
            diff = (a.val[a.offset + x] & MASK) -(b.val[b.offset + y] & MASK) - ((int)-(diff >> 32));
            res[rstart--] = (int)diff;
        }
        while (x > 0) {
            x--;
            diff = (a.val[a.offset + x] & MASK) - ((int)-(diff >> 32));
            res[rstart--] = (int)diff;
        }
        val = res;
        len = resLen;
        offset = val.length - resLen;
        normalize();
        return sign;
    }

    void valueCopy(MutBigInteger src) {
        int length = src.len;
        if (val.length < length) {
            val = new int[len];
        }
        System.arraycopy(src.val, src.offset, val, 0, length);
        len = length;
        offset = 0;
    }

    static MutBigInteger fixUp(MutBigInteger c, MutBigInteger p, int k) {
        MutBigInteger tmp = new MutBigInteger();
        int r = -inverseMod32(p.val[p.offset + p.len -1]);
        for (int i=0, words = k >> 5; i < words; i++) {
            int  v = r * c.val[c.offset + c.len - 1];
            p.mul(v, tmp);
            c.add(tmp);
            c.len--;
        }
        int bits = k & 0x1f;
        if (bits != 0) {
            int v = r * c.val[c.offset + c.len-1];
            v &= ((1<<bits) - 1);
            p.mul(v, tmp);
            c.add(tmp);
            c.shiftRight(bits);
        }
        if (c.compare(p) >= 0)
            c = c.divide(p, new MutBigInteger());
        return c;
    }

    void mul(int y, MutBigInteger z) {
        if (y == 1) {
            z.valueCopy(this);
            return;
        }
        if (y == 0) {
            z.clear();
            return;
        }
        long longY = y & MASK;
        int[] valZ = (z.val.length < len +1 ? new int[len + 1] : z.val);
        long carry = 0;
        for (int i = len - 1; i >= 0; i--) {
            long product = longY * (val[i+offset] & MASK) + carry;
            valZ[i + 1] = (int)product;
            carry = product >>> 32;
        }
        if (carry == 0) {
            z.len = len;
            z.offset = 1;
        }
        else {
            valZ[0] = (int)carry;
            z.len = len + 1;
            z.offset = 0;
        }
        z.val = valZ;
    }

    static int inverseMod32(int val) {
        int t = val;
        for (int i = 0; i < 4; i ++) {
            t = t * (2 - val * t);
        }
        return t;
    }

    static long inverseMod64(long val) {
        long t = val;
        for (int i = 0; i < 5; i ++) {
            t = t * (2 - val * t);
        }
        assert(t * val == 1);
        return t;
    }

    MutBigInteger modInverseMP2(int k) {
        if (isEven()) {
            throw new ArithmeticException("Cannot invert.");
        }
        if (k > 64) {
            return euclidModInverse(k);
        }
        int t = inverseMod32(val[len + offset - 1]);
        if (k < 33) {
            t = (k == 32 ? t : t & ((1 << k) - 1));
            return new MutBigInteger(t);
        }
        long pLong = (val[len + offset - 1] & MASK);
        if (len > 1) {
            pLong |=  ((long)val[len + offset-2] << 32);
        }
        long tLong = t & MASK;
        tLong = tLong * (2 - pLong * tLong);
        tLong = (k == 64 ? tLong : tLong & ((1L << k) - 1));
        MutBigInteger res = new MutBigInteger(new int[2]);
        res.val[0] = (int)(tLong >>> 32);
        res.val[1] = (int)tLong;
        res.len = 2;
        res.normalize();
        return res;
    }

    MutBigInteger euclidModInverse(int k) {
        MutBigInteger b = new MutBigInteger(1);
        b.shiftLeft(k);
        MutBigInteger mod = new MutBigInteger(b);
        MutBigInteger a = new MutBigInteger(this);
        MutBigInteger q = new MutBigInteger();
        MutBigInteger r = b.divide(a, q);
        MutBigInteger tmp = b;
        b = r;
        r = tmp;
        MutBigInteger t1 = new MutBigInteger(q);
        MutBigInteger t0 = new MutBigInteger(1);
        MutBigInteger temp = new MutBigInteger();
        while (!b.isOne()) {
            r = a.divide(b, q);
            if (r.len == 0) {
                throw new ArithmeticException("BigInteger not invertible.");
            }
            tmp = r;
            a = tmp;
            if (q.len == 1) {
                t1.mul(q.val[q.offset], temp);
            }
            else {
                q.multiply(t1, temp);
            }
            tmp = q;
            q = temp;
            temp = tmp;
            t0.add(q);
            if (a.isOne()) {
                return t0;
            }
            r = b.divide(a, q);
            if (r.len == 0) {
                throw new ArithmeticException("Cannot invert.");
            }
            tmp = b;
            b =  r;
            if (q.len == 1) {
                t0.mul(q.val[q.offset], temp);
            }
            else {
                q.multiply(t0, temp);
            }
            tmp = q; q = temp; temp = tmp;
            t1.add(q);
        }
        mod.subtract(t1);
        return mod;
    }

    static MutBigInteger modInverseBP2(MutBigInteger mod, int k) {
        return fixUp(new MutBigInteger(1), new MutBigInteger(mod), k);
    }

    public int[] toIntArray() {
        int[] res = new int[len];
        for(int i=0; i < len; i++)
            res[i] = val[offset+i];
        return res;
    }

}
