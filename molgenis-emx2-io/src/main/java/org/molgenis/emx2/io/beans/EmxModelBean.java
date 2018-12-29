package org.molgenis.emx2.io.beans;

import org.javers.core.metamodel.annotation.TypeName;
import org.molgenis.emx2.EmxModel;
import org.molgenis.emx2.EmxTable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@TypeName("model")
public class EmxModelBean implements EmxModel {

  private Map<String, EmxTableBean> tables = new LinkedHashMap<>();

  public EmxModelBean() {}

  public EmxModelBean(Map<String, EmxTableBean> tables) {
    this.tables = tables;
  }

  @Override
  public Collection<String> getTableNames() {
    return Collections.unmodifiableSet(tables.keySet());
  }

  @Override
  public EmxTableBean getTable(String name) {
    return tables.get(name);
  }

  @Override
  public Collection<EmxTable> getTables() {
    return Collections.unmodifiableCollection(tables.values());
  }

  public EmxTableBean addTable(String name) {
    EmxTableBean table = new EmxTableBean(name);

    tables.put(name, table);

    return table;
  }

  public String print() {
    StringBuilder builder = new StringBuilder();
    builder.append("EmxModel(");

    for (EmxTableBean table : tables.values()) {
      builder.append("\n").append(table.print());
    }

    builder.append("\n);");

    return builder.toString();
  }
}
