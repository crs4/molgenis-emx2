package org.molgenis.emx2.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.Scalars;
import graphql.schema.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.Filter;
import org.molgenis.emx2.sql.SqlGraphQuery;
import org.molgenis.emx2.sql.SqlTypeUtils;
import org.molgenis.emx2.utils.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.*;

import static graphql.schema.GraphQLEnumType.newEnum;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.ColumnType.REF_ARRAY;
import static org.molgenis.emx2.Order.ASC;
import static org.molgenis.emx2.Order.DESC;
import static org.molgenis.emx2.sql.Filter.f;
import static org.molgenis.emx2.web.JsonApi.jsonToSchema;
import static org.molgenis.emx2.web.JsonApi.schemaToJson;
import static spark.Spark.*;

/**
 * Benchmarks show the api part adds about 10-30ms overhead on top of the underlying database call
 */
class GraphqlApi {
  private static final String INPUT = "input";
  private static final String FILTER = "filter";
  private static final String TABLES = "tables";
  private static final String MEMBERS = "members";
  private static final String FILTER1 = "Filter";
  private static final String DETAIL = "detail";
  private static final String ITEMS = "items";
  private static final String COUNT = "count";
  private static final String LIMIT = "limit";
  private static final String OFFSET = "offset";
  private static final String SEARCH = "search";
  private static final String ORDERBY = "orderby";
  private static Logger logger = LoggerFactory.getLogger(GraphqlApi.class);

  /** standard result type for mutations */
  static GraphQLObjectType typeForMutationResult =
      newObject()
          .name("MolgenisMessage")
          .field(newFieldDefinition().name("type").type(Scalars.GraphQLString).build())
          .field(newFieldDefinition().name("title").type(Scalars.GraphQLString).build())
          .field(newFieldDefinition().name(DETAIL).type(Scalars.GraphQLString).build())
          .build();

  private GraphqlApi() {
    // hide constructor
  }

  static void createGraphQLservice() {
    // schema level operations
    final String schemaPath = "/api/graphql/:schema"; // NOSONAR

    // per schema grapql
    get(schemaPath, GraphqlApi::getQuery);
    post(schemaPath, GraphqlApi::getQuery);

    // small overall graphql
    get("/api/graphql", GraphqlApi::getBaseQuery);
    post("/api/graphql", GraphqlApi::getBaseQuery);
  }

  private static String getBaseQuery(Request request, Response response) throws IOException {
    Database database = MolgenisWebservice.getAuthenticatedDatabase(request);
    return new GraphqlApiForDatabase(database).execute(getQueryFromRequest(request));
  }

  private static final String NAME = "name";
  // base schema is always the same. Crazy code but seems best way to do this code first

  private static String getQuery(Request request, Response response) throws IOException {

    long start = System.currentTimeMillis();
    Schema schema =
        MolgenisWebservice.getAuthenticatedDatabase(request)
            .getSchema(request.params(MolgenisWebservice.SCHEMA));

    GraphQLSchema gl = createGraphQLSchema(schema);
    GraphQL g = GraphQL.newGraphQL(gl).build();
    if (logger.isInfoEnabled())
      logger.info(
          "todo: create cache schema loading, it takes {}ms", (System.currentTimeMillis() - start));

    start = System.currentTimeMillis();

    // tests show overhead of this step is about 1ms (jooq takes the rest)
    return executeQuery(g, getQueryFromRequest(request));
  }

  private static String executeQuery(GraphQL g, String query) throws JsonProcessingException {
    long start = System.currentTimeMillis();

    if (logger.isInfoEnabled())
      logger.info("query: {}", query.replaceAll("[\n|\r|\t]", "").replaceAll(" +", " "));

    // tests show overhead of this step is about 20ms (jooq takes the rest)
    ExecutionResult executionResult = g.execute(query);
    for (GraphQLError err : executionResult.getErrors()) {
      logger.error(err.toString());
    }
    String result = convertExecutionResultToJson(executionResult);

    if (logger.isInfoEnabled())
      logger.info("graphql request completed in {}ms", +(System.currentTimeMillis() - start));

    return result;
  }

  static String convertExecutionResultToJson(ExecutionResult executionResult)
      throws JsonProcessingException {
    // tests show conversions below is under 3ms
    Map<String, Object> toSpecificationResult = executionResult.toSpecification();
    return JsonApi.getWriter().writeValueAsString(toSpecificationResult);
  }

  private static String getQueryFromRequest(Request request) throws IOException {
    String query = null;
    if ("POST".equals(request.requestMethod())) {
      ObjectNode node = new ObjectMapper().readValue(request.body(), ObjectNode.class);
      query = node.get("query").asText();
    } else {
      query =
          request.queryParamOrDefault(
              "query",
              "{\n"
                  + "  __schema {\n"
                  + "    types {\n"
                  + "      name\n"
                  + "    }\n"
                  + "  }\n"
                  + "}");
    }
    return query;
  }

  static GraphQLSchema createGraphQLSchema(Schema model) {
    GraphQLObjectType.Builder queryBuilder = newObject().name("Query");
    GraphQLObjectType.Builder mutationBuilder = newObject().name("Save");
    GraphQLObjectType mutationResultType = typeForMutationResult;

    // add meta query and mutation
    queryBuilder.field(
        newFieldDefinition().name("_meta").type(metaType).dataFetcher(metaQueryFetcher(model)));
    mutationBuilder.field(
        newFieldDefinition()
            .name("alterMetadata")
            .type(mutationResultType)
            .dataFetcher(metaMutationFetcher(model))
            .argument(GraphQLArgument.newArgument().name(INPUT).type(metaInput())));

    // add query and mutation for each table
    for (String tableName : model.getTableNames()) {
      Table table = model.getTable(tableName);

      // create types
      GraphQLObjectType tableType = tableObjectType(table);
      GraphQLInputObjectType inputType = tableInputObjectType(table);
      GraphQLObjectType connection = tableConnectionObjectType(table, tableType);

      // create query and mutation fields
      queryBuilder.field(getTableQueryField(table, connection));
      mutationBuilder.field(createTableMutationField(mutationResultType, table, inputType));

      //      newFieldDefinition()
      //          .name("delete" + tableMetadata.getTableName())
      //          .type(GraphQLTypeReference.typeRef(MUTATION_RESULT))
      //          .dataFetcher(createDeleteFetcher(table))
      //
      // .argument(GraphQLArgument.newArgument().name("input").type(GraphQLList.list(inputType)));
    }

    // assemble and return
    return graphql.schema.GraphQLSchema.newSchema()
        .query(queryBuilder)
        .mutation(mutationBuilder)
        .build();
  }

  private static GraphQLFieldDefinition.Builder createTableMutationField(
      GraphQLObjectType mutationResultType, Table table, GraphQLInputObjectType inputType) {
    return newFieldDefinition()
        .name("save" + table.getName())
        .type(mutationResultType)
        .dataFetcher(fetcherForSave(table))
        .argument(GraphQLArgument.newArgument().name(INPUT).type(GraphQLList.list(inputType)));
  }

  private static GraphQLFieldDefinition.Builder getTableQueryField(
      Table table, GraphQLObjectType connection) {
    return newFieldDefinition()
        .name(table.getName())
        .type(connection)
        .dataFetcher(tableQueryFetcher(table))
        .argument(
            GraphQLArgument.newArgument()
                .name(FILTER)
                .type(tableFilterInputObjectType(table.getMetadata()))
                .build())
        .argument(GraphQLArgument.newArgument().name(SEARCH).type(Scalars.GraphQLString).build());
  }

  private static GraphQLObjectType tableConnectionObjectType(
      Table table, GraphQLObjectType tableType) {
    GraphQLObjectType.Builder connectionBuilder = newObject().name(table.getName() + "Connection");
    connectionBuilder.field(newFieldDefinition().name(COUNT).type(Scalars.GraphQLInt));
    // connectionBuilder.field(newFieldDefinition().name("meta").type(metadataType));
    connectionBuilder.field(
        newFieldDefinition()
            .name(ITEMS)
            .type(GraphQLList.list(tableType))
            .argument(GraphQLArgument.newArgument().name(LIMIT).type(Scalars.GraphQLInt).build())
            .argument(GraphQLArgument.newArgument().name(OFFSET).type(Scalars.GraphQLInt).build())
            .argument(
                GraphQLArgument.newArgument()
                    .name(ORDERBY)
                    .type(tableOrderByInputObjectType(table))
                    .build()));
    return connectionBuilder.build();
  }

  private static GraphQLInputObjectType tableInputObjectType(Table table) {
    GraphQLInputObjectType.Builder inputBuilder = newInputObject().name(table.getName() + "Input");
    for (Column col : table.getMetadata().getColumns()) {
      inputBuilder.field(
          newInputObjectField().name(col.getColumnName()).type(typeForColumnInput(col)));
    }
    return inputBuilder.build();
  }

  private static GraphQLObjectType tableObjectType(Table table) {
    GraphQLObjectType.Builder tableTypeBuilder = newObject().name(table.getName());
    for (Column col : table.getMetadata().getColumns()) {
      tableTypeBuilder.field(
          newFieldDefinition().name(col.getColumnName()).type(typeForColumn(col)));
    }
    return tableTypeBuilder.build();
  }

  private static DataFetcher<?> metaMutationFetcher(Schema model) {
    return dataFetchingEnvironment -> {
      try {
        Map<String, Object> metaInput = dataFetchingEnvironment.getArgument(INPUT);
        model.tx(
            db -> {
              try {
                if (metaInput.containsKey(TABLES)) {
                  String json = JsonApi.getWriter().writeValueAsString(metaInput);
                  SchemaMetadata otherSchema = jsonToSchema(json);
                  model.merge(otherSchema);
                }
                if (metaInput.containsKey(MEMBERS)) {
                  List<Map<String, String>> members = (List) metaInput.get(MEMBERS);
                  for (Map<String, String> m : members) {
                    model.addMember(m.get("user"), m.get("role"));
                  }
                }
              } catch (IOException e) {
                throw new GraphqlApiException(e);
              }
            });
        Map result = new LinkedHashMap<>();
        result.put(DETAIL, "success");
        return result;
      } catch (MolgenisException e) {
        return transform(e);
      }
    };
  }

  private static Object transform(MolgenisException e) {
    Map<String, String> result = new LinkedHashMap<>();
    result.put("title", e.getTitle());
    result.put("type", e.getType());
    result.put(DETAIL, e.getDetail());
    return result;
  }

  private static GraphQLInputType typeForColumnInput(Column col) {
    ColumnType type = col.getColumnType();
    if (REF.equals(type)) type = SqlTypeUtils.getRefColumnType(col);
    switch (type) {
      case DECIMAL:
        return Scalars.GraphQLBigDecimal;
      case INT:
        return Scalars.GraphQLInt;
      default:
        return Scalars.GraphQLString;
    }
  }

  // metadataType
  private static GraphQLType metaRolesType =
      new GraphQLObjectType.Builder()
          .name("MolgenisRolesType")
          .field(newFieldDefinition().name(NAME).type(Scalars.GraphQLString))
          .build();

  private static GraphQLType metaMembersType =
      new GraphQLObjectType.Builder()
          .name("MolgenisMembersType")
          .field(newFieldDefinition().name("user").type(Scalars.GraphQLString))
          .field(newFieldDefinition().name("role").type(Scalars.GraphQLString))
          .build();

  private static GraphQLObjectType columnMetadataType =
      new GraphQLObjectType.Builder()
          .name("MolgenisColumnType")
          .field(newFieldDefinition().name(NAME).type(Scalars.GraphQLString))
          .field(newFieldDefinition().name("columnType").type(Scalars.GraphQLString))
          .field(newFieldDefinition().name("pkey").type(Scalars.GraphQLBoolean))
          .field(newFieldDefinition().name("nullable").type(Scalars.GraphQLBoolean))
          .field(newFieldDefinition().name("refTableName").type(Scalars.GraphQLString))
          .field(newFieldDefinition().name("refColumnName").type(Scalars.GraphQLString))
          .build();

  private static GraphQLObjectType tableMetadataType =
      new GraphQLObjectType.Builder()
          .name("MolgenisTableType")
          .field(newFieldDefinition().name(NAME).type(Scalars.GraphQLString))
          .field(newFieldDefinition().name("pkey").type(GraphQLList.list(Scalars.GraphQLString)))
          .field(
              newFieldDefinition()
                  .name("unique")
                  .type(GraphQLList.list(GraphQLList.list(Scalars.GraphQLString))))
          .field(newFieldDefinition().name("columns").type(GraphQLList.list(columnMetadataType)))
          .build();

  private static GraphQLObjectType metaType =
      new GraphQLObjectType.Builder()
          .name("MolgenisMetaType")
          .field(newFieldDefinition().name(TABLES).type(GraphQLList.list(tableMetadataType)))
          .field(newFieldDefinition().name(MEMBERS).type(GraphQLList.list(metaMembersType)))
          .field(newFieldDefinition().name("roles").type(GraphQLList.list(metaRolesType)))
          .build();

  private static GraphQLInputObjectType metaInput() {
    GraphQLInputObjectType.Builder metaBuilder = new GraphQLInputObjectType.Builder();
    metaBuilder.name("MolgenisMetaInput");
    metaBuilder.field(newInputObjectField().name(TABLES).type(GraphQLList.list(metaTablesInput())));
    metaBuilder.field(
        newInputObjectField().name(MEMBERS).type(GraphQLList.list(metaMembersInput())));
    return metaBuilder.build();
  }

  private static GraphQLInputObjectType metaMembersInput() {
    GraphQLInputObjectType.Builder membersTypeBuilder = new GraphQLInputObjectType.Builder();
    membersTypeBuilder.name("MolgenisMembersInput");
    membersTypeBuilder.field(newInputObjectField().name("user").type(Scalars.GraphQLString));
    membersTypeBuilder.field(newInputObjectField().name("role").type(Scalars.GraphQLString));
    return membersTypeBuilder.build();
  }

  private static GraphQLInputObjectType metaTablesInput() {
    // todo: is there a way to use same type between input and query?
    GraphQLInputObjectType.Builder columnTypeBuilder = new GraphQLInputObjectType.Builder();
    columnTypeBuilder.name("MolgenisColumnInput");
    columnTypeBuilder.field(newInputObjectField().name(NAME).type(Scalars.GraphQLString));
    columnTypeBuilder.field(newInputObjectField().name("columnType").type(Scalars.GraphQLString));
    columnTypeBuilder.field(newInputObjectField().name("pkey").type(Scalars.GraphQLBoolean));
    columnTypeBuilder.field(newInputObjectField().name("nullable").type(Scalars.GraphQLBoolean));
    columnTypeBuilder.field(newInputObjectField().name("refTableName").type(Scalars.GraphQLString));
    columnTypeBuilder.field(
        newInputObjectField().name("refColumnName").type(Scalars.GraphQLString));

    GraphQLInputObjectType.Builder tableTypeBuilder = new GraphQLInputObjectType.Builder();
    tableTypeBuilder.name("MolgenisTableInput");
    tableTypeBuilder.field(newInputObjectField().name(NAME).type(Scalars.GraphQLString));
    tableTypeBuilder.field(
        newInputObjectField().name("pkey").type(GraphQLList.list(Scalars.GraphQLString)));
    tableTypeBuilder.field(
        newInputObjectField()
            .name("unique")
            .type(GraphQLList.list(GraphQLList.list(Scalars.GraphQLString))));

    tableTypeBuilder.field(
        newInputObjectField().name("columns").type(GraphQLList.list(columnTypeBuilder.build())));

    return tableTypeBuilder.build();
  }

  private static GraphQLOutputType typeForColumn(Column col) {
    switch (col.getColumnType()) {
      case DECIMAL:
        return Scalars.GraphQLBigDecimal;
      case INT:
        return Scalars.GraphQLInt;
      case REF:
        return GraphQLTypeReference.typeRef(col.getRefTableName());
      case REF_ARRAY:
        return GraphQLTypeReference.typeRef(col.getRefTableName() + "Connection");
      default:
        return Scalars.GraphQLString;
    }
  }

  private static GraphQLInputObjectType tableFilterInputObjectType(TableMetadata table) {
    GraphQLInputObjectType.Builder filterBuilder =
        newInputObject().name(table.getTableName() + FILTER1);
    for (Column col : table.getColumns()) {
      if (REF.equals(col.getColumnType()) || REF_ARRAY.equals((col.getColumnType()))) {
        filterBuilder.field(
            newInputObjectField()
                .name(col.getColumnName())
                .type(GraphQLTypeReference.typeRef(col.getRefTableName() + FILTER1))
                .build());
      } else {
        filterBuilder.field(
            newInputObjectField()
                .name(col.getColumnName())
                .type(columnFilterInputObjectType(col))
                .build());
      }
    }
    return filterBuilder.build();
  }

  private static final GraphQLEnumType orderByEnum =
      newEnum().name("MolgenisOrderByEnum").value(ASC.name(), ASC).value(DESC.name(), DESC).build();

  private static GraphQLInputObjectType tableOrderByInputObjectType(Table table) {
    GraphQLInputObjectType.Builder orderByBuilder =
        newInputObject().name(table.getName() + ORDERBY);
    for (Column col : table.getMetadata().getColumns()) {
      if (!REF_ARRAY.equals((col.getColumnType()))) {
        orderByBuilder.field(newInputObjectField().name(col.getColumnName()).type(orderByEnum));
      }
    }
    return orderByBuilder.build();
  }

  private static Map<ColumnType, GraphQLInputObjectType> filterInputTypes = new LinkedHashMap<>();

  private static GraphQLInputObjectType columnFilterInputObjectType(Column column) {
    ColumnType type = column.getColumnType();
    // singleton
    if (filterInputTypes.get(type) == null) {
      String typeName = type.toString().toLowerCase();
      typeName = typeName.substring(0, 1).toUpperCase() + typeName.substring(1);
      GraphQLInputObjectType.Builder builder =
          newInputObject().name("Molgenis" + typeName + FILTER1);
      for (Operator operator : type.getOperators()) {
        builder.field(
            newInputObjectField()
                .name(operator.getAbbreviation())
                .type(GraphQLList.list(graphQLTypeOf(type))));
      }
      filterInputTypes.put(type, builder.build());
    }
    return filterInputTypes.get(type);
  }

  private static GraphQLScalarType graphQLTypeOf(ColumnType type) {
    switch (type) {
      case BOOL:
      case BOOL_ARRAY:
        return Scalars.GraphQLBoolean;
      case INT:
      case INT_ARRAY:
        return Scalars.GraphQLInt;
      case DECIMAL:
      case DECIMAL_ARRAY:
        return Scalars.GraphQLFloat;
      case DATE_ARRAY:
      case DATE:
      case DATETIME:
      case DATETIME_ARRAY:
      case STRING:
      case TEXT:
      case STRING_ARRAY:
      case TEXT_ARRAY:
      case UUID:
      case UUID_ARRAY:
      case REF:
      case REF_ARRAY:
      case MREF:
    }
    return Scalars.GraphQLString;
  }

  private static DataFetcher fetcherForSave(Table aTable) {
    return dataFetchingEnvironment -> {
      try {
        Table table = aTable;
        List<Map<String, Object>> map = dataFetchingEnvironment.getArgument(INPUT);
        int count = table.update(convertToRows(map));
        return resultMessage("success. saved " + count + " records");
      } catch (MolgenisException me) {
        return transform(me);
      }
    };
  }

  private static Map<String, String> resultMessage(String detail) {
    Map<String, String> message = new LinkedHashMap<>();
    message.put(DETAIL, detail);
    return message;
  }

  private static Iterable<Row> convertToRows(List<Map<String, Object>> map) {
    List<Row> rows = new ArrayList<>();
    for (Map<String, Object> row : map) {
      rows.add(new Row(row));
    }
    return rows;
  }

  private static DataFetcher<?> fetcherForDelete(Table table) {
    return dataFetchingEnvironment -> {
      throw new UnsupportedOperationException();
    };
  }

  private static DataFetcher<?> metaQueryFetcher(Schema schema) {
    return dataFetchingEnvironment -> {

      // silly conversions, look into if we can bypass

      // add schema
      String json = schemaToJson(schema.getMetadata());
      Map<String, Object> result = new ObjectMapper().readValue(json, Map.class);

      // add members
      List<Map<String, String>> members = new ArrayList<>();
      for (Member m : schema.getMembers()) {
        members.add(Map.of("user", m.getUser(), "role", m.getRole()));
      }
      result.put(MEMBERS, members);

      // add roles
      List<Map<String, String>> roles = new ArrayList<>();
      for (String role : schema.getRoles()) {
        roles.add(Map.of(NAME, role));
      }
      result.put("roles", roles);

      return result;
    };
  }

  private static DataFetcher tableQueryFetcher(Table aTable) {
    return dataFetchingEnvironment -> {
      Table table = aTable;
      SqlGraphQuery q = new SqlGraphQuery(table);
      q.select(createSelect(dataFetchingEnvironment.getSelectionSet()));
      if (dataFetchingEnvironment.getArgument(FILTER) != null) {
        q.filter(createFilters(table, dataFetchingEnvironment.getArgument(FILTER)));
      }
      String search = dataFetchingEnvironment.getArgument(SEARCH);
      if (search != null) {
        // todo proper tokenizer
        q.search(search.split(" "));
      }

      return transform(q.retrieve());
    };
  }

  private static Filter[] createFilters(Table table, Map<String, Object> filter) {
    List<Filter> subFilters = new ArrayList<>();
    for (Map.Entry<String, Object> entry : filter.entrySet()) {
      Column c = table.getMetadata().getColumn(entry.getKey());
      if (c == null)
        throw new GraphqlApiException(
            "Column " + entry.getKey() + " unknown in table " + table.getName());
      switch (c.getColumnType()) {
        case REF:
        case REF_ARRAY:
          subFilters.add(
              f(
                  c.getColumnName(),
                  createFilters(
                      table.getSchema().getTable(c.getRefTableName()), (Map) entry.getValue())));
          break;
        default:
          if (entry.getValue() instanceof Map) {
            Filter f = f(entry.getKey());
            for (Map.Entry<String, Object> entry2 :
                ((Map<String, Object>) entry.getValue()).entrySet()) {
              Operator op = Operator.fromAbbreviation(entry2.getKey());
              if (entry2.getValue() instanceof List) {
                f.add(op, (List) entry2.getValue());
              } else {
                f.add(op, entry2.getValue());
              }
            }
            subFilters.add(f);
          } else {
            throw new GraphqlApiException(
                "unknown filter expression " + entry.getValue() + " for column " + entry.getKey());
          }
      }
    }
    return subFilters.toArray(new Filter[subFilters.size()]);
  }

  /** bit unfortunate that we have to convert from json to map and back */
  private static Object transform(String json) throws IOException {
    // benchmark shows this only takes a few ms so not a large performance issue
    if (json != null) {
      return new ObjectMapper().readValue(json, Map.class);
    } else {
      return new LinkedHashMap<>();
    }
  }

  /** creates a list like List.of(field1,field2, path1, List.of(pathsubfield1), ...) */
  private static SqlGraphQuery.SelectColumn[] createSelect(
      DataFetchingFieldSelectionSet selection) {
    List<SqlGraphQuery.SelectColumn> result = new ArrayList<>();
    for (SelectedField s : selection.getFields()) {
      if (!s.getQualifiedName().contains("/")) {
        if (!s.getSelectionSet().getFields().isEmpty()) {
          SqlGraphQuery.SelectColumn sc =
              new SqlGraphQuery.SelectColumn(s.getName(), createSelect(s.getSelectionSet()));
          // get limit and offset for the selection
          Map<String, Object> args = s.getArguments();
          if (args.containsKey(LIMIT)) {
            sc.setLimit((int) args.get(LIMIT));
          }
          if (args.containsKey(OFFSET)) {
            sc.setOffset((int) args.get(OFFSET));
          }
          if (args.containsKey(ORDERBY)) {
            sc.setOrderBy((Map<String, Order>) args.get(ORDERBY));
          }
          result.add(sc);
        } else {
          result.add(new SqlGraphQuery.SelectColumn(s.getName()));
        }
      }
    }
    return result.toArray(new SqlGraphQuery.SelectColumn[result.size()]);
  }

  private static class GraphqlApiException extends MolgenisException {
    public GraphqlApiException(String message) {
      super(
          GraphqlApi.class.getSimpleName().toUpperCase(),
          GraphqlApi.class.getSimpleName(),
          message);
    }

    public GraphqlApiException(IOException e) {
      super(e);
    }
  }
}
