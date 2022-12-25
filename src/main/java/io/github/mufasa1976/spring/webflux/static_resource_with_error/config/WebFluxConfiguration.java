package io.github.mufasa1976.spring.webflux.static_resource_with_error.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.server.*;
import org.thymeleaf.spring6.view.reactive.ThymeleafReactiveViewResolver;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
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
  private static final List<Locale> SUPPORTED_LANGUAGES = List.of(Locale.GERMAN, Locale.ENGLISH);
  private static final Locale DEFAULT_LANGUAGE = Locale.ENGLISH;

  private final String prefix;
  private final ThymeleafReactiveViewResolver thymeleafReactiveViewResolver;

  public WebFluxConfiguration(@Value("${spring.thymeleaf.prefix:" + ThymeleafProperties.DEFAULT_PREFIX + "}") String prefix, ThymeleafReactiveViewResolver thymeleafReactiveViewResolver) {
    this.prefix = prefix;
    this.thymeleafReactiveViewResolver = thymeleafReactiveViewResolver;
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.setOrder(1);
    registry.addResourceHandler("/webjars/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/")
            .resourceChain(true);
    SUPPORTED_LANGUAGES.forEach(addLocalizedAngularResourcesTo(registry));
  }

  private Consumer<Locale> addLocalizedAngularResourcesTo(ResourceHandlerRegistry registry) {
    return language -> {
      final var relativeAngularResources = Stream.of(ANGULAR_RESOURCES)
                                                 .filter(resource -> StringUtils.contains(resource, "*"))
                                                 .map(resource -> "/" + language.getLanguage() + resource)
                                                 .toArray(String[]::new);
      registry.addResourceHandler(relativeAngularResources)
              .addResourceLocations(prefix + language.getLanguage() + "/");

      final var fixedAngularResources = Stream.of(ANGULAR_RESOURCES)
                                              .filter(resource -> !StringUtils.contains(resource, "*"))
                                              .map(resource -> "/" + language.getLanguage() + resource)
                                              .toArray(String[]::new);
      registry.addResourceHandler(fixedAngularResources)
              .addResourceLocations(prefix);

      registry.addResourceHandler("/" + language.getLanguage() + "/assets/**")
              .addResourceLocations(prefix + language.getLanguage() + "/assets/");
    };
  }

  @Override
  public void configureViewResolvers(ViewResolverRegistry registry) {
    registry.viewResolver(thymeleafReactiveViewResolver);
  }

  @Bean
  public RouterFunction<ServerResponse> routerFunction() {
    final var routerFunctionBuilder = route().GET("/", this::defaultLandingPage);
    SUPPORTED_LANGUAGES.forEach(addLocalizedLandingPageTo(routerFunctionBuilder));
    return routerFunctionBuilder.build();
  }

  private Mono<ServerResponse> defaultLandingPage(ServerRequest request) {
    final var locale = Optional.ofNullable(Locale.lookup(request.headers().acceptLanguage(), SUPPORTED_LANGUAGES))
                               .orElse(DEFAULT_LANGUAGE);
    return ServerResponse.temporaryRedirect(request.uriBuilder().path(locale.getLanguage()).build()).build();
  }

  private Consumer<Locale> addLocalizedLandingPageTo(RouterFunctions.Builder routerFunctionBuilder) {
    return language -> {
      var requestPredicate = Stream.of(ANGULAR_RESOURCES)
                                   .map(angularResource -> "/" + language.getLanguage() + angularResource)
                                   .reduce(GET("/" + language.getLanguage() + "/**"), (requestPredicte, route) -> requestPredicte.and(GET(route).negate()), RequestPredicate::and);
      requestPredicate = requestPredicate.and(GET("/" + language.getLanguage() + "/assets/**").negate());
      routerFunctionBuilder.route(requestPredicate, request -> ServerResponse.ok().contentType(MediaType.TEXT_HTML).render(language.getLanguage() + "/index"));
    };
  }
}
