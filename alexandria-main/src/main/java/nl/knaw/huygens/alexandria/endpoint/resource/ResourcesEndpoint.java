package nl.knaw.huygens.alexandria.endpoint.resource;

import static nl.knaw.huygens.alexandria.endpoint.EndpointPaths.RESOURCES;

import java.net.URI;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import nl.knaw.huygens.Log;
import nl.knaw.huygens.alexandria.config.AlexandriaConfiguration;
import nl.knaw.huygens.alexandria.endpoint.JSONEndpoint;
import nl.knaw.huygens.alexandria.endpoint.LocationBuilder;
import nl.knaw.huygens.alexandria.endpoint.UUIDParam;
import nl.knaw.huygens.alexandria.model.AlexandriaResource;
import nl.knaw.huygens.alexandria.service.AlexandriaService;

@Path(RESOURCES)
public class ResourcesEndpoint extends JSONEndpoint {

  private static final URI HERE = URI.create("");

  private final AlexandriaService alexandriaService;
  private final ResourceEntityBuilder entityBuilder;
  private final ResourceCreationRequestBuilder requestBuilder;
  private final AlexandriaConfiguration configuration;
  private final LocationBuilder locationBuilder;

  @Inject
  public ResourcesEndpoint(AlexandriaService service, //
      AlexandriaConfiguration configuration,//
      ResourceCreationRequestBuilder requestBuilder, //
      LocationBuilder locationBuilder, //
      ResourceEntityBuilder entityBuilder) {
    this.configuration = configuration;
    this.locationBuilder = locationBuilder;
    Log.trace("Resources created, alexandriaService=[{}]", service);

    this.alexandriaService = service;
    this.entityBuilder = entityBuilder;
    this.requestBuilder = requestBuilder;
  }

  @GET
  @Path("{uuid}")
  public Response getResourceByID(@PathParam("uuid") final UUIDParam uuid) {
    final AlexandriaResource resource = alexandriaService.readResource(uuid.getValue());
    return Response.ok(entityBuilder.build(resource)).build();
  }

  @POST
  public Response createResource(@NotNull @Valid @WithoutId ResourcePrototype protoType) {
    Log.trace("protoType=[{}]", protoType);

    final ResourceCreationRequest request = requestBuilder.build(protoType);
    AlexandriaResource resource = request.execute(alexandriaService);

    if (request.wasExecutedAsIs()) {
      return Response.noContent().build();
    }

    // final ResourceEntity entity = entityBuilder.build(resource);
    return Response.created(locationBuilder.locationOf(resource)).build();
  }

  @PUT
  @Path("{uuid}")
  public Response setResourceAtSpecificID(@NotNull @Valid @MatchesPathId ResourcePrototype protoType) {
    Log.trace("protoType=[{}]", protoType);

    final ResourceCreationRequest request = requestBuilder.build(protoType);
    request.execute(alexandriaService);

    if (request.newResourceWasCreated()) {
      return Response.created(HERE).build();
    }

    if (request.wasExecutedAsIs()) {
      return Response.noContent().build();
    }

    return Response.ok().build();
  }

  @DELETE
  @Path("{uuid}")
  public Response deleteNotSupported(@PathParam("uuid") final UUIDParam paramId) {
    return methodNotImplemented();
  }

  // TODO: replace with sub-resource analogous to {uuid}/annotations (see below)
  @GET
  @Path("{uuid}/ref")
  public Response getResourceRef(@PathParam("uuid") final UUIDParam uuidParam) {
    AlexandriaResource resource = alexandriaService.readResource(uuidParam.getValue());
    return Response.ok(new RefEntity(resource.getRef())).build();
  }

  // Sub-resource delegation

  @Path("{uuid}/subresources")
  public Class<SubResources> getSubResources() {
    return SubResources.class; // no instantiation of our own; let Jersey handle the lifecycle
  }

  @Path("{uuid}/annotations")
  public Class<ResourceAnnotations> getAnnotations() {
    return ResourceAnnotations.class; // no instantiation of our own; let Jersey handle the lifecycle
  }

}
