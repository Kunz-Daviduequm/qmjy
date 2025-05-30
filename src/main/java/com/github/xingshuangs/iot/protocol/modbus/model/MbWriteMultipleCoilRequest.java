package com.github.xingshuangs.iot.protocol.modbus.model;


import com.github.xingshuangs.iot.protocol.modbus.enums.EMbFunctionCode;
import com.github.xingshuangs.iot.protocol.common.buff.ByteReadBuff;
import com.github.xingshuangs.iot.protocol.common.buff.ByteWriteBuff;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 请求写多个线圈
 *
 * @author xingshuang
 */
@EqualsAndHashCode(callSuper = true)
@Data
public final class MbWriteMultipleCoilRequest extends MbPdu {

    /**
     * 输出地址 说是从0x0000 至 0xFFFF，但对应实际却只是0001-9999，对应0x0000-0x270F <br>
     * 字节大小：2个字节
     */
    private int address;

    /**
     * 输出数量 0x0001 至 0x07B0 <br>
     * 字节大小：2个字节
     */
    private int quantity;

    /**
     * 字节数 N* <br>
     * 字节大小：1个字节
     */
    private int count;

    /**
     * 输出值，域比特位置中的逻辑“1”请求相应输出为ON。域比特位置中的逻辑“0”请求相应输出为 OFF。
     * 字节大小：N*×1 个字节
     */
    private byte[] value;

    public MbWriteMultipleCoilRequest() {
        this.functionCode = EMbFunctionCode.WRITE_MULTIPLE_COIL;
    }

    public MbWriteMultipleCoilRequest(int address, int quantity, byte[] value) {
        this.functionCode = EMbFunctionCode.WRITE_MULTIPLE_COIL;
        this.address = address;
        this.quantity = quantity;
        this.count = value.length;
        this.value = value;
    }

    @Override
    public int byteArrayLength() {
        return super.byteArrayLength() + 5 + this.value.length;
    }

    @Override
    public byte[] toByteArray() {
        return ByteWriteBuff.newInstance(this.byteArrayLength())
                .putByte(this.functionCode.getCode())
                .putShort(this.address)
                .putShort(this.quantity)
                .putByte(this.count)
                .putBytes(this.value)
                .getData();
    }

    /**
     * 解析字节数组数据
     *
     * @param data 字节数组数据
     * @return MbWriteMultipleCoilRequest
     */
    public static MbWriteMultipleCoilRequest fromBytes(final byte[] data) {
        return fromBytes(data, 0);
    }

    /**
     * 解析字节数组数据
     *
     * @param data   字节数组数据
     * @param offset 偏移量
     * @return MbWriteMultipleCoilRequest
     */
    public static MbWriteMultipleCoilRequest fromBytes(final byte[] data, final int offset) {
        ByteReadBuff buff = new ByteReadBuff(data, offset);
        MbWriteMultipleCoilRequest res = new MbWriteMultipleCoilRequest();
        res.functionCode = EMbFunctionCode.from(buff.getByte());
        res.address = buff.getUInt16();
        res.quantity = buff.getUInt16();
        res.count = buff.getByteToInt();
        res.value = buff.getBytes(res.count);
        return res;
    }
}
