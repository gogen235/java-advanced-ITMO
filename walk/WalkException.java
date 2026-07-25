package info.kgeorgiy.ja.goge.walk;

public class WalkException extends Exception {

    public WalkException(String massage) {
        super(massage);
    }
    public WalkException(String massage, Exception e) {
        super(massage, e);
    }
}
