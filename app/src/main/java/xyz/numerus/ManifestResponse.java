package xyz.numerus;

import java.util.Map;

public class ManifestResponse {
    public int schema_version;
    public String last_updated;
    public int min_app_version_required;
    public Map<String, LanguageInfo> languages;

    public static class LanguageInfo {
        public int version;
        public String url;
    }
}
