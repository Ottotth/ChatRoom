package com.otto.chatroom.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/avatars")
public class AvatarController {

  @Value("${app.avatar-dir:uploads/avatars}")
  private String avatarDir;

  @GetMapping("/{filename:.+}")
  public ResponseEntity<Resource> serveAvatar(@PathVariable String filename) {
    try {
      Path baseDir = resolveAvatarDir();
      Path filePath = baseDir.resolve(filename).normalize();

      // Security check: prevent directory traversal.
      if (!filePath.getParent().equals(baseDir)) {
        return ResponseEntity.notFound().build();
      }

      if (!Files.exists(filePath)) {
        return ResponseEntity.notFound().build();
      }

      Resource resource = new FileSystemResource(filePath.toFile());
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
          .contentType(MediaType.parseMediaType(guessMediaType(filename)))
          .body(resource);
    } catch (Exception e) {
      return ResponseEntity.notFound().build();
    }
  }

  private Path resolveAvatarDir() {
    Path configured = Paths.get(avatarDir);
    if (configured.isAbsolute()) {
      return configured.normalize();
    }

    Path userDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    Path direct = userDir.resolve(configured).normalize();
    Path chatroomModule = userDir.resolve("chatroom").resolve(configured).normalize();

    if (Files.exists(direct)) {
      return direct;
    }
    if (Files.exists(chatroomModule)) {
      return chatroomModule;
    }

    if (Files.exists(userDir.resolve("pom.xml"))) {
      return direct;
    }
    if (Files.exists(userDir.resolve("chatroom").resolve("pom.xml"))) {
      return chatroomModule;
    }

    return direct;
  }

  private String guessMediaType(String filename) {
    String lower = filename.toLowerCase();
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
      return "image/jpeg";
    }
    if (lower.endsWith(".png")) {
      return "image/png";
    }
    if (lower.endsWith(".gif")) {
      return "image/gif";
    }
    if (lower.endsWith(".webp")) {
      return "image/webp";
    }
    return "application/octet-stream";
  }
}
