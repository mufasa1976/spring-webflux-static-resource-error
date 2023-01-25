package io.github.mufasa1976.spring.webflux.static_resource_with_error.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class TestController {
  @GetMapping("/api/v1/test")
  public Mono<String> test() {
    return Mono.just("test");
  }
}
