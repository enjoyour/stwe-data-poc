package com.toowe.stwe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 海关数据文件访问配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.customs-data")
public class CustomsDataProperties {
    /**
     * 基础目录路径
     * 本地模式: D:/azs/share_volumn_01
     * 远程模式: smb://192.168.1.3/财务部/2-2核算组/23_智能体抓取数据源
     */
    private String baseDir;

    /**
     * 访问模式: local（本地）或 remote（远程SMB）
     */
    private String accessMode = "local";

    /**
     * SMB服务器地址（远程模式使用）
     */
    private String smbHost;

    /**
     * SMB共享路径（远程模式使用）
     * 例如: /财务部/2-2核算组/23_智能体抓取数据源
     */
    private String smbSharePath;

    /**
     * SMB用户名（如果需要认证）
     */
    private String smbUsername;

    /**
     * SMB密码（如果需要认证）
     */
    private String smbPassword;

    /**
     * SMB域名（可选）
     */
    private String smbDomain;
}