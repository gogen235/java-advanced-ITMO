package info.kgeorgiy.ja.goge.walk;

public class Walk {
    public static void main(String[] args) {
        // :NOTE: copy-paste
        try {
            new BaseWalk(args).walk(0);
        } catch (WalkException e) {
            System.err.println(e.getMessage());
        }
    }
}
