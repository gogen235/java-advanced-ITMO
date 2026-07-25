package info.kgeorgiy.ja.goge.walk;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

public class JenkinsHash extends MessageDigest implements Cloneable {
    private int hash = 0;

    protected JenkinsHash() {
        super("JENKINS");
    }

    @Override
    protected void engineUpdate(byte input) {
        hash += input & 0xff;
        hash += hash << 10;
        hash ^= hash >>> 6;
    }

    @Override
    protected void engineUpdate(byte[] input, int offset, int len) {
        for (int i = offset; i < offset + len; i++) {
            engineUpdate(input[i]);
        }
    }

    @Override
    protected byte[] engineDigest() {
        hash += hash << 3;
        hash ^= hash >>> 11;
        hash += hash << 15;
        return ByteBuffer.allocate(4).putInt(hash).array();
    }

    @Override
    protected void engineReset() {
        hash = 0;
    }


    // :NOTE: getDigestLength??
    @Override
    public JenkinsHash clone() throws CloneNotSupportedException {
        return (JenkinsHash) super.clone();
    }
}
