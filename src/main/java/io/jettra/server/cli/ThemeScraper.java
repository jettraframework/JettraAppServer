package io.jettra.server.cli;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThemeScraper {

    public static String scrapeThemeJson(String urlSource, String projectName) {
        String primary = "#ef4444";
        String secondary = "#dc2626";
        String background = "#0f172a";
        String surface = "rgba(30, 41, 59, 0.7)";
        String onPrimary = "#ffffff";
        String onSurface = "#f8fafc";
        String fontFamily = "'Inter', sans-serif";
        
        StringBuilder customCss = new StringBuilder();
        StringBuilder customJs = new StringBuilder();
        
        if (urlSource != null && !urlSource.isEmpty()) {
            try {
                System.out.println("[ThemeScraper] Fetching HTML from " + urlSource + " ...");
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(urlSource))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                String html = response.body();

                // Extract CSS links
                Pattern linkPattern = Pattern.compile("<link[^>]+(?:rel=[\"']stylesheet[\"'][^>]*href=[\"']([^\"']+)[\"']|href=[\"']([^\"']+)[\"'][^>]*rel=[\"']stylesheet[\"'])[^>]*>", Pattern.CASE_INSENSITIVE);
                Matcher linkMatcher = linkPattern.matcher(html);
                List<String> cssUrls = new ArrayList<>();
                while (linkMatcher.find()) {
                    String href = linkMatcher.group(1) != null ? linkMatcher.group(1) : linkMatcher.group(2);
                    if (!href.startsWith("http") && !href.startsWith("data:")) {
                        href = URI.create(urlSource).resolve(href).toString();
                    }
                    cssUrls.add(href);
                }
                
                // Also find <style> tags
                Pattern stylePattern = Pattern.compile("<style[^>]*>(.*?)</style>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                Matcher styleMatcher = stylePattern.matcher(html);
                while (styleMatcher.find()) {
                    String styleContent = styleMatcher.group(1);
                    styleContent = styleContent.replaceAll("\\r?\\n", " ").replaceAll("\\s+", " ");
                    styleContent = resolveCssUrls(styleContent, urlSource);
                    customCss.append(styleContent).append(" ");
                }

                // Download external CSS
                for (String cssUrl : cssUrls) {
                    try {
                        System.out.println("[ThemeScraper] Downloading CSS: " + cssUrl);
                        HttpRequest cssReq = HttpRequest.newBuilder().uri(URI.create(cssUrl)).GET().build();
                        HttpResponse<String> cssResp = client.send(cssReq, HttpResponse.BodyHandlers.ofString());
                        String css = cssResp.body();
                        // Basic minification to fit in JSON
                        css = css.replaceAll("\\r?\\n", " ").replaceAll("\\s+", " ");
                        css = resolveCssUrls(css, cssUrl);
                        customCss.append("/* From ").append(cssUrl).append(" */ ").append(css).append(" ");
                    } catch (Exception e) {
                        System.err.println("[ThemeScraper] Warning: Could not download CSS " + cssUrl + " - " + e.getMessage());
                    }
                }
                
                // Extract JS scripts
                Pattern scriptPattern = Pattern.compile("<script[^>]+src=[\"']([^\"']+)[\"'][^>]*></script>", Pattern.CASE_INSENSITIVE);
                Matcher scriptMatcher = scriptPattern.matcher(html);
                while (scriptMatcher.find()) {
                    String src = scriptMatcher.group(1);
                    if (!src.startsWith("http") && !src.startsWith("data:")) {
                        src = URI.create(urlSource).resolve(src).toString();
                    }
                    customJs.append("var s = document.createElement('script'); s.src = '").append(src).append("'; s.crossOrigin = 'anonymous'; document.head.appendChild(s); ");
                }
                
                System.out.println("[ThemeScraper] Successfully extracted styles and scripts from " + urlSource);

            } catch (Exception e) {
                System.err.println("[ThemeScraper] Failed to scrape theme from " + urlSource + ": " + e.getMessage());
                System.err.println("[ThemeScraper] Falling back to default styling.");
            }
        }

        // Default custom CSS if nothing was found
        if (customCss.length() == 0) {
            customCss.append(".top-btn-today { background-color: #d32f2f; color: white; border: none; padding: 8px 16px; border-radius: 6px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 8px; } ");
            customCss.append(".sidebar-logo { font-size: 1.5rem; font-weight: 700; color: #d32f2f; padding: 10px 15px; margin-bottom: 20px; display: flex; align-items: center; gap: 10px; }");
        } else {
            // Trim and limit the CSS size if it's absurdly large, though typically we want all of it
            if (customCss.length() > 500000) {
                System.err.println("[ThemeScraper] Warning: CSS is extremely large, truncating to avoid OOM.");
                customCss.setLength(500000);
            }
            
            // Extract colors and fonts dynamically from the gathered CSS
            String cssStr = customCss.toString();
            Matcher m;
            
            m = Pattern.compile("--p-primary-500:\\s*([^;]+);").matcher(cssStr);
            if (m.find()) primary = "var(--p-primary-500, " + m.group(1) + ")";
            else {
                m = Pattern.compile("--primary-color:\\s*([^;]+);").matcher(cssStr);
                if (m.find()) primary = "var(--primary-color, " + m.group(1) + ")";
                else {
                    m = Pattern.compile("--bs-primary:\\s*([^;]+);").matcher(cssStr);
                    if (m.find()) primary = "var(--bs-primary, " + m.group(1) + ")";
                }
            }

            m = Pattern.compile("--p-primary-400:\\s*([^;]+);").matcher(cssStr);
            if (m.find()) secondary = "var(--p-primary-400, " + m.group(1) + ")";

            m = Pattern.compile("--p-surface-0:\\s*([^;]+);").matcher(cssStr);
            if (m.find()) {
                surface = "var(--p-surface-0, " + m.group(1) + ")";
                background = "var(--p-surface-50, #f8fafc)";
            } else {
                m = Pattern.compile("--surface-card:\\s*([^;]+);").matcher(cssStr);
                if (m.find()) surface = "var(--surface-card, " + m.group(1) + ")";
                else {
                    m = Pattern.compile("--bs-body-bg:\\s*([^;]+);").matcher(cssStr);
                    if (m.find()) surface = "var(--bs-body-bg, " + m.group(1) + ")";
                }
                
                m = Pattern.compile("--surface-ground:\\s*([^;]+);").matcher(cssStr);
                if (m.find()) background = "var(--surface-ground, " + m.group(1) + ")";
                else {
                    m = Pattern.compile("--bs-tertiary-bg:\\s*([^;]+);").matcher(cssStr);
                    if (m.find()) background = "var(--bs-tertiary-bg, " + m.group(1) + ")";
                }
            }

            m = Pattern.compile("--p-surface-900:\\s*([^;]+);").matcher(cssStr);
            if (m.find()) onSurface = "var(--p-surface-900, " + m.group(1) + ")";
            else {
                m = Pattern.compile("--text-color:\\s*([^;]+);").matcher(cssStr);
                if (m.find()) onSurface = "var(--text-color, " + m.group(1) + ")";
                else {
                    m = Pattern.compile("--bs-body-color:\\s*([^;]+);").matcher(cssStr);
                    if (m.find()) onSurface = "var(--bs-body-color, " + m.group(1) + ")";
                }
            }

            m = Pattern.compile("font-family:\\s*([^;\\}]+)").matcher(cssStr);
            if (m.find()) {
                fontFamily = m.group(1).trim();
            } else {
                m = Pattern.compile("--bs-body-font-family:\\s*([^;]+);").matcher(cssStr);
                if (m.find()) fontFamily = "var(--bs-body-font-family, " + m.group(1).trim() + ")";
                else {
                    m = Pattern.compile("--bs-font-sans-serif:\\s*([^;]+);").matcher(cssStr);
                    if (m.find()) fontFamily = "var(--bs-font-sans-serif, " + m.group(1).trim() + ")";
                }
            }
        }

        if (customCss.toString().contains("--bs-primary")) {
            customJs.append("document.documentElement.setAttribute('data-bs-theme', 'dark'); ");
            customJs.append("const addBsClasses = (node) => { if(node.querySelectorAll) { ");
            customJs.append("node.querySelectorAll('.espresso-textfield, .espresso-textarea, .espresso-input').forEach(el => el.classList.add('form-control')); ");
            customJs.append("node.querySelectorAll('.espresso-dropdown, .espresso-select').forEach(el => el.classList.add('form-select')); ");
            customJs.append("node.querySelectorAll('.espresso-button').forEach(el => el.classList.add('btn', 'btn-primary')); ");
            customJs.append("node.querySelectorAll('.espresso-checkbox').forEach(el => el.classList.add('form-check-input')); ");
            customJs.append("} }; ");
            customJs.append("document.addEventListener('DOMContentLoaded', () => { ");
            customJs.append("  addBsClasses(document); ");
            customJs.append("  new MutationObserver((mutations) => { mutations.forEach(m => m.addedNodes.forEach(n => addBsClasses(n))); }).observe(document.body, {childList: true, subtree: true}); ");
            customJs.append("}); ");
        }

        String customJsStr = customJs.length() > 0 ? customJs.toString() + "console.log('Scraped Theme scripts injected!');" : "console.log('Scraped Theme Loaded!');";

        String buttonStyle = "border: none; border-radius: 8px; padding: 12px 24px; font-weight: 600; cursor: pointer; transition: all 0.3s ease; background: " + primary + "; color: " + onPrimary + ";";
        String cardStyle = "border-radius: 16px; box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.1); padding: 24px; background-color: " + surface + "; color: " + onSurface + ";";
        
        return "{\n" +
               "  \"name\": \"" + projectName + "\",\n" +
               "  \"primary\": \"" + primary + "\",\n" +
               "  \"secondary\": \"" + secondary + "\",\n" +
               "  \"background\": \"" + background + "\",\n" +
               "  \"surface\": \"" + surface + "\",\n" +
               "  \"onPrimary\": \"" + onPrimary + "\",\n" +
               "  \"onSurface\": \"" + onSurface + "\",\n" +
               "  \"buttonStyle\": \"" + buttonStyle + "\",\n" +
               "  \"cardStyle\": \"" + cardStyle + "\",\n" +
               "  \"containerStyle\": \"padding: 24px; border-radius: 12px; background: transparent;\",\n" +
               "  \"textStyle\": \"font-family: " + fontFamily + "; font-size: 15px; color: " + onSurface + "; line-height: 1.6;\",\n" +
               "  \"customCss\": \"" + customCss.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ") + "\",\n" +
               "  \"customJs\": \"" + customJsStr.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ") + "\"\n" +
               "}";
    }

    private static String resolveCssUrls(String css, String baseUrl) {
        Pattern urlPattern = Pattern.compile("url\\(['\"]?(.*?)['\"]?\\)", Pattern.CASE_INSENSITIVE);
        Matcher m = urlPattern.matcher(css);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String url = m.group(1).trim();
            if (!url.startsWith("data:") && !url.startsWith("http")) {
                try {
                    url = URI.create(baseUrl).resolve(url).toString();
                } catch (Exception e) {}
            }
            m.appendReplacement(sb, "url('" + url + "')");
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
