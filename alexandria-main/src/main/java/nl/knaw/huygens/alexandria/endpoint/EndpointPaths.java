package nl.knaw.huygens.alexandria.endpoint;

public final class EndpointPaths {
  public static final String ANNOTATIONS = "annotations";
  public static final String RESOURCES = "resources";

  private EndpointPaths() {
    throw new AssertionError("Paths shall not be instantiated");
  }
}