package com.kaka.rpc.core;

import com.kaka.util.ReflectUtils;
import com.kaka.util.Serializer;
import com.kaka.util.StringUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

final class Utils {
    /**
     * 写入异常基础信息到调用方
     *
     * @param sendBuf 发送缓冲区
     * @param exInfo  异常基础信息
     */
    static void writeException(ByteBuf sendBuf, String exInfo, Serializer<Object> serializer) {
        sendBuf.writeByte(-1); //写入异常
        writeValue(sendBuf, exInfo, serializer);
    }

    /**
     * 写入异常详情到调用方
     *
     * @param sendBuf 发送缓冲区
     * @param ex      异常
     */
    static void writeException(ByteBuf sendBuf, Throwable ex, Serializer<Object> serializer) {
        try (ByteArrayOutputStream baOs = new ByteArrayOutputStream()) {
            try (PrintStream ps = new PrintStream(baOs, true, StandardCharsets.UTF_8)) {
                ex.printStackTrace(ps);
            }
            sendBuf.writeByte(-1); //写入异常
            byte[] exBytes = baOs.toByteArray();
            writeValue(sendBuf, exBytes, serializer);
        } catch (Exception e) {
            writeException(sendBuf, "关闭远端输出流错误", serializer);
        }
    }

    /**
     * 写入返回到调用方的结果
     *
     * @param sendBuf 发送缓冲区
     * @param result  返回到调用方的结果数据
     */
    static void writeResult(ByteBuf sendBuf, Object result, Serializer<Object> serializer) {
        sendBuf.writeByte(1); //写入正确的结果
        writeValue(sendBuf, result, serializer);
    }

    /**
     * 向字节缓冲区写入数据
     *
     * @param sendBuf    字节缓冲区
     * @param value      待写入的数据
     * @param serializer 待写入的非基础类型数据序列化器
     */
    static void writeValue(ByteBuf sendBuf, Object value, Serializer<Object> serializer) {
        if (value == null) {
            sendBuf.writeByte(0);
            return;
        }
        Class<?> clazz = value.getClass();
        if (clazz == Byte.class) {
            sendBuf.writeByte(1);
            sendBuf.writeByte((byte) value);
        } else if (clazz == Boolean.class) {
            sendBuf.writeByte(2);
            sendBuf.writeBoolean((boolean) value);
        } else if (clazz == Short.class) {
            sendBuf.writeByte(3);
            sendBuf.writeShort((short) value);
        } else if (clazz == Integer.class) {
            sendBuf.writeByte(4);
            sendBuf.writeInt((int) value);
        } else if (clazz == Long.class) {
            sendBuf.writeByte(5);
            sendBuf.writeLong((long) value);
        } else if (clazz == Float.class) {
            sendBuf.writeByte(6);
            sendBuf.writeFloat((float) value);
        } else if (clazz == Double.class) {
            sendBuf.writeByte(7);
            sendBuf.writeDouble((double) value);
        } else if (clazz == String.class) {
            sendBuf.writeByte(8);
            byte[] bytes = ((String) value).getBytes(StandardCharsets.UTF_8);
            sendBuf.writeInt(bytes.length);
            sendBuf.writeBytes(bytes);
        } else if (clazz == Character.class) {
            sendBuf.writeByte(9);
            sendBuf.writeChar((char) value);
        } else if (clazz == byte[].class) {
            sendBuf.writeByte(11);
            byte[] bytes = (byte[]) value;
            sendBuf.writeInt(bytes.length);
            sendBuf.writeBytes(bytes);
        } else if (clazz == boolean[].class) {
            sendBuf.writeByte(12);
            boolean[] bs = (boolean[]) value;
            sendBuf.writeInt(bs.length);
            for (boolean b : bs) {
                sendBuf.writeBoolean(b);
            }
        } else if (clazz == short[].class) {
            sendBuf.writeByte(13);
            short[] s = (short[]) value;
            sendBuf.writeInt(s.length);
            for (short b : s) {
                sendBuf.writeShort(b);
            }
        } else if (clazz == int[].class) {
            sendBuf.writeByte(14);
            int[] s = (int[]) value;
            sendBuf.writeInt(s.length);
            for (int b : s) {
                sendBuf.writeInt(b);
            }
        } else if (clazz == long[].class) {
            sendBuf.writeByte(15);
            long[] s = (long[]) value;
            sendBuf.writeInt(s.length);
            for (long b : s) {
                sendBuf.writeLong(b);
            }
        } else if (clazz == float[].class) {
            sendBuf.writeByte(16);
            float[] s = (float[]) value;
            sendBuf.writeInt(s.length);
            for (float b : s) {
                sendBuf.writeFloat(b);
            }
        } else if (clazz == double[].class) {
            sendBuf.writeByte(17);
            double[] s = (double[]) value;
            sendBuf.writeInt(s.length);
            for (double b : s) {
                sendBuf.writeDouble(b);
            }
        } else if (clazz == String[].class) {
            sendBuf.writeByte(18);
            String[] s = (String[]) value;
            sendBuf.writeInt(s.length);
            for (String b : s) {
                byte[] bytes = b.getBytes(StandardCharsets.UTF_8);
                sendBuf.writeInt(bytes.length);
                sendBuf.writeBytes(bytes);
            }
        } else if (clazz == char[].class) {
            sendBuf.writeByte(19);
            char[] s = (char[]) value;
            sendBuf.writeInt(s.length);
            for (char b : s) {
                sendBuf.writeChar(b);
            }
        } else if (clazz == Byte[].class) {
            sendBuf.writeByte(21);
            Byte[] s = (Byte[]) value;
            sendBuf.writeInt(s.length);
            for (Byte b : s) {
                sendBuf.writeByte(b);
            }
        } else if (clazz == Boolean[].class) {
            sendBuf.writeByte(22);
            Boolean[] s = (Boolean[]) value;
            sendBuf.writeInt(s.length);
            for (Boolean b : s) {
                sendBuf.writeBoolean(b);
            }
        } else if (clazz == Short[].class) {
            sendBuf.writeByte(23);
            Short[] s = (Short[]) value;
            sendBuf.writeInt(s.length);
            for (Short b : s) {
                sendBuf.writeShort(b);
            }
        } else if (clazz == Integer[].class) {
            sendBuf.writeByte(24);
            Integer[] s = (Integer[]) value;
            sendBuf.writeInt(s.length);
            for (Integer b : s) {
                sendBuf.writeInt(b);
            }
        } else if (clazz == Long[].class) {
            sendBuf.writeByte(25);
            Long[] s = (Long[]) value;
            sendBuf.writeInt(s.length);
            for (Long b : s) {
                sendBuf.writeLong(b);
            }
        } else if (clazz == Float[].class) {
            sendBuf.writeByte(26);
            Float[] s = (Float[]) value;
            sendBuf.writeInt(s.length);
            for (Float b : s) {
                sendBuf.writeFloat(b);
            }
        } else if (clazz == Double[].class) {
            sendBuf.writeByte(27);
            Double[] s = (Double[]) value;
            sendBuf.writeInt(s.length);
            for (Double b : s) {
                sendBuf.writeDouble(b);
            }
        } else if (clazz == Character[].class) {
            sendBuf.writeByte(29);
            Character[] s = (Character[]) value;
            sendBuf.writeInt(s.length);
            for (Character b : s) {
                sendBuf.writeChar(b);
            }
        } else {
            sendBuf.writeByte(100);
            byte[] bytes = serializer.serialize(value);
            sendBuf.writeInt(bytes.length);
            sendBuf.writeBytes(bytes);
        }
    }

    /**
     * 从字节缓冲区读取数据
     *
     * @param byteBuf    字节缓冲区
     * @param serializer 待读取的非基础类型数据序列化器
     * @return 反序列化后的数据
     */
    static Object readValue(ByteBuf byteBuf, Serializer<Object> serializer) {
        int type = byteBuf.readByte();
        switch (type) {
            case 0:
                return null;
            case 1:
                return byteBuf.readByte();
            case 2:
                return byteBuf.readBoolean();
            case 3:
                return byteBuf.readShort();
            case 4:
                return byteBuf.readInt();
            case 5:
                return byteBuf.readLong();
            case 6:
                return byteBuf.readFloat();
            case 7:
                return byteBuf.readDouble();
            case 8:
                int strLen = byteBuf.readInt();
                byte[] bytes = new byte[strLen];
                byteBuf.readBytes(bytes);
                return new String(bytes, StandardCharsets.UTF_8);
            case 9:
                return byteBuf.readChar();
            case 11:
                int len11 = byteBuf.readInt();
                byte[] a1a = new byte[len11];
                byteBuf.readBytes(a1a);
                return a1a;
            case 12:
                int len12 = byteBuf.readInt();
                boolean[] a12 = new boolean[len12];
                for (int i = 0; i < len12; i++) {
                    a12[i] = byteBuf.readBoolean();
                }
                return a12;
            case 13:
                int len13 = byteBuf.readInt();
                short[] a13 = new short[len13];
                for (int i = 0; i < len13; i++) {
                    a13[i] = byteBuf.readShort();
                }
                return a13;
            case 14:
                int len14 = byteBuf.readInt();
                int[] a14 = new int[len14];
                for (int i = 0; i < len14; i++) {
                    a14[i] = byteBuf.readInt();
                }
                return a14;
            case 15:
                int len15 = byteBuf.readInt();
                long[] a15 = new long[len15];
                for (int i = 0; i < len15; i++) {
                    a15[i] = byteBuf.readLong();
                }
                return a15;
            case 16:
                int len16 = byteBuf.readInt();
                float[] a16 = new float[len16];
                for (int i = 0; i < len16; i++) {
                    a16[i] = byteBuf.readFloat();
                }
                return a16;
            case 17:
                int len17 = byteBuf.readInt();
                double[] a17 = new double[len17];
                for (int i = 0; i < len17; i++) {
                    a17[i] = byteBuf.readDouble();
                }
                return a17;
            case 18:
                int len18 = byteBuf.readInt();
                String[] a18 = new String[len18];
                for (int i = 0; i < len18; i++) {
                    int sLen = byteBuf.readInt();
                    byte[] sbs = new byte[sLen];
                    byteBuf.readBytes(sbs);
                    a18[i] = new String(sbs, StandardCharsets.UTF_8);
                }
                return a18;
            case 19:
                int len19 = byteBuf.readInt();
                char[] a19 = new char[len19];
                for (int i = 0; i < len19; i++) {
                    a19[i] = byteBuf.readChar();
                }
                return a19;
            case 21:
                int len21 = byteBuf.readInt();
                Byte[] a21 = new Byte[len21];
                for (int i = 0; i < len21; i++) {
                    a21[i] = byteBuf.readByte();
                }
                return a21;
            case 22:
                int len22 = byteBuf.readInt();
                Boolean[] a22 = new Boolean[len22];
                for (int i = 0; i < len22; i++) {
                    a22[i] = byteBuf.readBoolean();
                }
                return a22;
            case 23:
                int len23 = byteBuf.readInt();
                Short[] a23 = new Short[len23];
                for (int i = 0; i < len23; i++) {
                    a23[i] = byteBuf.readShort();
                }
                return a23;
            case 24:
                int len24 = byteBuf.readInt();
                Integer[] a24 = new Integer[len24];
                for (int i = 0; i < len24; i++) {
                    a24[i] = byteBuf.readInt();
                }
                return a24;
            case 25:
                int len25 = byteBuf.readInt();
                Long[] a25 = new Long[len25];
                for (int i = 0; i < len25; i++) {
                    a25[i] = byteBuf.readLong();
                }
                return a25;
            case 26:
                int len26 = byteBuf.readInt();
                Float[] a26 = new Float[len26];
                for (int i = 0; i < len26; i++) {
                    a26[i] = byteBuf.readFloat();
                }
                return a26;
            case 27:
                int len27 = byteBuf.readInt();
                Double[] a27 = new Double[len27];
                for (int i = 0; i < len27; i++) {
                    a27[i] = byteBuf.readDouble();
                }
                return a27;
            case 29:
                int len29 = byteBuf.readInt();
                Character[] a29 = new Character[len29];
                for (int i = 0; i < len29; i++) {
                    a29[i] = byteBuf.readChar();
                }
                return a29;
            default:
                int len100 = byteBuf.readInt();
                byte[] bbs = new byte[len100];
                byteBuf.readBytes(bbs);
                return serializer.deserialize(bbs);
        }
    }

    static Method getMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        for (Class<?> cls = clazz; cls != Object.class; cls = cls.getSuperclass()) {
            try {
                Method[] methods = cls.getDeclaredMethods();
                for (Method m : methods) {
                    if (!m.getName().equals(methodName)) continue;
                    Class<?>[] parameterTypes = m.getParameterTypes();
                    if (parameterTypes.length != paramTypes.length) continue;
                    boolean match = true;
                    for (int i = 0; i < paramTypes.length; i++) {
                        Class<?> paramType = paramTypes[i];
                        if (parameterTypes[i] != paramType) {
                            if (parameterTypes[i] == int.class && paramType == Integer.class)
                                continue;
                            if (parameterTypes[i] == long.class && paramType == Long.class)
                                continue;
                            if (parameterTypes[i] == float.class && paramType == Float.class)
                                continue;
                            if (parameterTypes[i] == double.class && paramType == Double.class)
                                continue;
                            if (parameterTypes[i] == boolean.class && paramType == Boolean.class)
                                continue;
                            if (parameterTypes[i] == byte.class && paramType == Byte.class)
                                continue;
                            if (parameterTypes[i] == short.class && paramType == Short.class)
                                continue;
                            if (parameterTypes[i] == char.class && paramType == Character.class)
                                continue;
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        return m;
                    }
                }
            } catch (SecurityException e) {
                //错误后直接循环，从父类中获取
            }
        }
        return null;
    }

    /**
     * 获取接口类所有方法
     *
     * @param interfaceClass 接口类
     * @return 接口唯一标识映射方法对象
     */
    static Map<Long, Method> getAllMethods(Class<?> interfaceClass) {
        Map<Long, Method> allMethods = new HashMap<>();
        Class<?>[] interfaces = interfaceClass.getInterfaces();
        for (Class<?> clazz : interfaces) {
            allMethods.putAll(getAllMethods(clazz));
        }
        Method[] declaredMethods = interfaceClass.getDeclaredMethods();
        for (Method method : declaredMethods) {
            String methodStr = method.toGenericString().replace("public ", "").replace("abstract", "").trim();
            allMethods.put(StringUtils.toNumber(methodStr), method);
        }
        return allMethods;
    }

    static Method findMethod(Class<?> clazz, String methodName, Object[] params) {
        try {
            if (params != null) {
                Class<?>[] paramTypes = new Class<?>[params.length];
                for (int i = 0; i < params.length; i++) {
                    paramTypes[i] = params[i].getClass();
                }
                return getMethod(clazz, methodName, paramTypes);
            }
            return ReflectUtils.getDeclaredMethod(clazz, methodName);
        } catch (Exception ignored) {
            return null;
        }
    }
}
