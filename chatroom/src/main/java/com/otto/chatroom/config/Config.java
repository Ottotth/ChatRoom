package com.otto.chatroom.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Paths;

@Configuration
public class Config implements WebMvcConfigurer {

  @Value("${app.avatar-dir:uploads/avatars}")
  private String avatarDir;

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String absolutePath = Paths.get(avatarDir).toAbsolutePath().toString();
    // 為支援跨平台，統一用 URI 格式
    String fileUri = "file:" + (absolutePath.startsWith("/") ? "/" : "") + absolutePath + "/";
    registry.addResourceHandler("/avatars/**")
        .addResourceLocations(fileUri);
  }
}
