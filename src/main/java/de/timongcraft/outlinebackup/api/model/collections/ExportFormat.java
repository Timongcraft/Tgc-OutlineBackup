package de.timongcraft.outlinebackup.api.model.collections;

public enum ExportFormat {

    MD("outline-markdown"),
    JSON("json"),
    HTML("html");

    public static ExportFormat fromApiValue(String apiValue) {
        for (ExportFormat format : values()) {
            if (format.apiValue.equals(apiValue)) return format;
        }
        throw new IllegalArgumentException("Unknown format: " + apiValue);
    }

    private final String apiValue;

    ExportFormat(String apiValue) {
        this.apiValue = apiValue;
    }

    public String getApiValue() {
        return apiValue;
    }

    @Override
    public String toString() {
        return "ExportFormat{" +
                "apiValue='" + apiValue + '\'' +
                '}';
    }
}