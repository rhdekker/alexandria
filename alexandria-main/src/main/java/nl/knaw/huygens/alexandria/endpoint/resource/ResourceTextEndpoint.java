package nl.knaw.huygens.alexandria.endpoint.resource;

import java.util.UUID;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.annotations.ApiOperation;
import nl.knaw.huygens.alexandria.endpoint.JSONEndpoint;
import nl.knaw.huygens.alexandria.endpoint.LocationBuilder;
import nl.knaw.huygens.alexandria.endpoint.UUIDParam;
import nl.knaw.huygens.alexandria.model.AlexandriaResource;
import nl.knaw.huygens.alexandria.service.AlexandriaService;

public class ResourceTextEndpoint extends JSONEndpoint {
  private ResourceCreationRequestBuilder requestBuilder;
  private LocationBuilder locationBuilder;
  private AlexandriaService service;
  private AlexandriaResource resource;
  private UUID resourceUUID;

  @Inject
  public ResourceTextEndpoint(AlexandriaService service, //
      ResourceCreationRequestBuilder requestBuilder, //
      LocationBuilder locationBuilder, //
      @PathParam("uuid") final UUIDParam uuidParam) {
    this.service = service;
    this.requestBuilder = requestBuilder;
    this.locationBuilder = locationBuilder;
    resourceUUID = uuidParam.getValue();
    resource = service.readResource(resourceUUID)//
        .orElseThrow(ResourcesEndpoint.resourceNotFoundForId(resourceUUID));
    if (resource.isTentative()) {
      throw ResourcesEndpoint.resourceIsTentativeException(resourceUUID);
    }
  }

  @GET
  @Produces(MediaType.TEXT_XML)
  @ApiOperation("get text as xml")
  public Response getXMLText() {
    String text = getResourceText();
    return Response.ok(text).build();
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  @ApiOperation("get text as plain text")
  public Response getPlainText() {
    String text = getResourceText();
    return Response.ok(text).build();
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation("set text from text protoype")
  public Response setTextWithPrototype(@NotNull @Valid TextPrototype prototype) {
    String body = prototype.getBody();
    service.setResourceText(resourceUUID, body);
    return Response.ok(body).build();
  }

  @PUT
  @Consumes(MediaType.TEXT_XML)
  @ApiOperation("set text from xml")
  public Response setTextFromXml(@NotNull @Valid String xml) {
    service.setResourceText(resourceUUID, xml);
    return Response.ok(xml).build();
  }

  private String getResourceText() {
    String text = service.getResourceText(resourceUUID)//
        .orElseThrow(() -> new NotFoundException());
    return text;
  }

}
