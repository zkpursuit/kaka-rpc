package com.kaka.rpc.core;

import io.netty.buffer.ByteBuf;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * 客户端接口
 *
 * @author zhoukai
 */
public interface IClient {
    /**
     * 发送自定义数据包
     *
     * @param data 数据包，结构为：int协议号+协议内容
     * @return true表示发送成功
     */
    boolean sendAndFlush(ByteBuf data);

    /**
     * 执行远程服务器领域事件，亦可为rpc调用 <br>
     * 使用方式可参看 {@code execRemotingLogic} 异步回调
     *
     * @param cmd            前后端通信协议号，亦表示领域事件名 <br>
     *                       <p>
     *                       当为 rpc:接口完全限定名:接口方法名 时，表示为rpc调用
     *                       </p>
     * @param timeoutSeconds 超时秒数，当此值小于或等于0时，程序不会设定超时控制
     * @param params         当为rpc调用时则为方法参数，其它情况为普通领域事件处理器参数
     * @return 异步结果，当 timeoutSeconds 小于或等于0时，须调用 {@code get(timeout, timeUnit)} 限定超时
     */
    CompletableFuture<Object> execRemotingLogic0(final String cmd, final int timeoutSeconds, final Object[] params) throws Exception;

    /**
     * 执行远程服务器领域事件，亦可为rpc调用 <br>
     * 使用方式可参看 {@code execRemotingLogic} 异步回调
     *
     * @param cmd    前后端通信协议号，亦表示领域事件名 <br>
     *               <p>
     *               当为 rpc:接口完全限定名:接口方法名 时，表示为rpc调用
     *               </p>
     * @param params 当为rpc调用时则为方法参数，其它情况为普通领域事件处理器参数
     * @return 异步结果，须调用 {@code get(timeout, timeUnit)} 限定超时
     */
    CompletableFuture<Object> execRemotingLogic0(final String cmd, final Object[] params) throws Exception;

    /**
     * 执行远程服务器领域事件，亦可为rpc调用
     *
     * @param cmd            前后端通信协议号，亦表示领域事件名 <br>
     *                       <p>
     *                       当为 rpc:接口完全限定名:接口方法名 时，表示为rpc调用
     *                       </p>
     * @param timeoutSeconds 请求超时时间（秒），最大为30分钟
     * @param params         当为rpc调用时则为方法参数，其它情况为普通领域事件处理器参数
     * @return 远程服务器返回的结果
     * @throws Exception 远端异常或者获取结果超时
     */
    <T> T execRemotingLogic(final String cmd, final int timeoutSeconds, final Object[] params) throws Exception;

    /**
     * 执行远程服务器领域事件，亦可为rpc调用，默认5秒超时
     *
     * @param cmd    前后端通信协议号，亦表示领域事件名 <br>
     *               <p>
     *               当为 rpc:接口完全限定名:接口方法名 时，表示为rpc调用
     *               </p>
     * @param params 当为rpc调用时则为方法参数，其它情况为普通领域事件处理器参数
     * @return 远程服务器返回的结果
     * @throws Exception 远端异常或者获取结果超时
     */
    <T> T execRemotingLogic(final String cmd, Object... params) throws Exception;

    /**
     * 异步回调执行远程服务器领域事件，亦可为rpc调用
     *
     * @param cmd            前后端通信协议号，亦表示领域事件名 <br>
     *                       <p>
     *                       当为 rpc:接口完全限定名:接口方法名 时，表示为rpc调用
     *                       </p>
     * @param timeoutSeconds 请求超时时间（秒），最大为30分钟
     * @param params         当为rpc调用时则为方法参数，其它情况为普通领域事件处理器参数
     * @param action         结果回调，当用户对结果进行逻辑处理而抛出异常请务必处理此函数的异常参数，否则程序不会抛出异常
     */
    void execRemotingLogic(final String cmd, final int timeoutSeconds, final BiConsumer<Object, ? super Throwable> action, Object... params);

    /**
     * 异步回调执行远程服务器领域事件，亦可为rpc调用，默认5秒超时
     *
     * @param cmd    前后端通信协议号，亦表示领域事件名 <br>
     *               <p>
     *               当为 rpc:接口完全限定名:接口方法名 时，表示为rpc调用
     *               </p>
     * @param params 当为rpc调用时则为方法参数，其它情况为普通领域事件处理器参数
     * @param action 结果回调，当用户对结果进行逻辑处理而抛出异常请务必处理此函数的异常参数，否则程序不会抛出异常
     */
    void execRemotingLogic(final String cmd, final BiConsumer<Object, ? super Throwable> action, Object... params);
}
