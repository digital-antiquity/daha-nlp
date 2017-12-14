package org.tdar.nlp;

public enum SourceType {

    PERSON("People.txt","en-ner-custom-person"),
    INSTITUTION("Institutions.txt","en-ner-custom-organization"),
    CULTURE("Cultures_flattened.txt","culture"),
    SITE("Site_ProjectNames_flattened.txt","site"),
    LOCATION("Geographic_regions.txt","en-ner-custom-location"),
    MATERIAL("Materials_flattened.txt","material"),
    CERAMIC("CeramicType_Wares.txt","ceramic"),
    CITATION(null, "citation"),
    SITECODE(null, null);

    SourceType(String filename, String trainingFilename) {
        this.filename = filename;
        this.trainingFilename = trainingFilename;

    }

    private String filename = "";
    private String trainingFilename = "";

    public String getFilename() {
        return filename;
    }

    public String getTrainingFilename() {
        return trainingFilename;
    }

    public boolean isCaseSensitive() {
        switch (this) {
            case CERAMIC:
            case MATERIAL:
                return false;
            default:
                return true;
        }
    }

    public boolean allowInitialPreposition() {
        switch (this) {
            case MATERIAL:
            case LOCATION:
                return true;
            default:
                return false;
        }
    }
}
