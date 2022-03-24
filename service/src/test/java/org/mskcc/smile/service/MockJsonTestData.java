package org.mskcc.smile.service;

/**
 *
 * @author ochoaa
 */
public class MockJsonTestData {
    private String identifier;
    private String filepath;
    private String description;
    private String jsonString;

    public MockJsonTestData() {}

    /**
     * MockJsonTestData constructor.
     * @param identifier
     * @param filepath
     * @param description
     * @param jsonString
     */
    public MockJsonTestData(String identifier, String filepath, String description, String jsonString) {
        this.identifier = identifier;
        this.filepath = filepath;
        this.description = description;
        this.jsonString = jsonString;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJsonString() {
        return jsonString;
    }

    public void setJsonString(String jsonString) {
        this.jsonString = jsonString;
    }
    
}
