package nl.knaw.huygens.alexandria.client;

/*
 * #%L
 * alexandria-java-client
 * =======
 * Copyright (C) 2015 - 2016 Huygens ING (KNAW)
 * =======
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Lists;

import nl.knaw.huygens.Log;
import nl.knaw.huygens.alexandria.api.EndpointPaths;
import nl.knaw.huygens.alexandria.api.model.AlexandriaState;
import nl.knaw.huygens.alexandria.api.model.ElementDefinition;
import nl.knaw.huygens.alexandria.api.model.TextView;
import nl.knaw.huygens.alexandria.api.model.TextViewPrototype;
import nl.knaw.huygens.alexandria.client.model.ProvenancePojo;
import nl.knaw.huygens.alexandria.client.model.ResourcePojo;

public class ResourceTest extends AlexandriaClientTest {
  @Test
  public void testAddResourceReturnsValidUUID() {
    client.setAuthKey(AUTHKEY);
    client.setAutoConfirm(false);
    String resourceRef = "corpus";
    ResourcePojo resource = new ResourcePojo(resourceRef);
    RestResult<UUID> result = client.addResource(resource);
    assertRequestSucceeded(result);
    UUID resourceUuid = result.get();
    Log.info("resourceUUID = {}", resourceUuid);
    softly.assertThat(resourceUuid).isNotNull();

    // retrieve the resource
    RestResult<ResourcePojo> result2 = client.getResource(resourceUuid);
    assertRequestSucceeded(result2);
    ResourcePojo ResourcePojo = result2.get();
    softly.assertThat(ResourcePojo).isNotNull();
    softly.assertThat(ResourcePojo.getRef()).as("ref").isEqualTo(resourceRef);
    softly.assertThat(ResourcePojo.getState().getValue()).as("state").isEqualTo(AlexandriaState.TENTATIVE);

    // confirm the resource
    RestResult<Void> result3 = client.confirmResource(resourceUuid);
    assertRequestSucceeded(result3);

    // retrieve the resource again
    RestResult<ResourcePojo> result4 = client.getResource(resourceUuid);
    assertRequestSucceeded(result4);
    ResourcePojo ResourcePojo2 = result4.get();
    softly.assertThat(ResourcePojo2).isNotNull();
    softly.assertThat(ResourcePojo2.getRef()).as("ref").isEqualTo(resourceRef);
    softly.assertThat(ResourcePojo2.getState().getValue()).as("state").isEqualTo(AlexandriaState.CONFIRMED);
  }

  @Test
  public void testAddResourceWithProvenanceReturnsValidUUID() {
    client.setAuthKey(AUTHKEY);
    ResourcePojo resource = new ResourcePojo("corpus2").withProvenance(new ProvenancePojo().setWho("test").setWhy("because test"));
    RestResult<UUID> result = client.addResource(resource);
    assertRequestSucceeded(result);
    UUID resourceUuid = result.get();
    Log.info("resourceUUID = {}", resourceUuid);
    assertThat(resourceUuid).isNotNull();
  }

  @Test
  public void testSetResourceWithProvidedUUID() {
    client.setAuthKey(AUTHKEY);
    String ref = "resource3";
    ResourcePojo resource = new ResourcePojo(ref).withProvenance(new ProvenancePojo().setWho("test3").setWhy("because test3"));
    UUID resourceId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    RestResult<Void> result = client.setResource(resourceId, resource);
    assertRequestSucceeded(result);

    // retrieve the resource
    RestResult<ResourcePojo> result2 = client.getResource(resourceId);
    assertRequestSucceeded(result2);
    ResourcePojo ResourcePojo = result2.get();
    softly.assertThat(ResourcePojo).as("entity != null").isNotNull();
    softly.assertThat(ResourcePojo.getRef()).as("ref").isEqualTo(ref);
    softly.assertThat(ResourcePojo.getState().getValue()).as("state").isEqualTo(AlexandriaState.CONFIRMED);
  }

  @Ignore
  @Test
  public void testSetAndRetrieveTextView() {
    // first, create a resource
    client.setAuthKey(AUTHKEY);
    String ref = "corpus";
    ResourcePojo resource = new ResourcePojo(ref).withProvenance(new ProvenancePojo().setWho("test").setWhy("because test"));
    UUID resourceId = UUID.randomUUID();
    RestResult<Void> result = client.setResource(resourceId, resource);
    assertRequestSucceeded(result);

    // then, set the base layer definition
    ElementDefinition baseElement1 = ElementDefinition.withName("body").withAttributes("lang");
    ElementDefinition baseElement2 = ElementDefinition.withName("p").withAttributes("n");
    List<ElementDefinition> list = Lists.newArrayList(baseElement1, baseElement2);
    TextViewPrototype textView = new TextViewPrototype().setIncludedElements(list).setIgnoredElements(Lists.newArrayList("note"));
    RestResult<URI> result2 = client.addTextView(resourceId, textView);
    assertRequestSucceeded(result2);
    softly.assertThat(result2.get().toString()).as("Location").endsWith("/" + resourceId + "/" + EndpointPaths.TEXT + "/" + EndpointPaths.TEXTVIEWS);

    // now, retrieve the base layer definition
    RestResult<TextView> result3 = client.getTextView(resourceId);
    assertRequestSucceeded(result3);
    TextView returnedDefinition = result3.get();
    // softly.assertThat(returnedDefinition.getIgnoredElements()).as("SubresourceElements").containsExactly("note");
    // softly.assertThat(returnedDefinition.getIncludedElementDefinitions()).as("BaseElements").containsExactly(baseElement1, baseElement2);
  }

}
