package org.molgenis.emx2.beaconv2.responses.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class EntryTypes {
  Dataset dataset = new Dataset();
  // TODO: others e.g. genomic variant that follow the same structure as Dataset
  // except for aCollectionOf in Dataset that announces the presence of these 'others'
}
