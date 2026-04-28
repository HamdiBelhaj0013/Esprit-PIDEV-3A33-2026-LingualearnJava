import org.mindrot.jbcrypt.BCrypt;

public class TestHash {
    public static void main(String[] args) {
        String hash = BCrypt.hashpw("password", BCrypt.gensalt(12));
        System.out.println(hash);
    }
}