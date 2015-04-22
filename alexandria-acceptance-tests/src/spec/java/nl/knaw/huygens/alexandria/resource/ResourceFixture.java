package nl.knaw.huygens.alexandria.resource;

import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import nl.knaw.huygens.alexandria.endpoint.resource.ResourceCreationCommandBuilder;
import nl.knaw.huygens.alexandria.helpers.ApiFixture;
import nl.knaw.huygens.alexandria.service.ResourceService;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(ConcordionRunner.class)
public class ResourceFixture extends ApiFixture {
  private static final Logger LOG = LoggerFactory.getLogger(ResourceFixture.class);

  private static final ResourceService RESOURCE_SERVICE_MOCK = mock(ResourceService.class);

  @BeforeClass
  public static void setup() {
    ApiFixture.setupJerseyAndGuice(resourceModule());
  }

  private static Module resourceModule() {
    return new AbstractModule() {
      @Override
      protected void configure() {
        LOG.trace("setting up Guice bindings");
        bind(ResourceService.class).toInstance(RESOURCE_SERVICE_MOCK);
        bind(ResourceCreationCommandBuilder.class).toInstance(new ResourceCreationCommandBuilder());
      }
    };
  }

  @Override
  public void clear() {
    LOG.trace("clear");
    super.clear();
    Mockito.reset(RESOURCE_SERVICE_MOCK);
  }

  protected ResourceService resourceService() {
    return RESOURCE_SERVICE_MOCK;
  }
}