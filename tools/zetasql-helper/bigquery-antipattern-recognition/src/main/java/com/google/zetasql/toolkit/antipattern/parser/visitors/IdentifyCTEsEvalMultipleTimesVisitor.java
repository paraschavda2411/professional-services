package com.google.zetasql.toolkit.antipattern.parser.visitors;

import com.google.zetasql.parser.ASTNodes.ASTSelect;
import com.google.zetasql.parser.ASTNodes.ASTTablePathExpression;
import com.google.zetasql.parser.ASTNodes.ASTWithClause;
import com.google.zetasql.parser.ParseTreeVisitor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class IdentifyCTEsEvalMultipleTimesVisitor extends ParseTreeVisitor {

  // A string template to be used for generating the suggestion message.
  private final String MULTIPLE_CTE_SUGGESTION_MESSAGE =
      "CTE with multiple references: alias %s is referenced %d times.";

  // An array list to store the suggestions.
  private ArrayList<String> result = new ArrayList<String>();

  // A map to keep track of the number of times each CTE is evaluated.
  private Map<String, Integer> cteCountMap = new HashMap<>();


  @Override
  public void visit(ASTWithClause withClause) {

    // Loop through all the CTE entries in the WITH clause.
    withClause.getWith().forEach(alias -> {

      // Add the CTE name to the count map with initial count 0.
      cteCountMap.put(alias.getAlias().getIdString().toLowerCase(),0);

      // If the query expression is a SELECT statement, visit the FROM clause.
      if (alias.getQuery().getQueryExpr() instanceof ASTSelect){
        ASTTablePathExpression tablePathExp = (ASTTablePathExpression)((ASTSelect) alias.getQuery().getQueryExpr()).getFromClause().getTableExpression();
        visit(tablePathExp);
      }
    });
  }


  public void visit(ASTTablePathExpression tablePathExpression){

    // Loop through all the identifiers in the table path expression.
    tablePathExpression.getPathExpr().getNames().forEach(identifier -> {

      // Get the identifier as a string in lower case.
      String table = identifier.getIdString().toLowerCase();

      // If the count map contains the identifier, increment its count.
      if(cteCountMap.containsKey(table)){
        cteCountMap.put(table,cteCountMap.get(table) + 1);
      }
    });

    // Loop through all the entries in the count map.
    for (Map.Entry<String, Integer> entry : cteCountMap.entrySet()) {

      // Get the CTE name and its count.
      String cteName = entry.getKey();
      int count = entry.getValue();

      // If the CTE count is greater than 1, add the suggestion message to the list.
      if (count > 1) {
        result.add(String.format(MULTIPLE_CTE_SUGGESTION_MESSAGE,cteName,count));
      }
    }
  }

  // Getter method to retrieve the list of suggestion messages.
  public ArrayList<String> getResult() {
    return result;
  }
}