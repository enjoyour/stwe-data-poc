package com.toowe.stwe.service;

import com.toowe.stwe.config.CustomsDataProperties;
import jcifs.smb.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 海关数据文件访问服务
 * 支持本地文件和远程SMB共享文件访问
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomsFileAccessService {

    private final CustomsDataProperties properties;

    /**
     * 获取目录下的所有Excel文件
     */
    public List<FileResource> listExcelFiles() {
        if ("local".equalsIgnoreCase(properties.getAccessMode())) {
            return listLocalExcelFiles();
        } else {
            return listSmbExcelFiles();
        }
    }

    /**
     * 获取文件的输入流
     */
    public InputStream getFileInputStream(String fileName) throws IOException {
        if ("local".equalsIgnoreCase(properties.getAccessMode())) {
            return getLocalFileInputStream(fileName);
        } else {
            return getSmbFileInputStream(fileName);
        }
    }

    /**
     * 列出本地Excel文件
     */
    private List<FileResource> listLocalExcelFiles() {
        log.info("使用本地模式访问文件: {}", properties.getBaseDir());
        List<FileResource> resources = new ArrayList<>();

        File dir = new File(properties.getBaseDir());
        if (!dir.exists() || !dir.isDirectory()) {
            log.error("本地目录不存在: {}", dir.getAbsolutePath());
            return resources;
        }

        File[] files = dir.listFiles((d, name) ->
                name.toLowerCase().endsWith(".xls") || name.toLowerCase().endsWith(".xlsx"));

        if (files != null) {
            for (File file : files) {
                resources.add(new LocalFileResource(file));
            }
        }

        log.info("本地模式找到 {} 个Excel文件", resources.size());
        return resources;
    }

    /**
     * 列出SMB共享Excel文件
     */
    private List<FileResource> listSmbExcelFiles() {
        log.info("使用SMB远程模式访问文件: {}@{}{}",
                properties.getSmbUsername(), properties.getSmbHost(), properties.getSmbSharePath());

        List<FileResource> resources = new ArrayList<>();

        try {
            String smbPath = buildSmbPath();
            NtlmPasswordAuthentication auth = createAuthentication();
            SmbFile smbDir = new SmbFile(smbPath, auth);

            if (!smbDir.exists()) {
                log.error("SMB共享路径不存在: {}", smbPath);
                return resources;
            }

            // 列出文件
            SmbFile[] files = smbDir.listFiles();
            if (files != null) {
                for (SmbFile file : files) {
                    if (file.isFile() && isExcelFile(file.getName())) {
                        resources.add(new SmbFileResource(file, auth));
                        log.info("找到SMB Excel文件: {}", file.getName());
                    }
                }
            }

            log.info("SMB模式找到 {} 个Excel文件", resources.size());
        } catch (SmbException e) {
            log.error("访问SMB共享失败", e);
        } catch (IOException e) {
            log.error("IO异常", e);
        }

        return resources;
    }

    /**
     * 获取本地文件输入流
     */
    private InputStream getLocalFileInputStream(String fileName) throws IOException {
        String filePath = properties.getBaseDir() + File.separator + fileName;
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("文件不存在: " + filePath);
        }
        return java.nio.file.Files.newInputStream(file.toPath());
    }

    /**
     * 获取SMB文件输入流
     */
    private InputStream getSmbFileInputStream(String fileName) throws IOException {
        String smbPath = buildSmbPath() + "/" + fileName;
        NtlmPasswordAuthentication auth = createAuthentication();
        SmbFile smbFile = new SmbFile(smbPath, auth);

        if (!smbFile.exists()) {
            throw new IOException("SMB文件不存在: " + smbPath);
        }

        return smbFile.getInputStream();
    }

    /**
     * 创建SMB认证信息
     */
    private NtlmPasswordAuthentication createAuthentication() {
        if (properties.getSmbUsername() != null && !properties.getSmbUsername().isEmpty()) {
            log.info("使用用户名密码认证: {}", properties.getSmbUsername());
            return new NtlmPasswordAuthentication(
                    properties.getSmbDomain(),
                    properties.getSmbUsername(),
                    properties.getSmbPassword()
            );
        } else {
            log.info("使用匿名访问");
            // 匿名访问
            return new NtlmPasswordAuthentication("", "", "");
        }
    }

    /**
     * 构建SMB路径
     */
    private String buildSmbPath() {
        // smb://host/share/path
        // 确保路径格式正确
        String host = properties.getSmbHost();
        String sharePath = properties.getSmbSharePath();

        // 如果sharePath不以/开头，添加/
        if (!sharePath.startsWith("/")) {
            sharePath = "/" + sharePath;
        }

        return String.format("smb://%s%s", host, sharePath);
    }

    /**
     * 判断是否为Excel文件
     */
    private boolean isExcelFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".xls") || lower.endsWith(".xlsx");
    }

    /**
     * 文件资源接口
     */
    public interface FileResource {
        String getName();
        long getSize();
        InputStream getInputStream() throws IOException;
    }

    /**
     * 本地文件资源实现
     */
    private static class LocalFileResource implements FileResource {
        private final File file;

        public LocalFileResource(File file) {
            this.file = file;
        }

        @Override
        public String getName() {
            return file.getName();
        }

        @Override
        public long getSize() {
            return file.length();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return java.nio.file.Files.newInputStream(file.toPath());
        }
    }

    /**
     * SMB文件资源实现
     */
    private static class SmbFileResource implements FileResource {
        private final SmbFile file;
        private final NtlmPasswordAuthentication auth;

        public SmbFileResource(SmbFile file, NtlmPasswordAuthentication auth) {
            this.file = file;
            this.auth = auth;
        }

        @Override
        public String getName() {
            return file.getName();
        }

        @Override
        public long getSize() {
            try {
                return file.length();
            } catch (SmbException e) {
                return 0;
            } catch (IOException e) {
                return 0;
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return file.getInputStream();
        }
    }
}