package nl.knaw.huygens.alexandria.client;

import nl.knaw.huygens.Log;
import nl.knaw.huygens.alexandria.api.model.AboutEntity;
import nl.knaw.huygens.alexandria.api.model.Annotator;
import nl.knaw.huygens.alexandria.api.model.text.TextImportStatus;
import nl.knaw.huygens.alexandria.api.model.text.TextRangeAnnotation;
import nl.knaw.huygens.alexandria.api.model.text.TextRangeAnnotation.Position;
import nl.knaw.huygens.alexandria.api.model.text.TextRangeAnnotationInfo;
import nl.knaw.huygens.alexandria.api.model.text.view.ElementView.ElementMode;
import nl.knaw.huygens.alexandria.api.model.text.view.ElementViewDefinition;
import nl.knaw.huygens.alexandria.api.model.text.view.TextViewDefinition;
import nl.knaw.huygens.alexandria.client.model.ResourcePrototype;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class OptimisticAlexandriaClientTest extends AlexandriaTestWithTestServer {

  private static final String EVERYTHING_UPTO_AND_INCLUDING_THE_LAST_PERIOD_REGEX = ".*\\.";
  private static OptimisticAlexandriaClient client;

  @BeforeClass
  public static void startClient() {
    client = new OptimisticAlexandriaClient("http://localhost:2016/");
  }

  @AfterClass
  public static void stopClient() {
    client.close();
  }

  @Before
  public void before() {
    client.setAuthKey(AUTHKEY);
    client.setAutoConfirm(true);
  }

  @Test
  public void testAbout() {
    AboutEntity about = client.getAbout();
    assertThat(about.getVersion()).isNotEmpty();
  }

  @Test
  public void testOptimisticAlexandriaClientHasDelegatedUnwrappedMethodForEachRelevantMethodInAlexandriaClient() {
    Class<AlexandriaClient> a = AlexandriaClient.class;

    String stubs = Arrays.stream(a.getMethods())//
        .filter(this::returnsRestResult)//
        .filter(this::hasNoDelegatedMethodInOptimisticAlexandriaClient)//
        .map(this::toDelegatedMethodStub)//
        .collect(joining("\n"));
    Log.info("Methods to add to OptimisticAlexandriaClient:\n{}", stubs);
    assertThat(stubs).isEmpty();
  }

  @Test
  public void testChangingExistingAttributesOnTextRangeAnnotationIsAllowedWhenAnnotatorAndPositionIsTheSame() {
    String xml = singleQuotesToDouble("<text><p xml:id='p-1'>This is a simple paragraph.</p></text>");
    UUID resourceUUID = createResourceWithText(xml);
    client.setAnnotator(resourceUUID, "ed", new Annotator().setCode("ed").setDescription("Eddy Wally"));

    UUID annotationUUID = UUID.randomUUID();
    Map<String, String> attributes1 = new HashMap<>();
    attributes1.put("key1", "value1");
    attributes1.put("key2", "value2");
    Position position = new Position()//
        .setXmlId("p-1");
    TextRangeAnnotation textRangeAnnotation = new TextRangeAnnotation()//
        .setId(annotationUUID)//
        .setName("tag")//
        .setAnnotator("ed")//
        .setPosition(position)//
        .setAttributes(attributes1);
    TextRangeAnnotationInfo info = client.setResourceTextRangeAnnotation(resourceUUID, textRangeAnnotation);
    assertThat(info.getAnnotates()).isEqualTo("This is a simple paragraph.");

    String annotatedXML = client.getTextAsString(resourceUUID);
    String expectation2 = singleQuotesToDouble("<text><p xml:id='p-1'><tag key1='value1' key2='value2' resp='#ed'>This is a simple paragraph.</tag></p></text>");
    assertThat(annotatedXML).isEqualTo(expectation2);

    // now to change the attributes of this annotation
    Map<String, String> attributes2 = new HashMap<>();
    attributes2.put("key1", "something");
    attributes2.put("key2", "entirely");
    textRangeAnnotation.setAttributes(attributes2);
    TextRangeAnnotationInfo info2 = client.setResourceTextRangeAnnotation(resourceUUID, textRangeAnnotation);
    Log.info("{}", info2);

    annotatedXML = client.getTextAsString(resourceUUID);
    String expectation3 = singleQuotesToDouble("<text><p xml:id='p-1'><tag key1='something' key2='entirely' resp='#ed'>This is a simple paragraph.</tag></p></text>");
    assertThat(annotatedXML).isEqualTo(expectation3);
  }

  @Test
  public void testAddingAttributesOnTextRangeAnnotationIsNotAllowed() {
    String xml = singleQuotesToDouble("<text><p xml:id='p-1'>This is a simple paragraph.</p></text>");
    UUID resourceUUID = createResourceWithText(xml);
    client.setAnnotator(resourceUUID, "ed", new Annotator().setCode("ed").setDescription("Eddy Wally"));

    UUID annotationUUID = UUID.randomUUID();
    Map<String, String> attributes1 = new HashMap<>();
    attributes1.put("key1", "value1");
    attributes1.put("key2", "value2");
    Position position = new Position()//
        .setXmlId("p-1");
    TextRangeAnnotation textRangeAnnotation = new TextRangeAnnotation()//
        .setId(annotationUUID)//
        .setName("tag")//
        .setAnnotator("ed")//
        .setPosition(position)//
        .setAttributes(attributes1);
    TextRangeAnnotationInfo info = client.setResourceTextRangeAnnotation(resourceUUID, textRangeAnnotation);
    assertThat(info.getAnnotates()).isEqualTo("This is a simple paragraph.");

    String annotatedXML = client.getTextAsString(resourceUUID);
    String expectation2 = singleQuotesToDouble("<text><p xml:id='p-1'><tag key1='value1' key2='value2' resp='#ed'>This is a simple paragraph.</tag></p></text>");
    assertThat(annotatedXML).isEqualTo(expectation2);

    // now to change the attributes of this annotation
    Map<String, String> attributes2 = new HashMap<>();
    attributes2.put("key1", "something");
    attributes2.put("key3", "entirely");
    textRangeAnnotation.setAttributes(attributes2);
    try {
      TextRangeAnnotationInfo info2 = client.setResourceTextRangeAnnotation(resourceUUID, textRangeAnnotation);
      Log.info("{}", info2);
      fail();
    } catch (AlexandriaException e) {
      assertThat(e.getMessage()).isEqualTo("400: You're only allowed to change existing attributes [key1, key2]");
    }

    annotatedXML = client.getTextAsString(resourceUUID);
    assertThat(annotatedXML).isEqualTo(expectation2);
  }

  @Test
  public void testOverlappingAnnotationsAreNotAllowed() {
    String xml = singleQuotesToDouble("<text><p xml:id='p-1'>This is a simple paragraph.</p></text>");
    UUID resourceUUID = createResourceWithText(xml);
    client.setAnnotator(resourceUUID, "ed", new Annotator().setCode("ed").setDescription("Eddy Wally"));

    UUID annotationUUID = UUID.randomUUID();
    Map<String, String> attributes1 = new HashMap<>();
    attributes1.put("key1", "value1");
    attributes1.put("key2", "value2");
    Position position = new Position()//
        .setXmlId("p-1");
    TextRangeAnnotation textRangeAnnotation = new TextRangeAnnotation()//
        .setId(annotationUUID)//
        .setName("tag")//
        .setAnnotator("ed")//
        .setPosition(position)//
        .setAttributes(attributes1);
    TextRangeAnnotationInfo info = client.setResourceTextRangeAnnotation(resourceUUID, textRangeAnnotation);
    assertThat(info.getAnnotates()).isEqualTo("This is a simple paragraph.");

    String annotatedXML = client.getTextAsString(resourceUUID);
    String expectation2 = singleQuotesToDouble("<text><p xml:id='p-1'><tag key1='value1' key2='value2' resp='#ed'>This is a simple paragraph.</tag></p></text>");
    assertThat(annotatedXML).isEqualTo(expectation2);

    // now try another annotation with the same position, name and annotator
    Map<String, String> attributes2 = new HashMap<>();
    attributes2.put("key1", "something");
    attributes2.put("key2", "different");
    UUID annotationUUID2 = UUID.randomUUID();
    TextRangeAnnotation textRangeAnnotation2 = new TextRangeAnnotation()//
        .setId(annotationUUID2)//
        .setName("tag")//
        .setAnnotator("ed")//
        .setPosition(position)//
        .setAttributes(attributes2);
    try {
      TextRangeAnnotationInfo info2 = client.setResourceTextRangeAnnotation(resourceUUID, textRangeAnnotation2);
      Log.info("{}", info2);
      fail();
    } catch (AlexandriaException e) {
      assertThat(e.getMessage()).isEqualTo("409: Overlapping annotations with the same name and responsibility.");
    }

    annotatedXML = client.getTextAsString(resourceUUID);
    assertThat(annotatedXML).isEqualTo(expectation2);
  }

  @Test
  public void testBugNLA324() {
    String rootXml = singleQuotesToDouble("<text><p xml:id='p-1'>I AM ROOT</p><ignore>this</ignore></text>");
    UUID rootResourceUUID = createResourceWithText(rootXml);

    ElementViewDefinition evd = new ElementViewDefinition()//
        .setElementMode(ElementMode.hide);
    TextViewDefinition textView = new TextViewDefinition()//
        .setDescription("ignore")//
        .setElementViewDefinition("ignore", evd);
    client.setResourceTextView(rootResourceUUID, "view1", textView);

    String rootView = client.getTextAsString(rootResourceUUID, "view1");
    Log.info("rootView = {}", rootView);

    UUID letterUUID = client.addSubResource(rootResourceUUID, "letter01");
    String letterXml = singleQuotesToDouble("<text><p xml:id='p-1'>show this.</p><ignore>ignore this</ignore></text>");
    client.setResourceTextSynchronously(letterUUID, letterXml);

    String letterView = client.getTextAsString(letterUUID, "view1");
    Log.info("letterView = {}", letterView);
    String expected = singleQuotesToDouble("<text><p xml:id='p-1'>show this.</p></text>");
    assertThat(letterView).isEqualTo(expected);

<<<<<<< HEAD
    // now, change the textview
    textView.setElementViewDefinition("p",evd); // hide <p>
    client.setResourceTextView(rootResourceUUID, "view1", textView);
    String letterView2 = client.getTextAsString(letterUUID, "view1");
    Log.info("letterView = {}", letterView2);
    String expected2 = singleQuotesToDouble("<text></text>");
    assertThat(letterView).isEqualTo(expected);

=======
    textView.setElementViewDefinition("p", evd); // hide <p> as well
    client.setResourceTextView(rootResourceUUID, "view1", textView);

    String letterView2 = client.getTextAsString(letterUUID, "view1");
    Log.info("letterView = {}", letterView2);
    String expected2 = singleQuotesToDouble("<text></text>");
    assertThat(letterView2).isEqualTo(expected2);
>>>>>>> develop
  }

  /// end tests

  private UUID createResourceWithText(String xml) {
    String resourceRef = "test";
    UUID resourceUUID = createResource(resourceRef);
    TextImportStatus textGraphImportStatus = setResourceText(resourceUUID, xml);
    URI expectedURI = URI.create("http://localhost:2016/resources/" + resourceUUID + "/text/xml");
    assertThat(textGraphImportStatus.getTextURI()).isEqualTo(expectedURI);
    return resourceUUID;
  }

  protected UUID createResource(String resourceRef) {
    ResourcePrototype resource = new ResourcePrototype().setRef(resourceRef);
    UUID resourceUuid = UUID.randomUUID();
    client.setResource(resourceUuid, resource);
    return resourceUuid;
  }

  boolean returnsRestResult(Method method) {
    return method.getReturnType().equals(RestResult.class);
  }

  boolean hasNoDelegatedMethodInOptimisticAlexandriaClient(Method method) {
    Class<OptimisticAlexandriaClient> o = OptimisticAlexandriaClient.class;
    try {
      Method oMethod = o.getMethod(method.getName(), method.getParameterTypes());
      Type type = actualReturnType(method);
      boolean equals = type.equals(Void.class)//
          ? oMethod.getReturnType().equals(Void.TYPE)//
          : oMethod.getReturnType().equals(type);
      return !equals;
    } catch (Exception e) {
      return true;
    }
  }

  private Type actualReturnType(Method method) {
    Type genericReturnType = method.getGenericReturnType();
    return ((ParameterizedType) genericReturnType).getActualTypeArguments()[0];
  }

  String toDelegatedMethodStub(Method method) {
    String returnType = actualReturnType(method).getTypeName().replaceFirst(EVERYTHING_UPTO_AND_INCLUDING_THE_LAST_PERIOD_REGEX, "").replace("Void", "void");
    String methodName = method.getName();
    String qualifiedParameters = Arrays.stream(method.getParameters())//
        .map(this::toQualifiedParameter)//
        .collect(joining(", "));
    String returnStatement = "void".equals(returnType) ? "" : "return ";
    String parameters = Arrays.stream(method.getParameters())//
        .map(this::parameterName)//
        .collect(joining(", "));

    return MessageFormat.format(//
        "public {0} {1}({2}) '{' {3}unwrap(delegate.{4}({5}));'}'", //
        returnType, //
        methodName, //
        qualifiedParameters, //
        returnStatement, //
        methodName, //
        parameters//
    );
  }

  String toQualifiedParameter(Parameter parameter) {
    return typeString(parameter) + " " + parameterName(parameter);
  }

  private String typeString(Parameter parameter) {
    return parameter.getType().getName().replaceFirst(EVERYTHING_UPTO_AND_INCLUDING_THE_LAST_PERIOD_REGEX, "");
  }

  String parameterName(Parameter parameter) {
    return typeString(parameter).toLowerCase();
  }

  protected TextImportStatus setResourceText(UUID resourceUuid, String xml) {
    return client.setResourceTextSynchronously(resourceUuid, xml);
  }

}
