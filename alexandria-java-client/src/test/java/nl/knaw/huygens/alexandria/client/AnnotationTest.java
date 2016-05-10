package nl.knaw.huygens.alexandria.client;

import java.util.UUID;

import org.junit.Test;

import nl.knaw.huygens.alexandria.client.model.AnnotationPojo;
import nl.knaw.huygens.alexandria.client.model.ResourcePojo;

public class AnnotationTest extends AlexandriaClientTest {
  @Test
  public void testAnnotatingAResource() {
    client.setAuthKey(AUTHKEY);
    client.setAutoConfirm(false);
    String resourceRef = "resource";
    ResourcePojo resource = new ResourcePojo(resourceRef);
    UUID resourceUuid = UUID.randomUUID();
    RestResult<Void> result = client.setResource(resourceUuid, resource);
    assertRequestSucceeded(result);

    // annotate the resource
    String annotationType = "userRemark";
    String annotationValue = "WTF?";
    AnnotationPojo annotationPrototype = new AnnotationPojo()//
        .setType(annotationType)//
        .setValue(annotationValue);
    RestResult<UUID> result2 = client.annotateResource(resourceUuid, annotationPrototype);
    assertRequestSucceeded(result2);
    UUID annotationUuid = result2.get();

    RestResult<AnnotationPojo> result3 = client.getAnnotation(annotationUuid);
    assertRequestSucceeded(result3);
    AnnotationPojo AnnotationPojo = result3.get();

    softly.assertThat(AnnotationPojo).isNotNull();
    softly.assertThat(AnnotationPojo.getType()).as("type").isEqualTo(annotationType);
    softly.assertThat(AnnotationPojo.getValue()).as("value").isEqualTo(annotationValue);
  }

}
