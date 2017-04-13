package com.networknt.rpc.router;

import com.networknt.colfer.ColferRpc;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by steve on 19/02/17.
 */
public class ColferHandlerTest {
    @Test
    public void testColferServiceId() {
        ColferRpc colferRpc = new ColferRpc();
        colferRpc.host = "www.networknt.com";
        colferRpc.service = "account";
        colferRpc.action = "credit";
        colferRpc.version = "0.1.1";

        ColferHandler handler = new ColferHandler();
        String serviceId = handler.getServiceId(colferRpc);
        Assert.assertEquals("www.networknt.com/account/credit/0.1.1", serviceId);
    }

}
