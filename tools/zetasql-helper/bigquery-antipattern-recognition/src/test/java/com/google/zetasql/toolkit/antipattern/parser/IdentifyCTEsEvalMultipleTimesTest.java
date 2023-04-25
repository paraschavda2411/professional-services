package com.google.zetasql.toolkit.antipattern.parser;

import static org.junit.Assert.assertEquals;

import com.google.zetasql.LanguageOptions;
import com.google.zetasql.Parser;
import com.google.zetasql.parser.ASTNodes.ASTStatement;
import org.junit.Before;
import org.junit.Test;

public class IdentifyCTEsEvalMultipleTimesTest {
  LanguageOptions languageOptions;

  @Before
  public void setUp() {
    languageOptions = new LanguageOptions();
    languageOptions.enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();
  }

  // Test with a query that uses two CTEs aliased (b,c) referencing CTE aliased (a) multiple times
  @Test
  public void withThreeCTEsTest() {
    String expected = "CTE with multiple references: alias a is referenced 2 times.";
    String query = "WITH\n" +
        "    a AS (\n" +
        "        SELECT\n" +
        "            *\n" +
        "        FROM\n" +
        "            `project.dataset.table1` t1\n" +
        "    ),\n" +
        "    b AS (\n" +
        "        SELECT\n" +
        "            *\n" +
        "        FROM\n" +
        "            a t2\n" +
        "    ),\n" +
        "    c AS (\n" +
        "        SELECT\n" +
        "            *\n" +
        "        FROM\n" +
        "            a t3\n" +
        "    )\n" +
        "SELECT\n" +
        "    b.col1,\n" +
        "    c.col2\n" +
        "FROM\n" +
        "    b, c;";


    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendations = (new IdentifyCTEsEvalMultipleTimes()).run(parsedQuery);
    assertEquals(expected, recommendations);
  }

  // Test with a query that uses one CTE aliased (a) referenced  multiple times in from clause and a subquery
  @Test
  public void withOneCTETest() {
    String expected = "CTE with multiple references: alias a is referenced 2 times.";
    String query = "WITH\n" +
        "  a AS (\n" +
        "  SELECT\n" +
        "    *\n" +
        "  FROM\n" +
        "    `project.dataset.table1 t1` )\n" +
        "SELECT\n" +
        "  a.col1,\n" +
        "  a.col2,\n" +
        "  total\n" +
        "FROM\n" +
        "  a\n" +
        "LEFT JOIN (\n" +
        "  SELECT\n" +
        "    a.col1,\n" +
        "    SUM(a.col3) total\n" +
        "  FROM\n" +
        "    a\n" +
        "  GROUP BY\n" +
        "    a.col1) t2\n" +
        "ON\n" +
        "  t2.col1 = a.col2;";

    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = (new IdentifyCTEsEvalMultipleTimes()).run(parsedQuery);
    assertEquals(expected, recommendation);
  }


  // Test with a query that uses two CTEs aliased (b,c) not referenced more than once.
  @Test
  public void withTwoCTEsTest() {
    String expected = "";
    String query = "WITH \n" +
            "    b AS ( \n" +
            "        SELECT \n" +
            "            * \n" +
            "        FROM \n" +
            "            `project.dataset.b` \n" +
            "    ), \n" +
            "    c AS ( \n" +
            "        SELECT \n" +
            "            * \n" +
            "        FROM \n" +
            "            `project.dataset.c` \n" +
            "    ) \n" +
            "SELECT \n" +
            "    b.dim1, \n" +
            "    c.dim2 \n" +
            "FROM \n" +
            "    b, \n" +
            "    c;";

    ASTStatement parsedQuery = Parser.parseStatement(query, languageOptions);
    String recommendation = (new IdentifyCTEsEvalMultipleTimes()).run(parsedQuery);
    assertEquals(expected, recommendation);
  }
}