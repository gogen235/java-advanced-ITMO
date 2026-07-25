package info.kgeorgiy.ja.goge.walk;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

public class HashMaker {
    private final static int BUFFER_SIZE = 1024;
    // :NOTE: global
    private final static byte[] BUFFER = new byte[BUFFER_SIZE];
    private final String hashFormat;
    private final MessageDigest hashCounter;

    public HashMaker(final String algorithm) throws NoSuchAlgorithmException {
        if (!Objects.equals(algorithm, "jenkins")) {
            hashCounter = MessageDigest.getInstance(algorithm.toUpperCase());
            // :NOTE: hashCounter.getDigestLength(); -> hashFormat
        } else {
            hashCounter = new JenkinsHash();
        }
        this.hashFormat = "%0" + hashCounter.getDigestLength() * 2 + "x";
    }

    public String zeroHash() {
        return String.format(hashFormat, 0);
    }

    public String countHash(final Path path) {
        try (final InputStream reader = Files.newInputStream(path)) {
            hashCounter.reset();
            // :NOTE: ??
            Arrays.fill(BUFFER, (byte) 0);
            int count;
            while ((count = reader.read(BUFFER)) >= 0) {
                hashCounter.update(BUFFER, 0, count);
            }
            // :NOTE: HexFormat
            return HexFormat.of().formatHex(hashCounter.digest());
        } catch (final SecurityException | IOException e) {
            return zeroHash();
        }
    }
}
