package com.tongguo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@MapperScan("com.tongguo.mapper")
public class TongguoApplication {
    public static void main(String[] args) {
        initAppBaseDir();
        SpringApplication.run(TongguoApplication.class, args);
    }

    private static void initAppBaseDir() {
        Path baseDir = resolveAppBaseDir();
        try {
            Files.createDirectories(baseDir.resolve("data"));
            Files.createDirectories(baseDir.resolve("uploads"));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize application directories", e);
        }
        System.setProperty("app.base-dir", baseDir.toAbsolutePath().normalize().toString().replace('\\', '/'));
    }

    private static Path resolveAppBaseDir() {
        try {
            URI location = TongguoApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path path = Paths.get(location).toAbsolutePath().normalize();
            if (Files.isRegularFile(path)) {
                return path.getParent();
            }
            Path fileName = path.getFileName();
            Path parent = path.getParent();
            if (fileName != null && parent != null && "classes".equals(fileName.toString())) {
                Path targetDir = parent.getFileName() != null && "target".equals(parent.getFileName().toString())
                        ? parent
                        : null;
                if (targetDir != null && targetDir.getParent() != null) {
                    return targetDir.getParent();
                }
            }
            return path;
        } catch (Exception e) {
            return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        }
    }
}
