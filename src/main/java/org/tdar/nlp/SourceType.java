package org.tdar.nlp;

public enum SourceType {

    PERSON("People.txt","custom-person"),
    INSTITUTION("Institutions.txt","custom-organization"),
    CULTURE("Cultures_flattened.txt","culture"),
    SITE("Site_ProjectNames_flattened.txt","site"),
    LOCATION("Geographic_regions.txt","custom-location"),
    FEATURES("Features_flattened.txt","features"),
    OBJECTS("Object_names_flattened.txt","objects"),
    MATERIAL("Materials_flattened.txt","material"),
    CERAMIC("CeramicType_Wares.txt","ceramic"),
    CITATION(null, "citation"),
    SITECODE(null, null),
    DATE(null,null);

    SourceType(String filename, String trainingFilename) {
        this.filename = filename;
        this.trainingFilename = trainingFilename;

    }

    private String filename = "";
    private String trainingFilename = "";

    public String getFilename() {
        return filename;
    }
    
    public String getOutputFilename() {
        return String.format("en-ner-%s.bin", trainingFilename);
    }

    public String getTrainingFilename() {
        return trainingFilename;
    }

    public boolean isCaseSensitive() {
        switch (this) {
            case CERAMIC:
            case MATERIAL:
            case OBJECTS:
            case FEATURES:
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
