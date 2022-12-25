package io.github.mufasa1976.spring.webflux.static_resource_with_error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafProperties;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE;

@SpringBootTest
@AutoConfigureWebTestClient
class StaticResourceWithErrorApplicationTests {
  private static final Logger log = LoggerFactory.getLogger(StaticResourceWithErrorApplicationTests.class);
  @Autowired
  private WebTestClient web;

  @Value("${spring.thymeleaf.prefix:" + ThymeleafProperties.DEFAULT_PREFIX + "}")
  private String thymeleafPrefix = ThymeleafProperties.DEFAULT_PREFIX;

  @Autowired
  private ResourceLoader resourceLoader;

  @Test
  @DisplayName("GET / and expect redirected to /en")
  void defaultLandingPage() {
    web.get()
       .uri("/")
       .exchange()
       .expectStatus().isTemporaryRedirect()
       .expectHeader().location("/en");
  }

  @Test
  @DisplayName("GET / with Headers [Accept-Language:\"de\"] and expect redirected to /de")
  void defaultLandingPageWithGermanLocale() {
    web.get()
       .uri("/")
       .header(ACCEPT_LANGUAGE, "de")
       .exchange()
       .expectStatus().isTemporaryRedirect()
       .expectHeader().location("/de");
  }

  @Test
  @DisplayName("GET /en and expect the english index.html")
  void englishLandingPage() {
    web.get()
       .uri("/en")
       .exchange()
       .expectStatus().isOk()
       .expectHeader().contentType(MediaType.TEXT_HTML)
       .expectBody().consumeWith(response -> assertThat(response.getResponseBody())
           .asString()
           .isNotEmpty()
           .startsWith("""
               <!DOCTYPE html><html lang="en" dir="ltr"><head>
                 <meta charset="utf-8">
                 <title>Calcmaster</title>
                 <base href="/en/">
                 """));
  }

  @Test
  @DisplayName("GET /de and expect the german index.html")
  void germanLandingPage() {
    web.get()
       .uri("/de")
       .exchange()
       .expectStatus().isOk()
       .expectHeader().contentType(MediaType.TEXT_HTML)
       .expectBody().consumeWith(response -> assertThat(response.getResponseBody())
           .asString()
           .isNotEmpty()
           .startsWith("""
               <!DOCTYPE html><html lang="de" dir="ltr"><head>
                 <meta charset="utf-8">
                 <title>Calcmaster</title>
                 <base href="/de/">
                 """));
  }

  @Test
  @DisplayName("GET /en/main.6187e65880c98290.js and expect the Content of en/main.6187e65880c98290.js")
  void relativeResource() {
    web.get()
       .uri("/en/main.6187e65880c98290.js")
       .exchange()
       .expectStatus().isOk()
       .expectBody().consumeWith(response -> assertThat(response.getResponseBody())
           .asString()
           .isNotEmpty()
           .isEqualTo(getContent("en/main.6187e65880c98290.js")));
  }

  private String getContent(String location) {
    final var resource = resourceLoader.getResource(String.join("/", thymeleafPrefix, location));
    if (!resource.exists() || !resource.isFile() || !resource.isReadable()) {
      return null;
    }
    try (final Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
      return FileCopyUtils.copyToString(reader);
    } catch (IOException e) {
      log.error("Error while reading Test-Resource {}", location, e);
      return null;
    }
  }

  @Test
  @DisplayName("GET /en/favicon.ico and expect the Content of en/favicon.ico")
  void staticResource() {
    web.get()
       .uri("/en/favicon.ico")
       .exchange()
       .expectStatus().isOk()
       .expectBody().consumeWith(response -> assertThat(response.getResponseBody())
           .asString()
           .isNotEmpty()
           .isEqualTo(getContent("en/favicon.ico")));
  }

  @Test
  @DisplayName("GET /en/assets/themes.json and expect the Content of en/assets/themes.json")
  void assetsResource() {
    web.get()
       .uri("/en/assets/themes.json")
       .exchange()
       .expectStatus().isOk()
       .expectBody().consumeWith(response -> assertThat(response.getResponseBody())
           .asString()
           .isNotEmpty()
           .isEqualTo(getContent("en/assets/themes.json")));
  }
}
