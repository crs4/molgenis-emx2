package org.molgenis.emx2.rdf;

import static java.util.Objects.requireNonNull;
import static org.molgenis.emx2.rdf.RdfUtils.formatBaseURL;

import org.molgenis.emx2.rdf.mappers.OntologyIriMapper;
import org.molgenis.emx2.rdf.mappers.ReferenceIriMapper;

public class RdfMapData {
  private final String baseURL;
  private final OntologyIriMapper ontologyIriMapper;
  private final ReferenceIriMapper referenceIriMapper;

  public RdfMapData(
      String baseURL, OntologyIriMapper ontologyIriMapper, ReferenceIriMapper referenceIriMapper) {
    this.baseURL = formatBaseURL(baseURL);
    this.ontologyIriMapper = requireNonNull(ontologyIriMapper);
    this.referenceIriMapper = requireNonNull(referenceIriMapper);
  }

  public String getBaseURL() {
    return baseURL;
  }

  public OntologyIriMapper getOntologyIriMapper() {
    return ontologyIriMapper;
  }

  public ReferenceIriMapper getReferenceIriMapper() {
    return referenceIriMapper;
  }
}
