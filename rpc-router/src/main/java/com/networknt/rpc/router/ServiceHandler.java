package com.networknt.rpc.router;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is used to mark a class as a service handler. The id is used to identify the handler
 *
 * @author Steve Hu
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceHandler {
    String id() default "";
}
