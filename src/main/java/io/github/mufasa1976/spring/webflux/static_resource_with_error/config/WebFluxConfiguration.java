package io.github.mufasa1976.spring.webflux.static_resource_with_error.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.thymeleaf.spring6.view.reactive.ThymeleafReactiveViewResolver;

import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@EnableWebFlux
public class WebFluxConfiguration implements WebFluxConfigurer {
  private static final String[] ANGULAR_RESOURCES = {
      "/favicon.ico",
      "/main.*.js",
      "/polyfills.*.js",
      "/runtime.*.js",
      "/styles.*.css",
      "/deeppurple-amber.css",
      "/indigo-pink.css",
      "/pink-bluegrey.css",
      "/purple-green.css",
      "/3rdpartylicenses.txt"
  };
  private final String prefix;
  private final ThymeleafReactiveViewResolver thymeleafReactiveViewResolver;

  public WebFluxConfiguration(@Value("${spring.thymeleaf.prefix:" + ThymeleafProperties.DEFAULT_PREFIX + "}") String prefix, ThymeleafReactiveViewResolver thymeleafReactiveViewResolver) {
    this.prefix = prefix;
    this.thymeleafReactiveViewResolver = thymeleafReactiveViewResolver;
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/webjars/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/")
            .resourceChain(true);
    registry.addResourceHandler("/**")
            .addResourceLocations(prefix);
  }

  @Override
  public void configureViewResolvers(ViewResolverRegistry registry) {
    registry.viewResolver(thymeleafReactiveViewResolver);
  }

  @Bean
  public RouterFunction<ServerResponse> routerFunction() {
    final var requestPredicate = Stream.concat(
        Stream.of(ANGULAR_RESOURCES),
        Stream.of("/assets/**", "/api/**", "/webjars/**")
    ).reduce(GET("/**"), (predicate, route) -> predicate.and(GET(route).negate()), RequestPredicate::and);
    return route(requestPredicate, request -> ServerResponse.ok()
                                                            .contentType(MediaType.TEXT_HTML)
                                                            .render("index"));
  }
}
