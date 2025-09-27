package com.example.profileservice.controller;

import com.example.profileservice.security.JwtTokenProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = {
    "http://localhost:3000",
    "http://localhost:8080",
    "http://localhost:4200",
    "http://localhost:5173",
    "http://127.0.0.1:3000",
    "http://127.0.0.1:8080",
    "http://127.0.0.1:4200",
    "http://127.0.0.1:5173"
})
public class LogController {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private static final String LOG_DIR = "./logs";
    private static final int MAX_FILE_SIZE_KB = 1024; // 1MB

    /**
     * 获取所有日志文件列表
     * 仅超级管理员可访问
     */
    @GetMapping("/files")
    public ResponseEntity<?> getLogFiles(@RequestHeader("Authorization") String authorization) {
        // 验证JWT token和用户权限
        if (!validateSuperAdminAccess(authorization)) {
            return ResponseEntity.status(403).body(Map.of("error", "只有超级管理员可以查看日志文件"));
        }

        try {
            Path logPath = Paths.get(LOG_DIR);
            if (!Files.exists(logPath)) {
                return ResponseEntity.ok(List.of());
            }

            List<Map<String, Object>> logFiles = new ArrayList<>();

            try (Stream<Path> paths = Files.list(logPath)) {
                paths.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(".log"))
                     .forEach(path -> {
                         try {
                             Map<String, Object> fileInfo = Map.of(
                                 "name", path.getFileName().toString(),
                                 "size", Files.size(path),
                                 "lastModified", Files.getLastModifiedTime(path).toMillis(),
                                 "path", path.toString()
                             );
                             logFiles.add(fileInfo);
                         } catch (IOException e) {
                             // 忽略无法访问的文件
                         }
                     });
            }

            return ResponseEntity.ok(logFiles);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "读取日志文件失败: " + e.getMessage()));
        }
    }

    /**
     * 获取指定日志文件的内容
     * 仅超级管理员可访问
     */
    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<?> getLogFileContent(
            @PathVariable String filename,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "100") int limit,
            @RequestHeader("Authorization") String authorization) {

        // 验证JWT token和用户权限
        if (!validateSuperAdminAccess(authorization)) {
            return ResponseEntity.status(403).body(Map.of("error", "只有超级管理员可以查看日志内容"));
        }

        // 验证文件名安全性
        if (!filename.endsWith(".log") || filename.contains("..") || filename.startsWith("/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的文件名"));
        }

        try {
            Path logPath = Paths.get(LOG_DIR, filename);
            if (!Files.exists(logPath) || !Files.isRegularFile(logPath)) {
                return ResponseEntity.notFound().build();
            }

            // 检查文件大小
            long fileSize = Files.size(logPath);
            if (fileSize > MAX_FILE_SIZE_KB * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "文件过大，请下载查看"));
            }

            // 读取文件内容
            List<String> lines = Files.readAllLines(logPath);

            // 应用分页（从末尾开始）
            int fromIndex = Math.max(0, lines.size() - offset - limit);
            int toIndex = Math.min(lines.size(), lines.size() - offset);

            if (fromIndex >= toIndex) {
                fromIndex = Math.max(0, lines.size() - limit);
                toIndex = lines.size();
            }

            List<String> content = lines.subList(fromIndex, toIndex);

            return ResponseEntity.ok(Map.of(
                "filename", filename,
                "totalLines", lines.size(),
                "lines", content,
                "fromIndex", fromIndex,
                "toIndex", toIndex,
                "hasMore", offset > 0,
                "fileSize", fileSize
            ));

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "读取日志文件失败: " + e.getMessage()));
        }
    }

    /**
     * 下载日志文件
     * 仅超级管理员可访问
     */
    @GetMapping("/files/{filename:.+}/download")
    public ResponseEntity<?> downloadLogFile(
            @PathVariable String filename,
            @RequestHeader("Authorization") String authorization) {

        // 验证JWT token和用户权限
        if (!validateSuperAdminAccess(authorization)) {
            return ResponseEntity.status(403).body(Map.of("error", "只有超级管理员可以下载日志文件"));
        }

        // 验证文件名安全性
        if (!filename.endsWith(".log") || filename.contains("..") || filename.startsWith("/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的文件名"));
        }

        try {
            Path logPath = Paths.get(LOG_DIR, filename);
            if (!Files.exists(logPath) || !Files.isRegularFile(logPath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(logPath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "下载文件失败: " + e.getMessage()));
        }
    }

    /**
     * 搜索日志内容
     * 仅超级管理员可访问
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchLogs(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "") String filename,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestHeader("Authorization") String authorization) {

        // 验证JWT token和用户权限
        if (!validateSuperAdminAccess(authorization)) {
            return ResponseEntity.status(403).body(Map.of("error", "只有超级管理员可以搜索日志"));
        }

        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "搜索关键字不能为空"));
        }

        try {
            Path logPath = Paths.get(LOG_DIR);
            if (!Files.exists(logPath)) {
                return ResponseEntity.ok(Map.of("results", List.of(), "total", 0));
            }

            List<Map<String, Object>> results = new ArrayList<>();

            try (Stream<Path> paths = Files.list(logPath)) {
                List<Path> logFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".log"))
                        .filter(path -> filename.isEmpty() || path.getFileName().toString().contains(filename))
                        .collect(Collectors.toList());

                for (Path logFile : logFiles) {
                    List<String> lines = Files.readAllLines(logFile);
                    String fileName = logFile.getFileName().toString();

                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        if (line.toLowerCase().contains(keyword.toLowerCase())) {
                            Map<String, Object> result = Map.of(
                                "filename", fileName,
                                "lineNumber", i + 1,
                                "content", line,
                                "timestamp", extractTimestamp(line)
                            );
                            results.add(result);
                        }
                    }
                }
            }

            // 分页处理
            int total = results.size();
            int fromIndex = Math.min(page * size, total);
            int toIndex = Math.min(fromIndex + size, total);

            List<Map<String, Object>> pageResults = results.subList(fromIndex, toIndex);

            return ResponseEntity.ok(Map.of(
                "results", pageResults,
                "total", total,
                "page", page,
                "size", size,
                "totalPages", (int) Math.ceil((double) total / size)
            ));

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "搜索日志失败: " + e.getMessage()));
        }
    }

    /**
     * 获取最近的日志条目
     * 仅超级管理员可访问
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentLogs(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "") String level,
            @RequestHeader("Authorization") String authorization) {

        // 验证JWT token和用户权限
        if (!validateSuperAdminAccess(authorization)) {
            return ResponseEntity.status(403).body(Map.of("error", "只有超级管理员可以查看日志"));
        }

        if (limit > 100) {
            limit = 100; // 限制最大返回数量
        }

        try {
            Path logPath = Paths.get(LOG_DIR);
            if (!Files.exists(logPath)) {
                return ResponseEntity.ok(List.of());
            }

            List<Map<String, Object>> recentLogs = new ArrayList<>();

            try (Stream<Path> paths = Files.list(logPath)) {
                List<Path> logFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".log"))
                        .sorted((p1, p2) -> {
                            try {
                                return Long.compare(Files.getLastModifiedTime(p2).toMillis(),
                                                  Files.getLastModifiedTime(p1).toMillis());
                            } catch (IOException e) {
                                return 0;
                            }
                        })
                        .limit(5) // 只检查最近5个日志文件
                        .collect(Collectors.toList());

                for (Path logFile : logFiles) {
                    List<String> lines = Files.readAllLines(logFile);
                    String fileName = logFile.getFileName().toString();

                    // 从末尾开始读取
                    for (int i = lines.size() - 1; i >= 0 && recentLogs.size() < limit; i--) {
                        String line = lines.get(i);

                        // 如果指定了日志级别，进行过滤
                        if (!level.isEmpty() && !line.toLowerCase().contains(level.toLowerCase())) {
                            continue;
                        }

                        Map<String, Object> logEntry = Map.of(
                            "filename", fileName,
                            "lineNumber", i + 1,
                            "content", line,
                            "timestamp", extractTimestamp(line),
                            "level", extractLogLevel(line)
                        );
                        recentLogs.add(logEntry);
                    }
                }
            }

            return ResponseEntity.ok(recentLogs);

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "读取日志失败: " + e.getMessage()));
        }
    }

    /**
     * 清理旧日志文件
     * 仅超级管理员可访问
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<?> cleanupOldLogs(
            @RequestParam(defaultValue = "30") int daysToKeep,
            @RequestHeader("Authorization") String authorization) {

        // 验证JWT token和用户权限
        if (!validateSuperAdminAccess(authorization)) {
            return ResponseEntity.status(403).body(Map.of("error", "只有超级管理员可以清理日志"));
        }

        if (daysToKeep < 1) {
            return ResponseEntity.badRequest().body(Map.of("error", "保留天数必须大于0"));
        }

        try {
            Path logPath = Paths.get(LOG_DIR);
            if (!Files.exists(logPath)) {
                return ResponseEntity.ok(Map.of("message", "日志目录不存在", "deletedFiles", List.of()));
            }

            List<String> deletedFiles = new ArrayList<>();
            long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L);

            try (Stream<Path> paths = Files.list(logPath)) {
                List<Path> oldFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".log"))
                        .filter(path -> {
                            try {
                                return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                            } catch (IOException e) {
                                return false;
                            }
                        })
                        .collect(Collectors.toList());

                for (Path oldFile : oldFiles) {
                    try {
                        Files.delete(oldFile);
                        deletedFiles.add(oldFile.getFileName().toString());
                    } catch (IOException e) {
                        // 继续删除其他文件
                    }
                }
            }

            return ResponseEntity.ok(Map.of(
                "message", "日志清理完成",
                "deletedFiles", deletedFiles,
                "deletedCount", deletedFiles.size()
            ));

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "清理日志失败: " + e.getMessage()));
        }
    }

    /**
     * 验证超级管理员访问权限
     */
    private boolean validateSuperAdminAccess(String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return false;
            }

            String token = authorization.substring(7);

            // 验证token有效性
            if (!jwtTokenProvider.validateToken(token)) {
                return false;
            }

            // 调用user-service验证用户角色
            String username = jwtTokenProvider.getUsernameFromToken(token);

            // 这里应该调用user-service的API验证用户角色
            // 为了简化，我们检查用户名是否为admin
            // 在生产环境中，需要调用: http://localhost:8082/api/auth/current-user
            return "admin".equals(username);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从日志行提取时间戳
     */
    private String extractTimestamp(String logLine) {
        // 常见日志格式：2024-01-01 10:00:00.123
        String[] parts = logLine.split(" ");
        if (parts.length >= 2) {
            return parts[0] + " " + parts[1];
        }
        return "";
    }

    /**
     * 从日志行提取日志级别
     */
    private String extractLogLevel(String logLine) {
        String lowerLine = logLine.toLowerCase();
        if (lowerLine.contains("error")) return "ERROR";
        if (lowerLine.contains("warn")) return "WARN";
        if (lowerLine.contains("info")) return "INFO";
        if (lowerLine.contains("debug")) return "DEBUG";
        if (lowerLine.contains("trace")) return "TRACE";
        return "UNKNOWN";
    }
}