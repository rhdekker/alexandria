package nl.knaw.huygens.alexandria.endpoint.command;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.io.output.WriterOutputStream;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import nl.knaw.huygens.alexandria.api.model.CommandResponse;
import nl.knaw.huygens.alexandria.api.model.Commands;
import nl.knaw.huygens.alexandria.endpoint.command.XpathCommand.XPathResult.Type;
import nl.knaw.huygens.alexandria.service.AlexandriaService;
import nl.knaw.huygens.alexandria.textgraph.TextGraphUtil;
import nl.knaw.huygens.tei.QueryableDocument;

public class XpathCommand extends ResourcesCommand {
  private static final String PARAMETER_XPATH = "xpath";

  private static class Parameters {
    List<ResourceViewId> resourceViewIds;
    String xpath;
  }

  private CommandResponse commandResponse = new CommandResponse();
  private AlexandriaService service;

  @Inject
  public XpathCommand(AlexandriaService service) {
    this.service = service;
  }

  @Override
  public String getName() {
    return Commands.XPATH;
  }

  @Override
  public CommandResponse runWith(Map<String, Object> parameterMap) {
    Parameters parameters = validateParameters(parameterMap);
    Map<String, XPathResult> resultMap = new HashMap<>();
    if (commandResponse.parametersAreValid()) {
      for (ResourceViewId resourceViewId : parameters.resourceViewIds) {
        service.runInTransaction(() -> {
          UUID resourceId = resourceViewId.getResourceId();
          String viewName = resourceViewId.getTextViewName().orElse("");
          StreamingOutput xmlOutputStream = TextGraphUtil.xmlOutputStream(service, resourceId, viewName);
          StringBuilderWriter sbWriter = new StringBuilderWriter();
          WriterOutputStream writerOutputStream = new WriterOutputStream(sbWriter);
          try {
            xmlOutputStream.write(writerOutputStream);
            writerOutputStream.close();
            String xml = sbWriter.toString();
            XPathResult result = testXPath(parameters.xpath, xml);
            resultMap.put(resourceViewId.toString(), result);
          } catch (WebApplicationException | IOException | XPathExpressionException e) {
            commandResponse.addErrorLine(resourceId + ": " + e.getMessage());
            e.printStackTrace();
          }
        });
      }
    }
    commandResponse.setResult(resultMap);
    return commandResponse;
  }

  static XPathResult testXPath(String xpathQuery, String xml) throws XPathExpressionException {
    QueryableDocument qDoc = QueryableDocument.createFromXml(xml, true);

    try {
      NodeList nodelist = qDoc.evaluateXPathToW3CNodeList(xpathQuery);
      return new XPathResult(Type.NODESET, toStringList(nodelist));
    } catch (XPathException e) {
      if (e.getMessage().contains("#BOOLEAN")) {
        Object b = qDoc.evaluateXPathToBoolean(xpathQuery);
        return new XPathResult(Type.BOOLEAN, b);
      }
      if (e.getMessage().contains("#NUMBER")) {
        Object n = qDoc.evaluateXPathToDouble(xpathQuery);
        return new XPathResult(Type.NUMBER, n);
      }
    }

    String string = qDoc.evaluateXPathToString(xpathQuery);
    return new XPathResult(Type.STRING, string);
  }

  private static List<String> toStringList(NodeList nodelist) {
    List<String> list = new ArrayList<>(nodelist.getLength());
    for (int i = 0; i < nodelist.getLength(); i++) {
      Node node = nodelist.item(i);
      list.add(toString(node));
    }
    return list;
  }

  public static String toString(Node node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null.");
    }

    try {
      // Remove unwanted whitespaces
      XPath xpath = XPathFactory.newInstance().newXPath();
      XPathExpression expr = xpath.compile("//text()[normalize-space()='']");
      NodeList nodeList = (NodeList) expr.evaluate(node, XPathConstants.NODESET);

      for (int i = 0; i < nodeList.getLength(); ++i) {
        Node nd = nodeList.item(i);
        nd.getParentNode().removeChild(nd);
      }

      // Create and setup transformer
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

      // transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      // transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

      // Turn the node into a string
      StringWriter writer = new StringWriter();
      transformer.transform(new DOMSource(node), new StreamResult(writer));
      return writer.toString();
    } catch (TransformerException | XPathExpressionException e) {
      throw new RuntimeException(e);
    }
  }

  private Parameters validateParameters(Map<String, Object> parameterMap) {
    final Parameters parameters = new Parameters();
    parameters.resourceViewIds = validateResourceViewIds(parameterMap, commandResponse, service);
    boolean valid = (commandResponse.getErrorLines().isEmpty());
    if (!parameterMap.containsKey(PARAMETER_XPATH)) {
      addXPathError();
      valid = false;

    } else {
      try {
        parameters.xpath = (String) parameterMap.get(PARAMETER_XPATH);
      } catch (ClassCastException e) {
        addXPathError();
        valid = false;
      }
    }

    if (valid) {
      commandResponse.setParametersAreValid(true);
    }
    return parameters;
  }

  private void addXPathError() {
    commandResponse.addErrorLine("Parameter '" + PARAMETER_XPATH + "' should be a valid xpath query.");
  }

  public static class XPathResult {
    public enum Type {
      BOOLEAN, NUMBER, STRING, NODESET
    }

    Object result;
    Type type;

    public XPathResult(Type type, Object result) {
      this.type = type;
      this.result = result;
    }

    public Object getResult() {
      return result;
    }

    public Type getType() {
      return type;
    }
  }
}