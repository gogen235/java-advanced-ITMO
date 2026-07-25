package info.kgeorgiy.ja.goge.i18n;

public class TextStatisticsException extends Exception {
    public TextStatisticsException(String massage) {
        super(massage);
    }
    public TextStatisticsException(String massage, Exception e) {
        super(massage, e);
    }
}
