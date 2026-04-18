/**
 * MomentKeep 朝暮记 - 统一响应结果类
 *
 * @description 封装API响应数据，提供统一的响应格式
 * @author MomentKeep Team
 * @since 2026-04-18
 */
package cn.edu.scnu.momentkeep.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果封装类
 * @param <T> 响应数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 状态码：200表示成功，其他表示错误 */
    private Integer code;
    /** 响应消息 */
    private String message;
    /** 响应数据 */
    private T data;
    /** 响应时间戳 */
    private Long timestamp;

    /**
     * 返回成功结果（无数据）
     * @param <T> 泛型类型
     * @return Result实例
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(),
                           ResultCode.SUCCESS.getMessage(),
                           null,
                           System.currentTimeMillis());
    }

    /**
     * 返回成功结果（带数据）
     * @param <T> 泛型类型
     * @param data 响应数据
     * @return Result实例
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(),
                           ResultCode.SUCCESS.getMessage(),
                           data,
                           System.currentTimeMillis());
    }

    /**
     * 返回错误结果（使用预定义错误码）
     * @param <T> 泛型类型
     * @param resultCode 错误码枚举
     * @return Result实例
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(),
                           resultCode.getMessage(),
                           null,
                           System.currentTimeMillis());
    }

    /**
     * 返回错误结果（自定义错误消息）
     * @param <T> 泛型类型
     * @param message 错误消息
     * @return Result实例
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCode.ERROR.getCode(),
                           message,
                           null,
                           System.currentTimeMillis());
    }
}
