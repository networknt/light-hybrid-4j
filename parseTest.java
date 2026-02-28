import java.util.*;

public class parseTest {
    public static void main(String[] args) {
        String method = "lightapi.net/service/createApiVersion/0.1.0";
        String[] parts = method.split("/");
        System.out.println(Arrays.toString(parts));
    }
}
