# 日志信息获取 API 使用指南

## 概述

为 user-service 和 profile-service 添加了完整的日志信息获取 API，仅允许超级管理员角色访问。

## 安全特性

- **权限控制**: 仅超级管理员 (SUPER_ADMIN) 可以访问
- **JWT 认证**: 所有 API 都需要有效的 JWT token
- **文件名安全**: 防止路径遍历攻击
- **大小限制**: 限制读取和下载的文件大小
- **分页支持**: 大文件支持分页读取

## API 接口

### 1. 获取日志文件列表

**User Service**: `GET /api/logs/files`
**Profile Service**: `GET /api/logs/files`

```bash
# 获取 user-service 日志文件列表
curl -X GET "http://localhost:8082/api/logs/files" \
  -H "Authorization: Bearer <admin_token>"

# 获取 profile-service 日志文件列表
curl -X GET "http://localhost:8084/api/logs/files" \
  -H "Authorization: Bearer <admin_token>"
```

**响应示例**:
```json
[
  {
    "name": "user-service.log",
    "size": 1024000,
    "lastModified": 1640995200000,
    "path": "./logs/user-service.log"
  },
  {
    "name": "user-service-2024-01-01.0.log",
    "size": 512000,
    "lastModified": 1640908800000,
    "path": "./logs/user-service-2024-01-01.0.log"
  }
]
```

### 2. 获取日志文件内容

**User Service**: `GET /api/logs/files/{filename}`
**Profile Service**: `GET /api/logs/files/{filename}`

```bash
# 获取 user-service 日志内容（最后100行）
curl -X GET "http://localhost:8082/api/logs/files/user-service.log?offset=0&limit=100" \
  -H "Authorization: Bearer <admin_token>"

# 获取 profile-service 日志内容（从第200行开始，读取50行）
curl -X GET "http://localhost:8084/api/logs/files/profile-service.log?offset=200&limit=50" \
  -H "Authorization: Bearer <admin_token>"
```

**请求参数**:
- `offset`: 偏移量（从末尾开始计算，默认0）
- `limit`: 读取行数（默认100，最大1000）

**响应示例**:
```json
{
  "filename": "user-service.log",
  "totalLines": 5000,
  "lines": [
    "2024-01-01 10:00:00.123 [main] INFO  o.v.u.UserController - User login successful",
    "2024-01-01 10:00:01.456 [main] DEBUG o.v.u.UserService - Processing user request",
    "2024-01-01 10:00:02.789 [main] ERROR o.v.u.AuthController - Authentication failed"
  ],
  "fromIndex": 4900,
  "toIndex": 5000,
  "hasMore": true,
  "fileSize": 1024000
}
```

### 3. 下载日志文件

**User Service**: `GET /api/logs/files/{filename}/download`
**Profile Service**: `GET /api/logs/files/{filename}/download`

```bash
# 下载 user-service 日志文件
curl -X GET "http://localhost:8082/api/logs/files/user-service.log/download" \
  -H "Authorization: Bearer <admin_token>" \
  --output user-service.log

# 下载 profile-service 日志文件
curl -X GET "http://localhost:8084/api/logs/files/profile-service.log/download" \
  -H "Authorization: Bearer <admin_token>" \
  --output profile-service.log
```

### 4. 搜索日志内容

**User Service**: `GET /api/logs/search`
**Profile Service**: `GET /api/logs/search`

```bash
# 在 user-service 所有日志中搜索 "ERROR"
curl -X GET "http://localhost:8082/api/logs/search?keyword=ERROR" \
  -H "Authorization: Bearer <admin_token>"

# 在特定文件中搜索，分页显示
curl -X GET "http://localhost:8084/api/logs/search?keyword=database&filename=profile-service.log&page=0&size=20" \
  -H "Authorization: Bearer <admin_token>"
```

**请求参数**:
- `keyword`: 搜索关键字（必需）
- `filename`: 文件名过滤（可选）
- `page`: 页码（默认0）
- `size`: 每页大小（默认50，最大100）

**响应示例**:
```json
{
  "results": [
    {
      "filename": "user-service.log",
      "lineNumber": 1250,
      "content": "2024-01-01 10:00:02.789 [main] ERROR o.v.u.AuthController - Database connection failed",
      "timestamp": "2024-01-01 10:00:02.789"
    }
  ],
  "total": 1,
  "page": 0,
  "size": 20,
  "totalPages": 1
}
```

### 5. 获取最近日志条目

**User Service**: `GET /api/logs/recent`
**Profile Service**: `GET /api/logs/recent`

```bash
# 获取最近10条日志
curl -X GET "http://localhost:8082/api/logs/recent?limit=10" \
  -H "Authorization: Bearer <admin_token>"

# 获取最近ERROR级别日志
curl -X GET "http://localhost:8084/api/logs/recent?limit=20&level=ERROR" \
  -H "Authorization: Bearer <admin_token>"
```

**请求参数**:
- `limit`: 返回条目数量（默认10，最大100）
- `level`: 日志级别过滤（ERROR, WARN, INFO, DEBUG, TRACE）

**响应示例**:
```json
[
  {
    "filename": "user-service.log",
    "lineNumber": 5000,
    "content": "2024-01-01 10:00:05.123 [main] INFO  o.v.u.UserController - Request processed",
    "timestamp": "2024-01-01 10:00:05.123",
    "level": "INFO"
  }
]
```

### 6. 清理旧日志文件

**User Service**: `DELETE /api/logs/cleanup`
**Profile Service**: `DELETE /api/logs/cleanup`

```bash
# 清理30天前的日志文件
curl -X DELETE "http://localhost:8082/api/logs/cleanup?daysToKeep=30" \
  -H "Authorization: Bearer <admin_token>"

# 清理7天前的日志文件
curl -X DELETE "http://localhost:8084/api/logs/cleanup?daysToKeep=7" \
  -H "Authorization: Bearer <admin_token>"
```

**请求参数**:
- `daysToKeep`: 保留天数（默认30，最小1）

**响应示例**:
```json
{
  "message": "日志清理完成",
  "deletedFiles": [
    "user-service-2023-12-01.0.log",
    "user-service-2023-12-02.0.log"
  ],
  "deletedCount": 2
}
```

## 认证流程

### 1. 获取管理员 Token

```bash
# 登录获取 JWT token
curl -X POST "http://localhost:8082/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

### 2. 使用 Token 访问日志 API

```bash
# 将获取的 token 用于后续请求
TOKEN="your_jwt_token_here"

curl -X GET "http://localhost:8082/api/logs/files" \
  -H "Authorization: Bearer $TOKEN"
```

## 日志配置

### User Service 日志配置
```yaml
logging:
  level:
    root: INFO
    org.verytogether.userservice: DEBUG
    org.springframework.web: DEBUG
  file:
    name: ./logs/user-service.log
  logback:
    rollingpolicy:
      max-file-size: 10MB
      max-history: 30
      clean-history-on-start: true
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

### Profile Service 日志配置
```yaml
logging:
  level:
    root: INFO
    com.example.profileservice: DEBUG
    org.springframework.web: DEBUG
  file:
    name: ./logs/profile-service.log
  logback:
    rollingpolicy:
      max-file-size: 10MB
      max-history: 30
      clean-history-on-start: true
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

## 错误处理

### 常见错误码

- **401 Unauthorized**: 未提供 token 或 token 无效
- **403 Forbidden**: 用户不是超级管理员
- **404 Not Found**: 日志文件不存在
- **400 Bad Request**: 请求参数错误
- **500 Internal Server Error**: 服务器内部错误

### 错误响应示例

```json
{
  "error": "只有超级管理员可以查看日志文件"
}
```

## 安全注意事项

1. **文件名安全**: API 会验证文件名，防止路径遍历攻击
2. **大小限制**: 在线读取限制 1MB，超过需要下载
3. **权限验证**: 每次请求都会验证用户角色
4. **日志目录**: 日志文件存储在应用目录下的 logs 文件夹中
5. **敏感信息**: 确保日志中不包含敏感信息（如密码、token等）

## 监控和维护

### 1. 日志轮转
- 日志文件达到 10MB 自动轮转
- 保留最近 30 天的日志文件
- 应用启动时清理过期日志

### 2. 磁盘空间监控
```bash
# 检查日志目录大小
du -sh ./logs/

# 检查日志文件数量
ls -la ./logs/ | wc -l
```

### 3. 定期清理
```bash
# 手动清理 30 天前的日志
curl -X DELETE "http://localhost:8082/api/logs/cleanup?daysToKeep=30" \
  -H "Authorization: Bearer <admin_token>"

curl -X DELETE "http://localhost:8084/api/logs/cleanup?daysToKeep=30" \
  -H "Authorization: Bearer <admin_token>"
```

## 集成示例

### 前端集成示例

```javascript
// 日志管理类
class LogManager {
    constructor(baseUrl, token) {
        this.baseUrl = baseUrl;
        this.token = token;
    }

    async getLogFiles() {
        const response = await fetch(`${this.baseUrl}/api/logs/files`, {
            headers: {
                'Authorization': `Bearer ${this.token}`
            }
        });
        return await response.json();
    }

    async getLogFileContent(filename, offset = 0, limit = 100) {
        const response = await fetch(
            `${this.baseUrl}/api/logs/files/${filename}?offset=${offset}&limit=${limit}`,
            {
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            }
        );
        return await response.json();
    }

    async searchLogs(keyword, filename = '', page = 0, size = 50) {
        const params = new URLSearchParams({
            keyword,
            filename,
            page: page.toString(),
            size: size.toString()
        });

        const response = await fetch(`${this.baseUrl}/api/logs/search?${params}`, {
            headers: {
                'Authorization': `Bearer ${this.token}`
            }
        });
        return await response.json();
    }

    async cleanupOldLogs(daysToKeep = 30) {
        const response = await fetch(
            `${this.baseUrl}/api/logs/cleanup?daysToKeep=${daysToKeep}`,
            {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${this.token}`
                }
            }
        );
        return await response.json();
    }
}

// 使用示例
const logManager = new LogManager('http://localhost:8082', 'your_admin_token');

// 获取日志文件列表
logManager.getLogFiles().then(files => {
    console.log('Log files:', files);
});

// 搜索错误日志
logManager.searchLogs('ERROR').then(results => {
    console.log('Search results:', results);
});
```

这样，超级管理员就可以安全地监控和管理两个服务的日志信息了。