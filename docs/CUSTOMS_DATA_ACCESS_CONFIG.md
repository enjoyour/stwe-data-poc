# 海关数据文件访问配置说明

## 概述
系统支持两种方式访问海关数据Excel文件：
1. **本地模式**（local）：直接访问本地文件系统
2. **远程模式**（remote）：通过SMB/CIFS协议访问Windows网络共享

## 配置方式

### 方式1：本地开发环境（Windows本地）

在 `application.yml` 中配置：
```yaml
app:
  customs-data:
    access-mode: local
    base-dir: D:/azs/share_volumn_01
```

或者通过环境变量：
```bash
export CUSTOMS_DATA_ACCESS_MODE=local
export CUSTOMS_DATA_BASE_DIR=D:/azs/share_volumn_01
```

### 方式2：生产环境（Linux服务器访问Windows共享）

在 `application.yml` 中配置：
```yaml
app:
  customs-data:
    access-mode: remote
    smb-host: 192.168.1.3
    smb-share-path: /财务部/2-2核算组/23_智能体抓取数据源
    smb-username: your_username
    smb-password: your_password
    smb-domain: WORKGROUP
```

或者通过环境变量：
```bash
export CUSTOMS_DATA_ACCESS_MODE=remote
export CUSTOMS_DATA_SMB_HOST=192.168.1.3
export CUSTOMS_DATA_SMB_SHARE_PATH=/财务部/2-2核算组/23_智能体抓取数据源
export CUSTOMS_DATA_SMB_USERNAME=your_username
export CUSTOMS_DATA_SMB_PASSWORD=your_password
export CUSTOMS_DATA_SMB_DOMAIN=WORKGROUP
```

## SMB路径说明

Windows网络共享路径 `\\192.168.1.3\财务部\2-2核算组\23_智能体抓取数据源\`

转换为SMB URL格式：
```
smb://192.168.1.3/财务部/2-2核算组/23_智能体抓取数据源/
```

## 配置参数说明

| 参数 | 说明 | 必填 | 默认值 |
|------|------|------|--------|
| access-mode | 访问模式：local/remote | 是 | local |
| base-dir | 本地文件路径（本地模式） | 是 | - |
| smb-host | SMB服务器IP或域名（远程模式） | 是 | - |
| smb-share-path | SMB共享路径（远程模式） | 是 | - |
| smb-username | SMB用户名 | 否 | 空（匿名访问） |
| smb-password | SMB密码 | 否 | 空 |
| smb-domain | SMB域 | 否 | 空 |

## 验证步骤

### 1. 本地模式验证
```bash
# 设置本地模式
export CUSTOMS_DATA_ACCESS_MODE=local
export CUSTOMS_DATA_BASE_DIR=D:/azs/share_volumn_01

# 启动应用
mvn spring-boot:run

# 查看日志，应该看到：
# 使用本地模式访问文件: D:/azs/share_volumn_01
# 本地模式找到 X 个Excel文件
```

### 2. 远程模式验证
```bash
# 设置远程模式
export CUSTOMS_DATA_ACCESS_MODE=remote
export CUSTOMS_DATA_SMB_HOST=192.168.1.3
export CUSTOMS_DATA_SMB_SHARE_PATH=/财务部/2-2核算组/23_智能体抓取数据源

# 启动应用
mvn spring-boot:run

# 查看日志，应该看到：
# 使用SMB远程模式访问文件: your_username@192.168.1.3/财务部/2-2核算组/23_智能体抓取数据源
# 找到SMB Excel文件: xxx.xls
# SMB模式找到 X 个Excel文件
```

## 常见问题

### Q1: 连接SMB失败
**错误**: `jcifs.smb.SmbException: Access is denied`

**解决方案**:
1. 检查用户名密码是否正确
2. 确认Windows共享是否允许该用户访问
3. 尝试添加域配置：`smb-domain: WORKGROUP`

### Q2: 找不到文件
**错误**: `海关数据目录中没有Excel文件`

**解决方案**:
1. 检查路径是否正确
2. 确认共享文件夹中是否有.xls或.xlsx文件
3. 检查文件权限

### Q3: 匿名访问失败
**错误**: `jcifs.smb.SmbException: Logon failure`

**解决方案**:
配置用户名和密码：
```yaml
smb-username: your_username
smb-password: your_password
```

## Windows共享设置建议

1. **启用SMBv1/v2**：
   - 控制面板 → 程序 → 启用或关闭Windows功能
   - 勾选 "SMB 1.0/CIFS File Sharing Support"

2. **网络发现**：
   - 控制面板 → 网络和共享中心
   - 启用网络发现和文件共享

3. **权限设置**：
   - 右键共享文件夹 → 属性 → 共享
   - 添加用户并设置权限

## 性能优化建议

1. **缓存文件列表**：如果文件数量很多，可以考虑实现缓存机制
2. **连接池**：SMB连接可以考虑使用连接池
3. **异步处理**：文件读取可以使用异步方式提高并发性能

## 安全建议

1. **敏感信息保护**：
   - 不要在代码中硬编码密码
   - 使用环境变量或配置中心管理敏感信息

2. **网络安全**：
   - 确保SMB访问在可信网络中进行
   - 考虑使用VPN或专线

3. **访问控制**：
   - 使用最小权限原则配置SMB用户
   - 定期更换密码