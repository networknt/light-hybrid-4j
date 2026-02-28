import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RpcTest {
    public static void main(String[] args) {
        String resultStr = "{\"host\":\"lightapi.net\"}";
        Object reqId = 123;
        
        String jsonRpcResponse = String.format("{\"jsonrpc\":\"2.0\",\"result\":%s,\"id\":%s}", resultStr, reqId);
        System.out.println(jsonRpcResponse);
    }
}
