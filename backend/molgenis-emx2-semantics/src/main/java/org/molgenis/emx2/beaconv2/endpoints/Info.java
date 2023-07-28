package org.molgenis.emx2.beaconv2.endpoints;

import static org.molgenis.emx2.semantics.RDFService.extractHost;
import static org.molgenis.emx2.semantics.rdf.IRIParsingEncoding.getURI;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.net.URI;
import java.net.URISyntaxException;
import org.molgenis.emx2.beaconv2.common.Meta;
import org.molgenis.emx2.beaconv2.endpoints.info.InfoResponse;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Info {

  // private String $schema;
  private Meta meta;
  private InfoResponse response;

  public Info() throws URISyntaxException {
    // this.$schema = "../beaconInfoResponse.json";
    this.meta = new Meta("../beaconInfoResponse.json", "info");
    this.response = new InfoResponse("unknown host");
  }

  public Info(Request request) throws URISyntaxException {
    // this.$schema = "../beaconInfoResponse.json";
    this.meta = new Meta("../beaconInfoResponse.json", "info");
    URI requestURI = getURI(request.url());
    String host = extractHost(requestURI);
    this.response = new InfoResponse(host);
  }
}
