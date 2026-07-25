package info.kgeorgiy.ja.goge.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public abstract class AbstractHelloUDPClient implements HelloClient {

    protected static final int TIMEOUT = 100;

    protected static void checkMain(final Supplier<AbstractHelloUDPClient> client, final String[] args) {
        if (args == null || args.length != 5) {
            throw new IllegalArgumentException("5 arguments expected");
        }
        // :NOTE: stream + anymatch
        for (final String arg : args) {
            if (arg == null) {
                throw new IllegalArgumentException("Argument can't be null");
            }
        }
        try {
            client.get().run(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Illegal arguments" + e.getMessage());
        }
    }

    protected String getMessage(final String prefix, final int threadNum, final int requestNum) {
        return prefix + threadNum + "_" + requestNum;
    }

    protected boolean checkResponse(final String response, final int threadNum, final int requestNum) {
        return checkNumbers(response, threadNum, requestNum) && checkHello(response);
    }

    private boolean checkHello(final String response) {
        final String firstWord = response.split(", ", 2)[0];
        return IntStream.range(0, firstWord.length()).allMatch((j) -> Character.isLetter(firstWord.charAt(j)));
    }

    private boolean checkNumber(final Matcher matcher, final int num) {
        return matcher.find() && Long.parseLong(matcher.group()) == num;
    }

    private boolean checkNumbers(final String response, final int threadNum, final int responseNum) {
        // :NOTE: pattern to constant
        final Matcher matcher = Pattern.compile("\\p{IsDigit}+").matcher(response);
        return checkNumber(matcher, threadNum) && checkNumber(matcher, responseNum);
    }

}
