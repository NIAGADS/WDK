package org.gusdb.wdk.model.query.param;

/**
 * captures the information from an ontology query used by filter param.  Only vaguely related to the official Ontology objects.
 * @author steve
 *
 */

public class OntologyItem {
  private String ontologyId;
  private String parentOntologyId;
  private String displayName;
  private String description;
  private String type;
  private String units;
  private long precision;  // TODO: this is long only because jdbc gives us a big decimal.
  private boolean isRange;
  
  public static final String TYPE_STRING = "string";
  public static final String TYPE_NUMBER = "number";
  public static final String TYPE_DATE = "date";

  public void setOntologyId(String ontologyId) {
    this.ontologyId = ontologyId;
  }

  public void setParentOntologyId(String parentOntologyId) {
    this.parentOntologyId = parentOntologyId;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setType(String type) {
    this.type = type;
  }
  
  public void setIsRange(Boolean isRange) {
    this.isRange = isRange;
  }

  public void setUnits(String units) {
    this.units = units;
  }

  public void setPrecision(Long precision) {
    this.precision = precision;
  }

  public String getOntologyId() {
    return ontologyId;
  }

  public String getParentOntologyId() {
    return parentOntologyId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }

  public String getType() {
    return type;
  }

  public String getUnits() {
    return units;
  }

  public Long getPrecision() {
    return precision;
  }
  
  public Boolean getIsRange() {
    return isRange;
  }

}