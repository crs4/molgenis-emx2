package org.molgenis.emx2.semantics.rdf;

import static org.molgenis.emx2.semantics.RDFService.encodedIRI;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.molgenis.emx2.Schema;

public class SchemaToRDF {
  public static void describeSchema(ModelBuilder builder, Schema schema, String schemaContext) {
    builder.add(schemaContext, RDFS.LABEL, schema.getName());
    if (schema.getMetadata().getDescription() != null) {
      builder.add(schemaContext, DCTERMS.DESCRIPTION, schema.getMetadata().getDescription());
    }
    builder.add(schemaContext, RDF.TYPE, RDFS.CONTAINER);
    for (String tableName : schema.getTableNames()) {
      IRI tableContext = encodedIRI(schemaContext + "/" + tableName);
      builder.add(schemaContext, "http://www.w3.org/ns/ldp#contains", tableContext);
    }
  }
}
