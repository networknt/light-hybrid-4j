package com.networknt.rpc.router;

import java.util.List;

public interface RpcResourcePathsProvider {
    List<String> getSafeResourcePaths();
}
