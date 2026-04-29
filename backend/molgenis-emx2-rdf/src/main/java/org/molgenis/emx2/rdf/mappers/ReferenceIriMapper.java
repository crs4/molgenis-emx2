package org.molgenis.emx2.rdf.mappers;

import static org.molgenis.emx2.SelectColumn.s;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.TableType;

public class ReferenceIriMapper {
  private final Map<String, Map<String, Map<String, IRI>>> irisPerSchema = new HashMap<>();

  public ReferenceIriMapper(Collection<Table> tables) {
    addAll(tables);
  }

  public ReferenceIriMapper(Table table) {
    add(table);
  }

  public ReferenceIriMapper() {}

  private void addAll(Collection<Table> tables) {
    for (Table table : tables) {
      add(table);
    }
  }

  private void add(Table table) {
    if (Objects.requireNonNull(table.getMetadata().getTableType()) == TableType.DATA) {
      addDataTable(table);
    }
  }

  private boolean hasIriColumn(TableMetadata table) {
    return table.getColumns().stream().anyMatch(c -> c.getName().equals("IRI"));
  }

  private void addDataTable(Table table) {
    for (Column column : table.getMetadata().getColumns()) {
      if (column.isReference() && hasIriColumn(column.getRefTable())) {
        addIrisForReference(column.getRefTable().getTable());
      }
    }
  }

  private void addIrisForReference(Table data) {
    Map<String, Map<String, IRI>> irisPerData =
        irisPerSchema.getOrDefault(data.getSchema().getName(), new HashMap<>());

    Map<String, IRI> irisPerName = irisPerData.getOrDefault(data.getName(), new HashMap<>());

    // Skips adding if already done.
    if (irisPerName.isEmpty()) {
      for (Row row : queryReferencesTable(data)) {
        if (row.getString("IRI") == null) continue;
        irisPerName.put(row.getString("id"), Values.iri(row.getString("IRI")));
      }
      irisPerData.put(data.getName(), irisPerName);
      irisPerSchema.put(data.getSchema().getName(), irisPerData);
    }
  }

  private List<Row> queryReferencesTable(Table table) {
    return table.query().select(s("id"), s("IRI")).retrieveRows();
  }

  /**
   * Retrieves IRI for single value.
   *
   * @return IRI if match is found, otherwise {@code null}
   */
  public IRI get(String schemaName, String dataTableName, String value) {
    Map<String, Map<String, IRI>> schemaIris = irisPerSchema.get(schemaName);
    if (schemaIris == null) return null;
    Map<String, IRI> referenceIris = schemaIris.get(dataTableName);
    if (referenceIris == null) return null;
    return referenceIris.get(value);
  }

  /**
   * Retrieves IRI for multiple values.
   *
   * @return a {@link Map} with the input {@code values} as key and the found match (or {@code null}
   *     for no match) as value
   * @see #get(String, String, String)
   */
  public Map<String, IRI> map(String schemaName, String dataTableName, String... values) {
    Map<String, IRI> iriMap = new HashMap<>();
    for (String value : values) {
      iriMap.put(value, get(schemaName, dataTableName, value));
    }
    return iriMap;
  }
}
