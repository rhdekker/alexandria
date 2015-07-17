package nl.knaw.huygens.concordion;

import static java.lang.String.format;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Scopes;
import nl.knaw.huygens.Log;
import nl.knaw.huygens.alexandria.config.AlexandriaConfiguration;
import nl.knaw.huygens.alexandria.endpoint.EndpointPathResolver;
import nl.knaw.huygens.alexandria.endpoint.annotation.AnnotationEntityBuilder;
import nl.knaw.huygens.alexandria.endpoint.resource.ResourceEntityBuilder;
import nl.knaw.huygens.alexandria.service.AlexandriaService;
import nl.knaw.huygens.alexandria.service.TinkerpopAlexandriaService;
import nl.knaw.huygens.alexandria.storage.Storage;
import nl.knaw.huygens.alexandria.util.UUIDParser;
import nl.knaw.huygens.cat.RestFixture;
import org.junit.BeforeClass;

public class AlexandriaFixture extends RestFixture {
  private static final AlexandriaConfiguration CONFIG = testConfiguration();

  private static TinkerpopAlexandriaService service = new TinkerpopAlexandriaService().withStorage(new Storage());

  @BeforeClass
  public static void setupAlexandriaFixture() {
    Log.debug("Setting up AlexandriaFixture");
    setupRestFixture(alexandriaModule());
    register(JsonConfiguration.class);
  }

  private static AlexandriaConfiguration testConfiguration() {
    return new AlexandriaConfiguration() {
      @Override
      public URI getBaseURI() {
        return UriBuilder.fromUri("https://localhost/").port(4242).build();
      }

      @Override
      public String getStorageDirectory() {
        return "/tmp";
      }

      @Override
      public String toString() {
        return Objects.toStringHelper(this).add("baseURI", getBaseURI()).toString();
      }
    };
  }

  private static Module alexandriaModule() {
    return new AbstractModule() {
      @Override
      protected void configure() {
        Log.trace("setting up Guice bindings");
        bind(AlexandriaService.class).toInstance(service);
        bind(AlexandriaConfiguration.class).toInstance(CONFIG);
        bind(AnnotationEntityBuilder.class).in(Scopes.SINGLETON);
        bind(EndpointPathResolver.class).in(Scopes.SINGLETON);
        bind(ResourceEntityBuilder.class).in(Scopes.SINGLETON);
      }
    };
  }

  public void clear() {
    Log.debug("Clearing {}", getClass().getSimpleName());
    super.clear();
  }

  public void clearStorage() {
    Log.debug("Clearing Storage");
    service.withStorage(new Storage());
  }

  @Override
  protected Application configure() {
    return super.configure(); // maybe move enabling of LOG_TRAFFIC and DUMP_ENTITY from RestFixture here?
  }

  protected AlexandriaService service() {
    return service;
  }

  private Optional<UUID> parse(String idStr) {
    return UUIDParser.fromString(idStr).get();
  }

  private Supplier<String> malformedDescription(String idStr) {
    return () -> "malformed UUID: " + idStr;
  }

  private String normalizeHostInfo(String s) {
    return s.replaceAll(hostInfo(), "{host}");
  }

  private String hostInfo() {
    final URI baseURI = getBaseUri();
    return format("%s:%d", baseURI.getHost(), baseURI.getPort());
  }

  private String baseOf(String s) {
    return s.substring(0, s.lastIndexOf('/') + 1);
  }

  private String tailOf(String s) {
    return Iterables.getLast(Splitter.on('/').split(s));
  }
}