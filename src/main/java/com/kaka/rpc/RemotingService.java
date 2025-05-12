package com.kaka.rpc;

import java.lang.annotation.*;

/**
 * RPC接口服务标注，此注解仅能标注 {@link com.kaka.notice.Proxy} 及其子类 <br>
 * 此注解生效则必须在{@link com.kaka.Startup}子类中调用
 * <pre>
 *     {@code addDetector(new RemotingServiceDetector())}
 * </pre>
 * <p>
 * 否则请参看{@link RemotingServiceDetector}的{@code discern}方法进行手动注册
 *
 * @author zkpursuit
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RemotingService {
    Class<?> value();

    String context() default "";
}
