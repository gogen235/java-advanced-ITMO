package info.kgeorgiy.ja.goge.i18n;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.BreakIterator;
import java.text.Collator;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.nio.file.Files.newBufferedWriter;

public class TextStatistics {

    private static final String BUNDLE_NAME = "info.kgeorgiy.ja.goge.i18n.StatisticsBundle";
    public static void main(String[] args) throws TextStatisticsException {
        if (args == null || args.length < 4) {
            throw new IllegalArgumentException("Four arguments expected");
        }
        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Argument can't be null");
        }
        Locale inputLocale = Locale.of("en", "us");
        Locale outputLocale = Locale.of("ru", "ru");
        Path inputFile = Path.of(args[2]);
        Path outputFile = Path.of(args[3]);

        new StatisticsCalculator(inputLocale, outputLocale, inputFile, outputFile).getStatistics();
    }

    private static class StatisticsCalculator {
        private final Locale inputLocale;

        private final Locale outputLocale;

        private final Path inputFile;

        private final Path outputFile;

        private final Collator collator;

        private StringBuilder statisticsSentence;

        private StringBuilder statisticsWords;

        private static final String SEP = System.lineSeparator();

        private static final String TAB = " ".repeat(4);

        ResourceBundle bundle;

        public StatisticsCalculator(Locale inputLocale, Locale outputLocale, Path inputFile, Path outputFile) {
            this.inputLocale = inputLocale;
            this.outputLocale = outputLocale;
            this.inputFile = inputFile;
            this.outputFile = outputFile;
            this.collator = Collator.getInstance(inputLocale);
            collator.setStrength(Collator.IDENTICAL);
            this.bundle = ResourceBundle.getBundle(BUNDLE_NAME, outputLocale);
        }

        private void getStatistics() {
            String text;
            try {
                text = Files.readString(inputFile);
            } catch (IOException e) {
                System.err.println("Exception while reading input file: " + e);
                return;
            }

            BreakIterator itSentence = BreakIterator.getSentenceInstance(inputLocale);
            this.statisticsSentence = count(itSentence, text, "sentences", "sentence", "sentence_genitive", (x) -> true);

            BreakIterator itWords = BreakIterator.getWordInstance(inputLocale);
            this.statisticsWords = count(itWords, text, "words", "word", "word_genitive", this::checkWord);
            writeStatistics();
        }

        private boolean checkWord(String sentence) {
            return sentence.isEmpty() || !sentence.codePoints().allMatch(Character::isAlphabetic);
        }

        private StringBuilder count(BreakIterator it, String text, String items, String item, String itemGenitive, Predicate<String> check) {
            it.setText(text);
            int start = it.first();
            String maxItem = "";
            String maxLengthItem = "";
            String minItem = "";
            String minLengthItem = "";
            int countItems = 0;
            int lengthItems = 0;
            Set<String> setItems = new HashSet<>();
            for (int end = it.next(); end != BreakIterator.DONE; start = end, end = it.next()) {
                String sentence = text.substring(start,end).trim();
                if (check.test(sentence)) {
                    continue;
                }
                countItems++;
                lengthItems += sentence.length();
                setItems.add(sentence);
                if (collator.compare(maxItem, sentence) < 0) {
                    maxItem = sentence;
                }
                if (sentence.length() > maxLengthItem.length()) {
                    maxLengthItem = sentence;
                }
                if (collator.compare(sentence, minItem) < 0 || minItem.isEmpty()) {
                    minItem = sentence;
                }
                if (sentence.length() < minLengthItem.length() || minLengthItem.isEmpty()) {
                    minLengthItem = sentence;
                }
            }
            StringBuilder stat = new StringBuilder();
            stat.append(String.format("%s %s: %s (%s %s)." + SEP,
                    getBundle("number_statistics"),
                    getBundle(items),
                    countItems,
                    setItems.size(),
                    setItems.size() == 1 ? getBundle("different_neuter_gender") : getBundle("different_many")));
            stat.append(String.format(TAB + "%s %s: \"%s\"." + SEP,
                    getBundle("min_neuter_gender"),
                    getBundle(item),
                    minItem));
            stat.append(String.format(TAB + "%s %s: \"%s\"." + SEP,
                    getBundle("max_neuter_gender"),
                    getBundle(item),
                    maxItem));
            stat.append(String.format(TAB + "%s %s %s: %s (\"%s\")." + SEP,
                    getBundle("min_feminine"),
                    getBundle("length"),
                    getBundle(itemGenitive),
                    minLengthItem.length(),
                    minLengthItem));
            stat.append(String.format(TAB + "%s %s %s: %s (\"%s\")." + SEP,
                    getBundle("max_feminine"),
                    getBundle("length"),
                    getBundle(itemGenitive),
                    maxLengthItem.length(),
                    maxLengthItem));
            return stat;
        }

        private String getBundle(String pattern) {
            return bundle.getString(pattern);
        }

        private void writeStatistics() {
            StringBuilder output = new StringBuilder();
            output.append(String.format("%s: \"%s\"." + SEP, bundle.getString("analyzed_file"), inputFile));
            output.append(bundle.getString("summary_statistics")).append(SEP);
            for (String item : List.of("sentences", "words", "numbers", "sums", "dates")) {
                output.append(String.format(TAB + "%s %s: %s." + SEP, bundle.getString("number_statistics"), bundle.getString(item), "0"));
            }
            output.append(statisticsSentence);
            output.append(statisticsWords);
            try (BufferedWriter writer = newBufferedWriter(outputFile)) {
                writer.write(output.toString());
            } catch (IOException ignore) {
            }
        }
    }
}
