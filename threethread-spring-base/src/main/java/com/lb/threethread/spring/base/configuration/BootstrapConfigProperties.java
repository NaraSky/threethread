package com.lb.threethread.spring.base.configuration;

import com.lb.threethread.core.executor.ThreadPoolExecutorProperties;
import com.lb.threethread.spring.base.parser.ConfigFileTypeEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 动态线程池启动配置属性类
 * <p>
 * 用于接收配置文件中关于动态线程池的相关配置，包括是否启用、Nacos配置、线程池配置等
 * </p>
 * <p>
 * 设计目的：
 * 1. 统一管理动态线程池的启动配置参数
 * 2. 支持从配置文件中自动绑定配置属性
 * 3. 为动态线程池的初始化和运行提供配置支持
 * </p>
 */
@Data
@ConfigurationProperties(prefix = BootstrapConfigProperties.PREFIX)
public class BootstrapConfigProperties {

    /**
     * 配置属性前缀
     * <p>
     * 在配置文件中使用 onethread 作为前缀，如 onethread.enable=true
     * </p>
     */
    public static final String PREFIX = "onethread";

    /**
     * 是否开启动态线程池开关
     * <p>
     * 默认为true，表示启用动态线程池功能
     * 可通过配置 onethread.enable=false 来关闭功能
     * </p>
     */
    private Boolean enable = Boolean.TRUE;

    /**
     * Nacos 配置文件
     * <p>
     * 包含Nacos配置中心的相关配置信息，如dataId和group
     * </p>
     */
    private NacosConfig nacos;

    /**
     * Nacos 远程配置文件格式类型
     * <p>
     * 指定Nacos配置文件的格式类型，如properties、yml、yaml等
     * </p>
     */
    private ConfigFileTypeEnum configFileType;

    /**
     * 线程池配置集合
     * <p>
     * 包含多个线程池的配置信息，用于批量创建和管理线程池
     * </p>
     */
    private List<ThreadPoolExecutorProperties> executors;

    /**
     * Nacos配置内部类
     * <p>
     * 用于封装Nacos配置中心的相关参数
     * </p>
     */
    @Data
    public static class NacosConfig {

        /**
         * Nacos配置的dataId
         * <p>
         * 用于唯一标识Nacos中的配置文件
         * </p>
         */
        private String dataId;

        /**
         * Nacos配置的group
         * <p>
         * 用于对配置进行分组管理
         * </p>
         */
        private String group;
    }
}