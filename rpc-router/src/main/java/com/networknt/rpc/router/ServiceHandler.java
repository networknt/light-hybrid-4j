package com.networknt.rpc.router;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by steve on 19/02/17.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceHandler {
    String id() default "";
}
