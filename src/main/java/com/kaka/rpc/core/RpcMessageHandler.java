package com.kaka.rpc.core;

import com.kaka.notice.Command;
import com.kaka.notice.IResult;
import com.kaka.notice.Message;

/**
 * 领域事件处理器基类
 *
 * @author zkpursuit
 */
abstract public class RpcMessageHandler extends Command {

    /**
     * 领域事件处理方法
     *
     * @param message 事件对象
     */
    @Override
    public void execute(Message message) {
        if (message instanceof RpcMessage pm) {
            try {
                IResult<Object> result = pm.getResult("return");
                if (result != null) {
                    result.set(this.execute(pm));
                } else {
                    this.execute(pm);
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 领域事件处理方法
     *
     * @param message 事件对象
     * @return 处理结果
     * @throws Throwable 处理领域事件时可能抛出的异常
     */
    abstract public Object execute(RpcMessage message) throws Throwable;
}
