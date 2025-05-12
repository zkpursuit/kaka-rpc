package com.test;

/**
 * rpc接口
 *
 * @author zkpursuit
 */
public interface DemoService {

    String say(String text);

    int add(int a, int b);

    long sum(int min, int max);

    float divide(float a, float b);

    int num(int val);

}
