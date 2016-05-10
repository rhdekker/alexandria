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

import static nl.knaw.huygens.alexandria.api.ApiConstants.HEADER_AUTH;

import java.net.URI;
import java.util.UUID;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import nl.knaw.huygens.Log;
import nl.knaw.huygens.alexandria.api.EndpointPaths;
import nl.knaw.huygens.alexandria.api.model.AboutEntity;
import nl.knaw.huygens.alexandria.api.model.AlexandriaState;
import nl.knaw.huygens.alexandria.api.model.StatePrototype;
import nl.knaw.huygens.alexandria.api.model.TextView;
import nl.knaw.huygens.alexandria.api.model.TextViewDefinition;
import nl.knaw.huygens.alexandria.api.model.TextViewPrototype;
import nl.knaw.huygens.alexandria.client.model.AnnotationPojo;
import nl.knaw.huygens.alexandria.client.model.ResourcePojo;

public class AlexandriaClient {
  private WebTarget rootTarget;
  private String authHeader = "";
  private Client client;
  private URI alexandriaURI;
  private boolean autoConfirm = true;

  public AlexandriaClient(URI alexandriaURI) {
    this.alexandriaURI = alexandriaURI;
    final ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());
    final JacksonJaxbJsonProvider jacksonProvider = new JacksonJaxbJsonProvider();
    jacksonProvider.setMapper(objectMapper);
    client = ClientBuilder.newClient(new ClientConfig(jacksonProvider));
    client.property(ClientProperties.CONNECT_TIMEOUT, 60000);
    client.property(ClientProperties.READ_TIMEOUT, 60000);
    rootTarget = client.target(alexandriaURI);
  }

  public void setProperty(String jerseyClientProperty, Object value) {
    client.property(jerseyClientProperty, value);
    rootTarget = client.target(alexandriaURI);
  }

  public void setAuthKey(String authKey) {
    authHeader = "SimpleAuth " + authKey;
    Log.info("authheader=[{}]", authHeader);
  }

  public void setAutoConfirm(boolean autoConfirm) {
    this.autoConfirm = autoConfirm;
  }

  public RestResult<AboutEntity> getAbout() {
    RestRequester<AboutEntity> requester = RestRequester.withResponseSupplier(() -> rootTarget.path(EndpointPaths.ABOUT).request().get());
    return requester//
        .onStatus(Status.OK, (response) -> {
          RestResult<AboutEntity> result = new RestResult<>();
          AboutEntity cargo = response.readEntity(AboutEntity.class);
          result.setCargo(cargo);
          return result;
        })//
        .getResult();
  }

  public RestResult<Void> setResource(UUID resourceId, ResourcePojo resource) {
    Entity<ResourcePojo> entity = Entity.json(resource);
    Supplier<Response> responseSupplier = () -> rootTarget//
        .path(EndpointPaths.RESOURCES)//
        .path(resourceId.toString())//
        .request()//
        .header(HEADER_AUTH, authHeader)//
        .put(entity);
    RestRequester<Void> requester = RestRequester.withResponseSupplier(responseSupplier);

    return requester//
        .onStatus(Status.CREATED, (response) -> new RestResult<>())//
        .getResult();
  }

  public RestResult<UUID> addResource(ResourcePojo resource) {
    Entity<ResourcePojo> entity = Entity.json(resource);
    Supplier<Response> responseSupplier = () -> rootTarget//
        .path(EndpointPaths.RESOURCES)//
        .request()//
        .header(HEADER_AUTH, authHeader)//
        .post(entity);
    RestRequester<UUID> requester = RestRequester.withResponseSupplier(responseSupplier);
    RestResult<UUID> addResult = requester//
        .onStatus(Status.CREATED, this::uuidFromLocationHeader)//
        .getResult();
    if (autoConfirm && !addResult.hasFailed()) {
      confirmResource(addResult.get());
    }

    return addResult;
  }

  public RestResult<ResourcePojo> getResource(UUID uuid) {
    RestRequester<ResourcePojo> requester = RestRequester.withResponseSupplier(//
        () -> rootTarget.path(EndpointPaths.RESOURCES)//
            .path(uuid.toString())//
            .request()//
            .get()//
    );
    return requester//
        .onStatus(Status.OK, this::toResourcePojoRestResult)//
        .getResult();
  }

  public RestResult<Void> confirmResource(UUID resourceUuid) {
    return confirm(EndpointPaths.RESOURCES, resourceUuid);
  }

  public RestResult<Void> confirmAnnotation(UUID annotationUuid) {
    return confirm(EndpointPaths.ANNOTATIONS, annotationUuid);
  }

  public RestResult<URI> addTextView(UUID resourceUUID, TextViewPrototype textView) {
    Entity<TextViewPrototype> entity = Entity.json(textView);
    Supplier<Response> responseSupplier = () -> rootTarget.path(EndpointPaths.RESOURCES)//
        .path(resourceUUID.toString())//
        .path(EndpointPaths.TEXT)//
        .path(EndpointPaths.TEXTVIEWS)//
        .request()//
        .header(HEADER_AUTH, authHeader)//
        .put(entity);
    RestRequester<URI> requester = RestRequester.withResponseSupplier(responseSupplier);
    return requester//
        .onStatus(Status.CREATED, this::uriFromLocationHeader)//
        .getResult();
  }

  public RestResult<TextView> getTextView(UUID uuid) {
    RestRequester<TextView> requester = RestRequester.withResponseSupplier(() -> rootTarget.path(EndpointPaths.RESOURCES)//
        .path(uuid.toString())//
        .path(EndpointPaths.TEXT)//
        .path(EndpointPaths.TEXTVIEWS)//
        .request().get());
    return requester//
        .onStatus(Status.OK, (response) -> {
          RestResult<TextView> result = new RestResult<>();
          TextView cargo = response.readEntity(TextView.class);
          result.setCargo(cargo);
          return result;
        })//
        .getResult();
  }

  public RestResult<ResourcePojo> setResourceTextView(UUID resourceUuid, String textViewName, TextViewDefinition textView) {
    // TODO Auto-generated method stub
    return null;
  }

  public RestResult<UUID> annotateResource(UUID resourceUuid, AnnotationPojo annotationPrototype) {
    Entity<AnnotationPojo> entity = Entity.json(annotationPrototype);
    Supplier<Response> responseSupplier = () -> rootTarget//
        .path(EndpointPaths.RESOURCES)//
        .path(resourceUuid.toString())//
        .path(EndpointPaths.ANNOTATIONS).request()//
        .header(HEADER_AUTH, authHeader)//
        .post(entity);
    RestRequester<UUID> requester = RestRequester.withResponseSupplier(responseSupplier);
    RestResult<UUID> annotateResult = requester//
        .onStatus(Status.CREATED, this::uuidFromLocationHeader)//
        .getResult();
    if (autoConfirm && !annotateResult.hasFailed()) {
      confirmAnnotation(annotateResult.get());
    }
    return annotateResult;
  }

  public RestResult<AnnotationPojo> getAnnotation(UUID uuid) {
    RestRequester<AnnotationPojo> requester = RestRequester.withResponseSupplier(() -> rootTarget.path(EndpointPaths.ANNOTATIONS).path(uuid.toString()).request().get());
    return requester//
        .onStatus(Status.OK, this::toAnnotationPojoRestResult)//
        .getResult();
  }

  // private methods

  private RestResult<Void> confirm(String endpoint, UUID resourceUuid) {
    StatePrototype state = new StatePrototype().setState(AlexandriaState.CONFIRMED);
    Entity<StatePrototype> confirmation = Entity.json(state);
    Supplier<Response> responseSupplier = () -> {
      return rootTarget.path(endpoint)//
          .path(resourceUuid.toString())//
          .path("state")//
          .request()//
          .header(HEADER_AUTH, authHeader)//
          .put(confirmation);
    };
    RestRequester<Void> requester = RestRequester.withResponseSupplier(responseSupplier);
    return requester//
        .onStatus(Status.NO_CONTENT, (response) -> new RestResult<>())//
        .getResult();
  }

  private RestResult<ResourcePojo> toResourcePojoRestResult(Response response) {
    return toEntityRestResult(response, ResourcePojo.class);
  }

  private RestResult<AnnotationPojo> toAnnotationPojoRestResult(Response response) {
    return toEntityRestResult(response, AnnotationPojo.class);
  }

  private <E> RestResult<E> toEntityRestResult(Response response, Class<E> entityClass) {
    RestResult<E> result = new RestResult<>();
    E cargo = response.readEntity(entityClass);
    result.setCargo(cargo);
    return result;
  }

  private RestResult<URI> uriFromLocationHeader(Response response) {
    RestResult<URI> result = new RestResult<>();
    String location = response.getHeaderString("Location");
    URI uri = URI.create(location);
    result.setCargo(uri);
    return result;
  }

  private RestResult<UUID> uuidFromLocationHeader(Response response) {
    RestResult<UUID> result = new RestResult<>();
    String location = response.getHeaderString("Location");
    UUID uuid = UUID.fromString(location.replaceFirst(".*/", ""));
    result.setCargo(uuid);
    return result;
  }
}
