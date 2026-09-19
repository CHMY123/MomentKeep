/**
 * MomentKeep 朝暮记 - 用户实体类
 *
 * @description 用户数据模型，对应数据库user表
 * @author MomentKeep Team
 * @since 2026-04-18
 */
package cn.edu.scnu.momentkeep.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 存储用户基本信息，包含登录凭证、角色和状态等
 */
@Data
@TableName("user")
public class User {
    /** 用户ID - 自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 - 唯一标识 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 头像URL */
    private String avatar;

    /** 密码（加密存储）；禁止序列化返回前端、禁止进入日志 */
    @JsonIgnore
    @ToString.Exclude
    private String password;

    /** 创建时间 - 插入时自动填充 */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 - 插入和更新时自动填充 */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 状态（1正常，-1已注销） */
    private Integer status;

    /** 角色（USER普通用户/ADMIN管理员） */
    private String role;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /**
     * 令牌版本号
     *
     * <p>登出、改密、注销时自增，使已签发的 JWT 立即失效（JWT 本身无状态，
     * 用版本号做吊销，避免为了黑名单再引入 Redis）。</p>
     */
    private Integer tokenVersion;
}
