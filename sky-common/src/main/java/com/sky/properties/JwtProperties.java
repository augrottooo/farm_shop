package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component// 作用：将JwtProperties实例创建出来，存入 Spring IOC 容器。后续在TokenFilter、JwtUtils工具类中，可以直接@Autowired注入使用。
@ConfigurationProperties(prefix = "sky.jwt")// 核心：绑定 yml 中前缀为 sky.jwt 的所有配置，自动把 yml 的值注入到类的同名属性。
@Data// @Data（Lombok）自动生成：所有属性 getter、setter、toString、equals、hashCode。
public class JwtProperties {

    /**
     * 管理端员工生成jwt令牌相关配置
     */
    private String adminSecretKey;
    private long adminTtl;
    private String adminTokenName;

    /**
     * 用户端微信用户生成jwt令牌相关配置
     */
    private String userSecretKey;
    private long userTtl;
    private String userTokenName;

}
