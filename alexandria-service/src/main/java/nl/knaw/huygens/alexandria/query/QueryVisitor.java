package nl.knaw.huygens.alexandria.query;

import static java.util.stream.Collectors.toList;

import java.util.List;

import com.google.common.collect.Lists;

import nl.knaw.huygens.alexandria.antlr.AQLBaseVisitor;
import nl.knaw.huygens.alexandria.antlr.AQLParser.ParameterContext;
import nl.knaw.huygens.alexandria.antlr.AQLParser.SubqueryContext;

public class QueryVisitor extends AQLBaseVisitor<Void> {
  private List<WhereToken> whereTokens = Lists.newArrayList();

  @Override
  public Void visitSubquery(SubqueryContext ctx) {
    List<Object> parameters = ctx.parameters().parameter().stream()//
        .map(ParameterContext::getText)//
        .map(QueryVisitor::parseParameterString)//
        .collect(toList());
    QueryFunction function = QueryFunction.valueOf(ctx.FUNCTION().getText());
    QueryField property = QueryField.fromExternalName(ctx.FIELDNAME().getText());
    WhereToken wToken = new WhereToken(property, function, parameters);
    whereTokens.add(wToken);
    return null;
  }

  public List<WhereToken> getWhereTokens() {
    return whereTokens;
  }

  static Object parseParameterString(String parameterString) {
    if (parameterString.startsWith("\"") && parameterString.endsWith("\"")) {
      return parameterString.replace("\"", "");
    }
    return Long.valueOf(parameterString);
  }

}
