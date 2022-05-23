package krypto;

class SignedMutBigIng extends MutBigInteger{
    int sign = 1;

    SignedMutBigIng() {
        super();
    }

    SignedMutBigIng(int value) {
        super(value);
    }

    void subtractSigned(SignedMutBigIng a) {
        if (sign == a.sign) {
            sign = sign * subtract(a);
        }
        else {
            add(a);
        }
    }

    void addSigned(SignedMutBigIng a) {
        if (sign == a.sign) {
            add(a);
        }
        else {
            sign = sign * subtract(a);
        }
    }

    void addSigned(MutBigInteger a) {
        if (sign == 1) {
            add(a);
        }
        else {
            sign = sign * subtract(a);
        }
    }

    @Override
    public String toString() {
        return this.toBigInt(sign).toString();
    }
}
