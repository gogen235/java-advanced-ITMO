package info.kgeorgiy.ja.goge.walk;

public class RecursiveWalk {
    public static void main(String[] args) {
        try {
            new BaseWalk(args).walk(Integer.MAX_VALUE);
        } catch (WalkException e) {
            System.err.println(e.getMessage());
        }
    }
}
