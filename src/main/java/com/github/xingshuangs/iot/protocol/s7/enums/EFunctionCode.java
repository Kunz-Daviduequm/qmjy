package com.github.xingshuangs.iot.protocol.s7.enums;


import java.util.HashMap;
import java.util.Map;

/**
 * 功能码 Job request/Ack-Data function codes
 *
 * @author xingshuang
 */
public enum EFunctionCode {
    /**
     * CPU服务
     */
    CPU_SERVICES((byte) 0x00),

    /**
     * 读变量
     */
    READ_VARIABLE((byte) 0x04),

    /**
     * 写变量
     */
    WRITE_VARIABLE((byte) 0x05),

    /**
     * 开始下载
     */
    START_DOWNLOAD((byte) 0x1A),

    /**
     * 下载阻塞
     */
    DOWNLOAD((byte) 0x1B),

    /**
     * 下载结束
     */
    END_DOWNLOAD((byte) 0x1C),

    /**
     * 开始上传
     */
    START_UPLOAD((byte) 0x1D),

    /**
     * 上传
     */
    UPLOAD((byte) 0x1E),

    /**
     * 结束上传
     */
    END_UPLOAD((byte) 0x1F),

    /**
     * 控制PLC
     */
    PLC_CONTROL((byte) 0x28),

    /**
     * 停止PLC
     */
    PLC_STOP((byte) 0x29),

    /**
     * 设置通信
     */
    SETUP_COMMUNICATION((byte) 0xF0),
    ;

    private static Map<Byte, EFunctionCode> map;

    public static EFunctionCode from(byte data) {
        if (map == null) {
            map = new HashMap<>();
            for (EFunctionCode item : EFunctionCode.values()) {
                map.put(item.code, item);
            }
        }
        return map.get(data);
    }

    private final byte code;

    EFunctionCode(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }
}
