package com.kaka.rpc.core;

/**
 * RPC操作码，亦是前后端通信协议号
 *
 * @author zkpursuit
 */
final public class RpcOpCode {

    /**
     * 客户端请求rpc调用
     */
    public final static String cs_rpc = "-104";

    /**
     * 服务端响应rpc调用返回给客户端
     */
    public final static String sc_rpc = "-105";

    /**
     * 错误相关
     */
    public final static int err_code = -777;

}
