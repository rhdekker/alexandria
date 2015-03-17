package nl.knaw.huygens.alexandria.external;

import static org.mockito.Mockito.mock;

import nl.knaw.huygens.alexandria.endpoint.Resources;
import nl.knaw.huygens.alexandria.helpers.ApiFixture;
import nl.knaw.huygens.alexandria.service.ResourceService;
import org.junit.BeforeClass;
import org.mockito.Mockito;

public class ResourcesFixture extends ApiFixture {
  private static ResourceService RESOURCE_SERVICE_MOCK = mock(ResourceService.class);

  @Override
  public void clear() {
    super.clear();
    Mockito.reset(RESOURCE_SERVICE_MOCK);
  }

  @BeforeClass
  public static void setup() {
    addClass(Resources.class);
    addProviderForContext(ResourceService.class, RESOURCE_SERVICE_MOCK);
  }

  protected ResourceService resourceService() {
    return RESOURCE_SERVICE_MOCK;
  }
}