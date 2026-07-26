package io.jettra.server.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PluginCLI {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: mvn -jettra <command> <plugin-name> [options]");
            return;
        }

        List<String> argList = new ArrayList<>(Arrays.asList(args));
        argList.removeIf(a -> a.equals("-jettra"));
        
        if (argList.isEmpty()) return;

        String command = argList.get(0);
        String pluginName = null;
        String pathStr = null;
        String excludePlugins = null;
        List<String> excludePackages = new ArrayList<>();
        List<String> excludeClasses = new ArrayList<>();
        boolean includeTest = false;

        for (int i = 0; i < argList.size(); i++) {
            String arg = argList.get(i);
            String nextArg = (i + 1 < argList.size()) ? argList.get(i + 1) : null;

            if ("-name".equalsIgnoreCase(arg) || "name".equalsIgnoreCase(arg)) {
                if (nextArg != null) pluginName = nextArg;
            } else if ("-path".equalsIgnoreCase(arg) || "path".equalsIgnoreCase(arg)) {
                if (nextArg != null) pathStr = nextArg;
            } else if ("exclude-plugin".equalsIgnoreCase(arg) || "-exclude-plugin".equalsIgnoreCase(arg)) {
                if (nextArg != null) excludePlugins = nextArg;
            } else if ("exclude-package".equalsIgnoreCase(arg) || "-exclude-package".equalsIgnoreCase(arg)) {
                List<String> tokens = collectOptionTokens(argList, i + 1);
                excludePackages.addAll(parseCommaOrSpaceSeparatedList(tokens));
            } else if ("exclude-class".equalsIgnoreCase(arg) || "-exclude-class".equalsIgnoreCase(arg)) {
                List<String> tokens = collectOptionTokens(argList, i + 1);
                excludeClasses.addAll(parseCommaOrSpaceSeparatedList(tokens));
            } else if ("incluye-test".equalsIgnoreCase(arg) || "-incluye-test".equalsIgnoreCase(arg) ||
                       "include-test".equalsIgnoreCase(arg) || "-include-test".equalsIgnoreCase(arg)) {
                if (nextArg != null && !isKnownOptionKey(nextArg)) {
                    String val = nextArg.trim().toLowerCase();
                    includeTest = val.equals("yes") || val.equals("true") || val.equals("si") || val.equals("y") || val.equals("1");
                } else {
                    includeTest = true;
                }
            }
        }

        // Backwards compatibility if flags are not used (e.g. command pluginName)
        if (pluginName == null && argList.size() > 1 && !argList.get(1).startsWith("-") && !"exclude-plugin".equalsIgnoreCase(argList.get(1))) {
            pluginName = argList.get(1);
        }
        
        if (pathStr == null) {
            pathStr = ".";
        }

        if (pluginName == null && !"list-plugin".equalsIgnoreCase(command)) {
            System.out.println("Plugin name is required.");
            return;
        }

        switch (command) {
            case "generate-plugin":
                generatePlugin(pathStr, pluginName, excludePlugins, excludePackages, excludeClasses, includeTest);
                break;
            case "install-plugin":
                installPlugin(pluginName);
                break;
            case "remove-plugin":
                removePlugin(pluginName);
                break;
            case "list-plugin":
                listPlugins();
                break;
            case "get-plugin":
                getPlugin(pluginName);
                break;
            default:
                System.out.println("Unknown command: " + command);
        }
    }

    private static void generatePlugin(String pathStr, String pluginName, String excludePlugins,
                                       List<String> excludePackages, List<String> excludeClasses, boolean includeTest) {
        Path baseDir = Paths.get(pathStr);
        Path targetDir = baseDir.resolve(pluginName);
        System.out.println("Generating autonomous plugin: " + targetDir.toString());
        if (excludePlugins != null) {
            System.out.println("Excluding plugins: " + excludePlugins);
        }
        if (!excludePackages.isEmpty()) {
            System.out.println("Excluding packages: " + String.join(", ", excludePackages));
        }
        if (!excludeClasses.isEmpty()) {
            System.out.println("Excluding classes: " + String.join(", ", excludeClasses));
        }
        System.out.println("Includes test: " + (includeTest ? "yes" : "no"));

        try {
            if (Files.exists(targetDir)) {
                System.err.println("Target directory " + targetDir.toString() + " already exists!");
                return;
            }
            
            // Map to store extracted versions
            Map<String, String> versions = new HashMap<>();
            
            // Try to extract versions from local pom.xml
            Path localPom = Paths.get("pom.xml");
            if (Files.exists(localPom)) {
                String pomContent = new String(Files.readAllBytes(localPom), StandardCharsets.UTF_8);
                extractVersion(pomContent, "jettra.annotation.version", versions);
                extractVersion(pomContent, "jettra.jwt.version", versions);
                extractVersion(pomContent, "jettra.gprc.version", versions);
                extractVersion(pomContent, "jettra.rules.version", versions);
                extractVersion(pomContent, "jettra.appserver.version", versions);
                extractVersion(pomContent, "jettra.report.version", versions);
                extractVersion(pomContent, "jettra.rest.version", versions);
                extractVersion(pomContent, "jettra.json.version", versions);
                extractVersion(pomContent, "jettra.test.version", versions);
                extractVersion(pomContent, "jettra.flux.version", versions);
            } else {
                System.out.println("Warning: pom.xml not found in current directory. Using default 1.0.0-SNAPSHOT for Jettra dependencies.");
            }

            Files.createDirectories(targetDir);
            String pluginNameLower = pluginName.toLowerCase();
            String packagePath = "io/jettraflux/" + pluginNameLower;
            
            Files.createDirectories(targetDir.resolve("src/main/java/" + packagePath));
            Files.createDirectories(targetDir.resolve("src/main/resources"));
            
            // 1. Generate POM
            String pom = generatePomTemplate(pluginName, pluginNameLower, versions);
            Files.write(targetDir.resolve("pom.xml"), pom.getBytes(StandardCharsets.UTF_8));
            
            // 2. Generate plugin-descriptor.md
            StringBuilder descriptor = new StringBuilder();
            descriptor.append("# Plugin Descriptor for ").append(pluginName).append("\n\n");
            descriptor.append("## Restrictions & Menus\n");
            descriptor.append("Menu definitions (WidgetLet) and layout restrictions should be defined here.\n\n");
            descriptor.append("```java\n");
            
            // Try to extract menu from TemplatePage.java
            boolean extractedMenu = false;
            Path localSrc = Paths.get("src/main/java");
            if (Files.exists(localSrc)) {
                try (java.util.stream.Stream<Path> stream = Files.walk(localSrc)) {
                    Optional<Path> templatePageOpt = stream.filter(p -> p.getFileName().toString().equals("TemplatePage.java")).findFirst();
                    if (templatePageOpt.isPresent()) {
                        List<String> lines = Files.readAllLines(templatePageOpt.get(), StandardCharsets.UTF_8);
                        boolean inMenu = false;
                        List<String> rawLines = new ArrayList<>();
                        List<String> varNames = new ArrayList<>();
                        for (String line : lines) {
                            if (line.contains("WidgetLet") && !line.contains("import ")) {
                                inMenu = true;
                            }
                            if (inMenu && (line.contains("Widget menu = Left.of") || line.contains("Left.of("))) {
                                break; // Stop extraction once we hit the menu creation
                            }
                            if (inMenu) {
                                rawLines.add(line);
                                java.util.regex.Matcher m = java.util.regex.Pattern.compile("WidgetLet\\s+(\\w+)\\s*=").matcher(line);
                                if (m.find()) {
                                    varNames.add(m.group(1));
                                }
                            }
                        }
                        
                        for (String line : rawLines) {
                            String modifiedLine = line;
                            for (String var : varNames) {
                                modifiedLine = modifiedLine.replaceAll("\\b" + java.util.regex.Pattern.quote(var) + "\\b", var + pluginName);
                            }
                            descriptor.append(modifiedLine).append("\n");
                        }
                        extractedMenu = true;
                    }
                }
            }
            if (!extractedMenu) {
                descriptor.append("// Define WidgetLets here.\n");
            }
            descriptor.append("```\n");

            
            // Migration: Copy local src/main/java
            if (Files.exists(localSrc)) {
                System.out.println("Migrating classes from current project...");
                Files.walkFileTree(localSrc, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                        if (file.toString().endsWith(".java")) {
                            Path relative = localSrc.relativize(file);
                            if (!isJavaFileExcluded(relative, file, excludePackages, excludeClasses)) {
                                Path dest = targetDir.resolve("src/main/java").resolve(relative);
                                Files.createDirectories(dest.getParent());
                                String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                                content = Pattern.compile("@InjectProperties\\(\\s*name\\s*=\\s*\"messages(?![-\\w]*" + Pattern.quote(pluginName) + ")([^\"]*)\"\\s*\\)")
                                                 .matcher(content)
                                                 .replaceAll("@InjectProperties(name = \"messages-" + pluginName + "$1\")");
                                content = content.replace("@InjectProperties(name = \"messages\")", "@InjectProperties(name = \"messages-" + pluginName + "\")")
                                                 .replace("@InjectProperties(name=\"messages\")", "@InjectProperties(name=\"messages-" + pluginName + "\")");
                                Files.write(dest, content.getBytes(StandardCharsets.UTF_8));
                            } else {
                                System.out.println("  [Excluded class] " + relative);
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }

            // Migration: Copy local src/main/resources excluding server properties
            Path localRes = Paths.get("src/main/resources");
            if (Files.exists(localRes)) {
                System.out.println("Migrating resources from current project...");
                Files.walkFileTree(localRes, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                        String fileName = file.getFileName().toString();
                        if (!fileName.equals("jettra-config.properties") && !fileName.equals("jettra-rest.properties")) {
                            Path relative = localRes.relativize(file);
                            
                            // Rename messages properties
                            if (fileName.startsWith("messages") && fileName.endsWith(".properties")) {
                                String newFileName = fileName.replace("messages", "messages-" + pluginName);
                                relative = relative.getParent() != null ? relative.getParent().resolve(newFileName) : Paths.get(newFileName);
                            }
                            
                            Path dest = targetDir.resolve("src/main/resources").resolve(relative);
                            Files.createDirectories(dest.getParent());
                            Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }

            // Ensure generic messages.properties is removed and messages-<pluginName>.properties exists
            Path genericMessages = targetDir.resolve("src/main/resources/messages.properties");
            if (Files.exists(genericMessages)) {
                Files.delete(genericMessages);
            }
            Path pluginMessages = targetDir.resolve("src/main/resources/messages-" + pluginName + ".properties");
            if (!Files.exists(pluginMessages)) {
                Files.write(pluginMessages, ("# Properties for plugin " + pluginName + "\ngreeting=Welcome\n").getBytes(StandardCharsets.UTF_8));
            }

            // Migration: Copy tests if includeTest is true
            List<String> migratedTests = new ArrayList<>();
            if (includeTest) {
                Path localTestSrc = Paths.get("src/test/java");
                if (Files.exists(localTestSrc)) {
                    System.out.println("Migrating test classes from current project...");
                    Files.walkFileTree(localTestSrc, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                            if (file.toString().endsWith(".java")) {
                                Path relative = localTestSrc.relativize(file);
                                if (!isJavaFileExcluded(relative, file, excludePackages, excludeClasses)) {
                                    Path dest = targetDir.resolve("src/test/java").resolve(relative);
                                    Files.createDirectories(dest.getParent());
                                    Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                                    migratedTests.add("src/test/java/" + relative.toString().replace('\\', '/'));
                                } else {
                                    System.out.println("  [Excluded test class] " + relative);
                                }
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }

                Path localTestRes = Paths.get("src/test/resources");
                if (Files.exists(localTestRes)) {
                    System.out.println("Migrating test resources from current project...");
                    Files.walkFileTree(localTestRes, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                            Path relative = localTestRes.relativize(file);
                            Path dest = targetDir.resolve("src/test/resources").resolve(relative);
                            Files.createDirectories(dest.getParent());
                            Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
            }

            if (!migratedTests.isEmpty()) {
                descriptor.append("\n## Tests\n");
                for (String tf : migratedTests) {
                    descriptor.append(tf).append("\n");
                }
            }

            Files.write(targetDir.resolve("plugin-descriptor.md"), descriptor.toString().getBytes(StandardCharsets.UTF_8));
            Files.write(targetDir.resolve("src/main/resources/plugin-descriptor.md"), descriptor.toString().getBytes(StandardCharsets.UTF_8));


            // 4. Generate Java Page
            String javaCode = "package io.jettraflux." + pluginNameLower + ";\n\n" +
                              "import io.jettra.core.inject.annotation.InjectProperties;\n" +
                              "import io.jettra.core.server.Page;\n" +
                              "import io.jettra.flux.pages.FluxBaseHandler;\n" +
                              "import io.jettra.flux.core.Widget;\n" +
                              "import io.jettra.flux.widgets.Text;\n" +
                              "import com.sun.net.httpserver.HttpExchange;\n" +
                              "import java.util.Map;\n" +
                              "import java.util.Properties;\n\n" +
                              "@Page(path = \"/" + pluginNameLower + "\")\n" +
                              "public class Main" + pluginName + "Page extends FluxBaseHandler {\n\n" +
                              "    @InjectProperties(name = \"messages-" + pluginName + "\")\n" +
                              "    private Properties msg;\n\n" +
                              "    @Override\n" +
                              "    protected Widget buildUI(HttpExchange exchange, Map<String, String> params, String currentTheme) {\n" +
                              "        String text = msg != null ? msg.getProperty(\"greeting\", \"Welcome\") : \"Welcome\";\n" +
                              "        return Text.of(text);\n" +
                              "    }\n" +
                              "}\n";
            Files.write(targetDir.resolve("src/main/java/" + packagePath + "/Main" + pluginName + "Page.java"), javaCode.getBytes(StandardCharsets.UTF_8));

            System.out.println("Plugin " + pluginName + " generated successfully!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void extractVersion(String pomContent, String tag, Map<String, String> versions) {
        Pattern p = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">");
        Matcher m = p.matcher(pomContent);
        if (m.find()) {
            versions.put(tag, m.group(1));
        } else {
            versions.put(tag, "1.0.0-SNAPSHOT"); // default fallback
        }
    }

    private static String generatePomTemplate(String pluginName, String pluginNameLower, Map<String, String> v) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
               "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
               "    <modelVersion>4.0.0</modelVersion>\n" +
               "    <groupId>io.jettraflux." + pluginNameLower + "</groupId>\n" +
               "    <artifactId>" + pluginName + "</artifactId>\n" +
               "    <packaging>jar</packaging>\n" +
               "    <version>1.0-SNAPSHOT</version>\n" +
               "    <name>" + pluginName + "</name>\n\n" +
               "    <properties>\n" +
               "        <maven.compiler.source>25</maven.compiler.source>\n" +
               "        <maven.compiler.target>25</maven.compiler.target>\n" +
               "        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n" +
               "        <skipTests>true</skipTests>\n" +
               "        <jettra.annotation.version>" + v.get("jettra.annotation.version") + "</jettra.annotation.version>\n" +
               "        <jettra.jwt.version>" + v.get("jettra.jwt.version") + "</jettra.jwt.version>\n" +
               "        <jettra.gprc.version>" + v.get("jettra.gprc.version") + "</jettra.gprc.version>\n" +
               "        <jettra.rules.version>" + v.get("jettra.rules.version") + "</jettra.rules.version>\n" +
               "        <jettra.appserver.version>" + v.get("jettra.appserver.version") + "</jettra.appserver.version>\n" +
               "        <jettra.report.version>" + v.get("jettra.report.version") + "</jettra.report.version>\n" +
               "        <jettra.rest.version>" + v.get("jettra.rest.version") + "</jettra.rest.version>\n" +
               "        <jettra.json.version>" + v.get("jettra.json.version") + "</jettra.json.version>\n" +
               "        <jettra.test.version>" + v.get("jettra.test.version") + "</jettra.test.version>\n" +
               "        <jettra.flux.version>" + v.get("jettra.flux.version") + "</jettra.flux.version>\n" +
               "    </properties>\n\n" +
               "    <dependencies>\n" +
               "        <dependency>\n" +
               "            <groupId>io.jettra</groupId>\n" +
               "            <artifactId>JettraJSON</artifactId>\n" +
               "            <version>${jettra.json.version}</version>\n" +
               "        </dependency>\n" +
               "        <dependency>\n" +
               "            <groupId>io.jettra</groupId>\n" +
               "            <artifactId>JettraAppServer</artifactId>\n" +
               "            <version>${jettra.appserver.version}</version>\n" +
               "        </dependency>\n" +
               "        <dependency>\n" +
               "            <groupId>io.jettra</groupId>\n" +
               "            <artifactId>JettraReport</artifactId>\n" +
               "            <version>${jettra.report.version}</version>\n" +
               "        </dependency>\n" +
               "        <dependency>\n" +
               "            <groupId>io.jettra</groupId>\n" +
               "            <artifactId>JettraRules</artifactId>\n" +
               "            <version>${jettra.rules.version}</version>\n" +
               "        </dependency>\n" +
               "        <dependency>\n" +
               "            <groupId>io.jettra</groupId>\n" +
               "            <artifactId>JettraJWT</artifactId>\n" +
               "            <version>${jettra.jwt.version}</version>\n" +
               "        </dependency>\n" +
               "        <dependency>\n" +
               "            <groupId>io.jettra</groupId>\n" +
               "            <artifactId>JettraRest</artifactId>\n" +
               "            <version>${jettra.rest.version}</version>\n" +
               "        </dependency>\n" +
               "        <dependency>\n" +
               "            <groupId>io.jettra</groupId>\n" +
               "            <artifactId>JettraAnnotation</artifactId>\n" +
               "            <version>${jettra.annotation.version}</version>\n" +
               "        </dependency>\n" +
               "        <dependency>\n" +
               "            <groupId>io.jettra</groupId>\n" +
               "            <artifactId>JettraTest</artifactId>\n" +
               "            <version>${jettra.test.version}</version>\n" +
               "        </dependency>\n" +
               "        <dependency>\n" +
               "            <groupId>io.jettra</groupId>\n" +
               "            <artifactId>JettraFlux</artifactId>\n" +
               "            <version>${jettra.flux.version}</version>\n" +
               "        </dependency>\n" +
               "    </dependencies>\n" +
               "    <build>\n" +
               "        <plugins>\n" +
               "            <plugin>\n" +
               "                <groupId>org.apache.maven.plugins</groupId>\n" +
               "                <artifactId>maven-compiler-plugin</artifactId>\n" +
               "                <version>3.11.0</version>\n" +
               "                <configuration>\n" +
               "                    <source>${maven.compiler.source}</source>\n" +
               "                    <target>${maven.compiler.target}</target>\n" +
               "                    <annotationProcessorPaths>\n" +
               "                        <path>\n" +
               "                            <groupId>io.jettra</groupId>\n" +
               "                            <artifactId>JettraAnnotation</artifactId>\n" +
               "                            <version>${jettra.annotation.version}</version>\n" +
               "                        </path>\n" +
               "                    </annotationProcessorPaths>\n" +
               "                </configuration>\n" +
               "            </plugin>\n" +
               "            <plugin>\n" +
               "                <groupId>org.apache.maven.plugins</groupId>\n" +
               "                <artifactId>maven-shade-plugin</artifactId>\n" +
               "                <version>3.5.1</version>\n" +
               "                <executions>\n" +
               "                    <execution>\n" +
               "                        <phase>package</phase>\n" +
               "                        <goals>\n" +
               "                            <goal>shade</goal>\n" +
               "                        </goals>\n" +
               "                        <configuration>\n" +
               "                            <createDependencyReducedPom>false</createDependencyReducedPom>\n" +
               "                        </configuration>\n" +
               "                    </execution>\n" +
               "                </executions>\n" +
               "            </plugin>\n" +
               "            <plugin>\n" +
               "                <groupId>org.apache.maven.plugins</groupId>\n" +
               "                <artifactId>maven-jar-plugin</artifactId>\n" +
               "                <version>3.3.0</version>\n" +
               "                <configuration>\n" +
               "                    <excludes>\n" +
               "                        <exclude>**/App.class</exclude>\n" +
               "                        <exclude>**/DashboardPage.class</exclude>\n" +
               "                        <exclude>**/ForgotPasswordPage.class</exclude>\n" +
               "                        <exclude>**/LoginPage.class</exclude>\n" +
               "                        <exclude>**/TemplatePage.class</exclude>\n" +
               "                    </excludes>\n" +
               "                </configuration>\n" +
               "            </plugin>\n" +
               "        </plugins>\n" +
               "    </build>\n" +
               "</project>\n";
    }

    private static void installPlugin(String pluginPathStr) {
        Path pluginPath = Paths.get(pluginPathStr);
        String pluginName = pluginPath.getFileName().toString();
        if (pluginName.toLowerCase().endsWith(".jar")) {
            pluginName = pluginName.substring(0, pluginName.length() - 4);
        }

        System.out.println("Installing plugin: " + pluginName);

        List<String> descLines = new ArrayList<>();

        if (Files.isDirectory(pluginPath)) {
            Path pluginPom = pluginPath.resolve("pom.xml");
            if (Files.exists(pluginPom)) {
                System.out.println("Building plugin from local directory...");
                runCommand(new String[]{"mvn", "clean", "install"}, pluginPath);

                Path localPom = Paths.get("pom.xml");
                if (Files.exists(localPom)) {
                    try {
                        String pPomContent = new String(Files.readAllBytes(pluginPom), StandardCharsets.UTF_8);
                        String groupId = extractTag(pPomContent, "groupId");
                        String artifactId = extractTag(pPomContent, "artifactId");
                        String version = extractTag(pPomContent, "version");

                        if (groupId != null && artifactId != null && version != null) {
                            String dependency = "        <dependency>\n" +
                                                "            <groupId>" + groupId + "</groupId>\n" +
                                                "            <artifactId>" + artifactId + "</artifactId>\n" +
                                                "            <version>" + version + "</version>\n" +
                                                "        </dependency>\n";
                            
                            String lPomContent = new String(Files.readAllBytes(localPom), StandardCharsets.UTF_8);
                            if (!lPomContent.contains("<artifactId>" + artifactId + "</artifactId>")) {
                                lPomContent = lPomContent.replaceFirst("</dependencies>", dependency + "    </dependencies>");
                                Files.write(localPom, lPomContent.getBytes(StandardCharsets.UTF_8));
                                System.out.println("Added dependency to pom.xml.");
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error injecting dependency: " + e.getMessage());
                    }
                }
            }

            Path descriptor = pluginPath.resolve("plugin-descriptor.md");
            if (Files.exists(descriptor)) {
                try {
                    descLines = Files.readAllLines(descriptor, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    System.err.println("Error reading descriptor file: " + e.getMessage());
                }
            }
        }

        if (descLines.isEmpty()) {
            descLines = findAndReadPluginDescriptor(pluginPathStr);
        }

        if (descLines.isEmpty()) {
            System.out.println("Warning: plugin-descriptor.md not found for plugin: " + pluginName);
            return;
        }

        boolean inCode = false;
        List<String> codeLines = new ArrayList<>();
        List<String> variables = new ArrayList<>();

        for (String line : descLines) {
            if (line.startsWith("```java")) {
                inCode = true;
                continue;
            }
            if (inCode && line.startsWith("```")) {
                inCode = false;
                continue;
            }
            if (inCode) {
                if (line.contains("Widget menu = Left.of") || (line.contains("Left.of(") && line.contains("Widget menu"))) {
                    continue;
                }
                codeLines.add(line);
                Matcher m = Pattern.compile("WidgetLet\\s+(\\w+)\\s*=").matcher(line);
                if (m.find()) {
                    variables.add(m.group(1));
                }
            }
        }

        if (!codeLines.isEmpty()) {
            try {
                injectIntoTemplatePage(pluginName, codeLines, variables);
            } catch (Exception e) {
                System.err.println("Error injecting into TemplatePage.java: " + e.getMessage());
            }
        } else {
            System.out.println("No menu code block (```java ... ```) found in plugin-descriptor.md for plugin: " + pluginName);
        }
    }

    private static String extractTag(String xml, String tag) {
        Matcher m = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">").matcher(xml);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private static void injectIntoTemplatePage(String pluginName, List<String> codeLines, List<String> variables) throws IOException {
        Path localSrc = Paths.get("src/main/java");
        if (!Files.exists(localSrc)) return;

        try (java.util.stream.Stream<Path> stream = Files.walk(localSrc)) {
            Optional<Path> templatePageOpt = stream.filter(p -> p.getFileName().toString().equals("TemplatePage.java")).findFirst();
            if (templatePageOpt.isPresent()) {
                Path tp = templatePageOpt.get();
                List<String> lines = Files.readAllLines(tp, StandardCharsets.UTF_8);
                
                String startTag = "Start Plugin: " + pluginName;
                String endTag = "End Plugin: " + pluginName;

                int existingStartHeader = -1;
                int existingStartLine = -1;
                int existingEndLine = -1;
                int existingEndFooter = -1;

                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line.contains(startTag)) {
                        existingStartLine = i;
                        if (i > 0 && lines.get(i - 1).trim().equals("/**")) {
                            existingStartHeader = i - 1;
                        }
                    }
                    if (line.contains(endTag)) {
                        existingEndLine = i;
                        if (i + 1 < lines.size() && lines.get(i + 1).trim().equals("**/")) {
                            existingEndFooter = i + 1;
                        }
                    }
                }

                // Filter out any Left.of initialization from codeLines to protect Widget menu = Left.of
                List<String> cleanCodeLines = new ArrayList<>();
                for (String cl : codeLines) {
                    if (!cl.contains("Widget menu = Left.of") && !(cl.contains("Left.of(") && cl.contains("Widget menu"))) {
                        cleanCodeLines.add(cl);
                    }
                }

                List<String> pluginSection = new ArrayList<>();
                pluginSection.add("/**");
                pluginSection.add("Start Plugin: " + pluginName);
                pluginSection.add("**/");
                pluginSection.addAll(cleanCodeLines);
                pluginSection.add("/**");
                pluginSection.add("End Plugin: " + pluginName);
                pluginSection.add("**/");

                List<String> newLines = new ArrayList<>();

                if (existingStartLine != -1 && existingEndLine != -1) {
                    int removeStart = (existingStartHeader != -1) ? existingStartHeader : existingStartLine;
                    int removeEnd = (existingEndFooter != -1) ? existingEndFooter : existingEndLine;

                    for (int i = 0; i < lines.size(); i++) {
                        if (i == removeStart) {
                            newLines.addAll(pluginSection);
                        }
                        if (i >= removeStart && i <= removeEnd) {
                            continue;
                        }
                        newLines.add(lines.get(i));
                    }
                } else {
                    boolean inserted = false;
                    for (String line : lines) {
                        if (!inserted && (line.contains("Widget menu = Left.of(") || line.contains("Left.of("))) {
                            newLines.addAll(pluginSection);
                            newLines.add("");
                            inserted = true;
                        }
                        newLines.add(line);
                    }
                    if (!inserted) {
                        int insertPoint = -1;
                        for (int i = newLines.size() - 1; i >= 0; i--) {
                            String l = newLines.get(i).trim();
                            if (l.startsWith("return Scaffold") || l.startsWith("return Column") || l.startsWith("return ")) {
                                insertPoint = i;
                                break;
                            }
                        }
                        if (insertPoint == -1) {
                            for (int i = newLines.size() - 1; i >= 0; i--) {
                                if (newLines.get(i).trim().equals("}")) {
                                    insertPoint = i;
                                    break;
                                }
                            }
                        }
                        if (insertPoint == -1) {
                            insertPoint = newLines.size();
                        }
                        List<String> toInsert = new ArrayList<>();
                        toInsert.addAll(pluginSection);
                        toInsert.add("");
                        newLines.addAll(insertPoint, toInsert);
                    }
                }

                int leftOfIndex = -1;
                for (int i = 0; i < newLines.size(); i++) {
                    if (newLines.get(i).contains("Left.of(")) {
                        leftOfIndex = i;
                        break;
                    }
                }

                if (leftOfIndex == -1) {
                    int insertPoint = -1;
                    for (int i = newLines.size() - 1; i >= 0; i--) {
                        String l = newLines.get(i).trim();
                        if (l.startsWith("return Scaffold") || l.startsWith("return Column") || l.startsWith("return ")) {
                            insertPoint = i;
                            break;
                        }
                    }
                    if (insertPoint == -1) {
                        for (int i = newLines.size() - 1; i >= 0; i--) {
                            if (newLines.get(i).trim().equals("}")) {
                                insertPoint = i;
                                break;
                            }
                        }
                    }
                    if (insertPoint == -1) {
                        insertPoint = newLines.size();
                    }

                    List<String> menuDecl = new ArrayList<>();
                    menuDecl.add("        Widget menu = Left.of(");
                    menuDecl.add("                SidebarLogo.of(Icon.LAYER_GROUP, \"Ocean\"),");
                    menuDecl.add("                SidebarCategory.of(\"Navigation\")" + (variables.isEmpty() ? "" : ","));
                    for (int v = 0; v < variables.size(); v++) {
                        String suffix = (v == variables.size() - 1) ? "" : ",";
                        menuDecl.add("                " + variables.get(v) + suffix);
                    }
                    menuDecl.add("        ).modifier(new io.jettra.flux.core.Modifier().cssClass(\"professional-left\"));");
                    menuDecl.add("");

                    newLines.addAll(insertPoint, menuDecl);
                    leftOfIndex = insertPoint;
                }

                if (leftOfIndex != -1 && !variables.isEmpty()) {
                    int searchEndIndex = Math.min(leftOfIndex + 100, newLines.size());

                    List<String> varsToAdd = new ArrayList<>();
                    for (String var : variables) {
                        boolean alreadyPresent = false;
                        for (int i = leftOfIndex; i < searchEndIndex; i++) {
                            if (Pattern.compile("\\b" + Pattern.quote(var) + "\\b").matcher(newLines.get(i)).find()) {
                                alreadyPresent = true;
                                break;
                            }
                        }
                        if (!alreadyPresent) {
                            varsToAdd.add(var);
                        }
                    }

                    if (!varsToAdd.isEmpty()) {
                        int endIndex = -1;
                        for (int i = leftOfIndex; i < searchEndIndex; i++) {
                            String trimmed = newLines.get(i).trim();
                            if (trimmed.startsWith(").modifier") || trimmed.equals(");") || trimmed.equals(")") || trimmed.startsWith(");") || trimmed.startsWith(")")) {
                                endIndex = i;
                                break;
                            }
                        }

                        if (endIndex != -1) {
                            if (endIndex > leftOfIndex) {
                                int lastCodeIndex = endIndex - 1;
                                while (lastCodeIndex > leftOfIndex && newLines.get(lastCodeIndex).trim().isEmpty()) {
                                    lastCodeIndex--;
                                }
                                if (lastCodeIndex >= leftOfIndex) {
                                    String prevLine = newLines.get(lastCodeIndex);
                                    String prevTrimmed = prevLine.trim();
                                    if (!prevTrimmed.isEmpty() && !prevTrimmed.endsWith("(") && !prevTrimmed.endsWith(",") && !prevLine.contains("Left.of(")) {
                                        newLines.set(lastCodeIndex, prevLine + ",");
                                    }
                                }
                            }
                            
                            for (int v = 0; v < varsToAdd.size(); v++) {
                                String var = varsToAdd.get(v);
                                String suffix = (v == varsToAdd.size() - 1) ? "" : ",";
                                String indent = "                ";
                                newLines.add(endIndex + v, indent + var + suffix);
                            }
                        } else {
                            for (String var : varsToAdd) {
                                String leftLine = newLines.get(leftOfIndex);
                                String trimmed = leftLine.trim();
                                if (trimmed.endsWith("Left.of(") || trimmed.endsWith("Left.of( ")) {
                                    String indent = "                ";
                                    if (leftLine.contains("Widget menu")) {
                                        indent = leftLine.substring(0, leftLine.indexOf("Widget menu")) + "                ";
                                    }
                                    newLines.add(leftOfIndex + 1, indent + var + ",");
                                } else {
                                    newLines.set(leftOfIndex, leftLine.replace("Left.of(", "Left.of(" + var + ", "));
                                }
                            }
                        }
                    }
                }

                Files.write(tp, newLines, StandardCharsets.UTF_8);
                System.out.println("Injected plugin menu section for '" + pluginName + "' into TemplatePage.java successfully.");
            } else {
                System.out.println("TemplatePage.java not found in src/main/java");
            }
        }
    }

    private static List<String> findAndReadPluginDescriptor(String target) {
        Path p = Paths.get(target);

        if (Files.isDirectory(p)) {
            Path descriptor = p.resolve("plugin-descriptor.md");
            if (Files.exists(descriptor)) {
                try {
                    return Files.readAllLines(descriptor, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    System.err.println("Error reading plugin-descriptor.md from directory: " + e.getMessage());
                }
            }
        }

        if (Files.isRegularFile(p) && target.toLowerCase().endsWith(".jar")) {
            List<String> lines = readDescriptorFromJar(p);
            if (lines != null) return lines;
        }

        String userHome = System.getProperty("user.home");
        Path m2Dir = Paths.get(userHome, ".m2", "repository");
        if (Files.exists(m2Dir)) {
            try (java.util.stream.Stream<Path> stream = Files.walk(m2Dir)) {
                Optional<Path> jarOpt = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        String tLower = target.toLowerCase();
                        return name.endsWith(".jar") && name.contains(tLower) && !name.endsWith("-sources.jar") && !name.endsWith("-javadoc.jar");
                    })
                    .findFirst();

                if (jarOpt.isPresent()) {
                    System.out.println("Found plugin JAR: " + jarOpt.get());
                    List<String> lines = readDescriptorFromJar(jarOpt.get());
                    if (lines != null) return lines;
                }
            } catch (IOException e) {
                System.err.println("Error searching ~/.m2/repository: " + e.getMessage());
            }
        }

        return Collections.emptyList();
    }

    private static List<String> readDescriptorFromJar(Path jarPath) {
        try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jarPath.toFile())) {
            java.util.jar.JarEntry entry = jarFile.getJarEntry("plugin-descriptor.md");
            if (entry == null) {
                entry = jarFile.getJarEntry("META-INF/plugin-descriptor.md");
            }
            if (entry != null) {
                try (java.io.InputStream is = jarFile.getInputStream(entry);
                     java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, StandardCharsets.UTF_8))) {
                    List<String> lines = new ArrayList<>();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lines.add(line);
                    }
                    return lines;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading JAR file: " + e.getMessage());
        }
        return null;
    }

    private static void removePlugin(String pluginName) {
        System.out.println("Removing plugin configuration for: " + pluginName);

        // 1. Remove dependency from local pom.xml
        removeFromPomXml(pluginName);

        // 2. Remove section and variables from TemplatePage.java
        removeFromTemplatePage(pluginName);

        // 3. Remove test classes
        removeTestsFromProject(pluginName);
    }

    private static void removeTestsFromProject(String pluginName) {
        List<String> descLines = findAndReadPluginDescriptor(pluginName);
        if (descLines.isEmpty()) {
            return;
        }
        boolean inTests = false;
        List<String> testFiles = new ArrayList<>();
        for (String line : descLines) {
            line = line.trim();
            if (line.startsWith("## Tests")) {
                inTests = true;
                continue;
            } else if (inTests && line.startsWith("## ")) {
                inTests = false;
            }
            if (inTests && !line.isEmpty()) {
                testFiles.add(line);
            }
        }
        if (!testFiles.isEmpty()) {
            System.out.println("Removing test classes associated with plugin: " + pluginName);
            for (String tf : testFiles) {
                Path p = Paths.get(tf);
                if (Files.exists(p)) {
                    try {
                        Files.delete(p);
                        System.out.println("  Deleted test file: " + p);
                    } catch (IOException e) {
                        System.err.println("  Failed to delete test file " + p + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    private static void removeFromPomXml(String pluginName) {
        Path localPom = Paths.get("pom.xml");
        if (!Files.exists(localPom)) {
            System.out.println("No pom.xml found in current directory.");
            return;
        }

        try {
            String pomContent = new String(Files.readAllBytes(localPom), StandardCharsets.UTF_8);
            String pluginNameLower = pluginName.toLowerCase();

            Pattern pattern = Pattern.compile(
                "(?s)\\s*<dependency>\\s*<groupId>[^<]+</groupId>\\s*<artifactId>(" + Pattern.quote(pluginName) + "|" + Pattern.quote(pluginNameLower) + ")</artifactId>\\s*<version>[^<]+</version>\\s*</dependency>"
            );

            Matcher matcher = pattern.matcher(pomContent);
            if (matcher.find()) {
                String updatedPom = matcher.replaceAll("");
                Files.write(localPom, updatedPom.getBytes(StandardCharsets.UTF_8));
                System.out.println("Removed dependency '" + pluginName + "' from pom.xml.");
            } else {
                System.out.println("Dependency '" + pluginName + "' not found in pom.xml.");
            }
        } catch (Exception e) {
            System.err.println("Error removing dependency from pom.xml: " + e.getMessage());
        }
    }

    private static void removeFromTemplatePage(String pluginName) {
        Path localSrc = Paths.get("src/main/java");
        if (!Files.exists(localSrc)) return;

        try (java.util.stream.Stream<Path> stream = Files.walk(localSrc)) {
            Optional<Path> templatePageOpt = stream.filter(p -> p.getFileName().toString().equals("TemplatePage.java")).findFirst();
            if (!templatePageOpt.isPresent()) {
                System.out.println("TemplatePage.java not found in src/main/java");
                return;
            }

            Path tp = templatePageOpt.get();
            List<String> lines = Files.readAllLines(tp, StandardCharsets.UTF_8);
            
            String startTag = "Start Plugin: " + pluginName;
            String endTag = "End Plugin: " + pluginName;

            int existingStartHeader = -1;
            int existingStartLine = -1;
            int existingEndLine = -1;
            int existingEndFooter = -1;

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains(startTag)) {
                    existingStartLine = i;
                    if (i > 0 && lines.get(i - 1).trim().equals("/**")) {
                        existingStartHeader = i - 1;
                    }
                }
                if (line.contains(endTag)) {
                    existingEndLine = i;
                    if (i + 1 < lines.size() && lines.get(i + 1).trim().equals("**/")) {
                        existingEndFooter = i + 1;
                    }
                }
            }

            if (existingStartLine == -1 || existingEndLine == -1) {
                System.out.println("Plugin section for '" + pluginName + "' not found in TemplatePage.java.");
                return;
            }

            int removeStart = (existingStartHeader != -1) ? existingStartHeader : existingStartLine;
            int removeEnd = (existingEndFooter != -1) ? existingEndFooter : existingEndLine;

            List<String> removedVariables = new ArrayList<>();
            for (int i = removeStart; i <= removeEnd; i++) {
                Matcher m = Pattern.compile("WidgetLet\\s+(\\w+)\\s*=").matcher(lines.get(i));
                if (m.find()) {
                    removedVariables.add(m.group(1));
                }
            }

            List<String> newLines = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                if (i >= removeStart && i <= removeEnd) {
                    continue;
                }
                newLines.add(lines.get(i));
            }

            if (!removedVariables.isEmpty()) {
                int leftOfIndex = -1;
                for (int i = 0; i < newLines.size(); i++) {
                    if (newLines.get(i).contains("Left.of(")) {
                        leftOfIndex = i;
                        break;
                    }
                }

                if (leftOfIndex != -1) {
                    for (String var : removedVariables) {
                        int searchEndIndex = Math.min(leftOfIndex + 35, newLines.size());
                        for (int i = leftOfIndex; i < searchEndIndex && i < newLines.size(); i++) {
                            String line = newLines.get(i);
                            if (Pattern.compile("\\b" + Pattern.quote(var) + "\\b").matcher(line).find()) {
                                String trimmed = line.trim();
                                if (trimmed.equals(var) || trimmed.equals(var + ",") || trimmed.equals(var + " ,")) {
                                    newLines.remove(i);
                                    i--;
                                    searchEndIndex--;
                                } else {
                                    String updated = line.replaceAll("\\b" + Pattern.quote(var) + "\\s*,\\s*", "")
                                                        .replaceAll(",\\s*\\b" + Pattern.quote(var) + "\\b", "")
                                                        .replaceAll("\\b" + Pattern.quote(var) + "\\b", "");
                                    if (updated.trim().isEmpty()) {
                                        newLines.remove(i);
                                        i--;
                                        searchEndIndex--;
                                    } else {
                                        newLines.set(i, updated);
                                    }
                                }
                            }
                        }
                    }
                }


                // Clean up any dangling trailing comma before the closing parenthesis
                if (leftOfIndex != -1) {
                    int endIndex = -1;
                    int searchEndIndex = Math.min(leftOfIndex + 100, newLines.size());
                    for (int i = leftOfIndex; i < searchEndIndex; i++) {
                        String trimmed = newLines.get(i).trim();
                        if (trimmed.startsWith(").modifier") || trimmed.equals(");") || trimmed.equals(")") || trimmed.startsWith(");") || trimmed.startsWith(")")) {
                            endIndex = i;
                            break;
                        }
                    }
                    if (endIndex != -1) {
                        for (int i = endIndex - 1; i >= leftOfIndex; i--) {
                            String line = newLines.get(i);
                            String trimmed = line.trim();
                            if (!trimmed.isEmpty()) {
                                if (trimmed.endsWith(",")) {
                                    int commaIdx = line.lastIndexOf(',');
                                    newLines.set(i, line.substring(0, commaIdx) + line.substring(commaIdx + 1));
                                }
                                break;
                            }
                        }
                    }
                }
            }

            Files.write(tp, newLines, StandardCharsets.UTF_8);
            System.out.println("Removed plugin menu section for '" + pluginName + "' from TemplatePage.java successfully.");
        } catch (Exception e) {
            System.err.println("Error removing plugin from TemplatePage.java: " + e.getMessage());
        }
    }

    private static void runCommand(String[] cmd, Path workDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(workDir.toFile());
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<String> collectOptionTokens(List<String> argList, int startIndex) {
        List<String> tokens = new ArrayList<>();
        for (int i = startIndex; i < argList.size(); i++) {
            String token = argList.get(i);
            if (isKnownOptionKey(token)) {
                break;
            }
            tokens.add(token);
        }
        return tokens;
    }

    private static boolean isKnownOptionKey(String token) {
        if (token == null) return false;
        String t = token.toLowerCase();
        return t.equals("-name") || t.equals("name") ||
               t.equals("-path") || t.equals("path") ||
               t.equals("exclude-plugin") || t.equals("-exclude-plugin") ||
               t.equals("exclude-package") || t.equals("-exclude-package") ||
               t.equals("exclude-class") || t.equals("-exclude-class") ||
               t.equals("incluye-test") || t.equals("-incluye-test") ||
               t.equals("include-test") || t.equals("-include-test");
    }

    private static List<String> parseCommaOrSpaceSeparatedList(List<String> tokens) {
        List<String> result = new ArrayList<>();
        String joined = String.join(" ", tokens);
        String[] parts = joined.split("[,\\s]+");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private static boolean isJavaFileExcluded(Path relativePath, Path file, List<String> excludePackages, List<String> excludeClasses) {
        Path parent = relativePath.getParent();
        String pkg = (parent == null) ? "" : parent.toString().replace('/', '.').replace('\\', '.');

        for (String exPkg : excludePackages) {
            String cleanPkg = exPkg.replace('/', '.').replace('\\', '.').replaceAll("^\\.+|\\.+$", "");
            if (!cleanPkg.isEmpty()) {
                if (pkg.equalsIgnoreCase(cleanPkg) || pkg.toLowerCase().startsWith(cleanPkg.toLowerCase() + ".")) {
                    return true;
                }
            }
        }

        String fileName = file.getFileName().toString();
        String fileNameNoExt = fileName.endsWith(".java") ? fileName.substring(0, fileName.length() - 5) : fileName;
        String relPathStr = relativePath.toString().replace('\\', '/');
        String fullClassName = pkg.isEmpty() ? fileNameNoExt : pkg + "." + fileNameNoExt;

        for (String exClass : excludeClasses) {
            String cleanClass = exClass.trim();
            if (cleanClass.isEmpty()) continue;

            if (fileName.equalsIgnoreCase(cleanClass) ||
                fileNameNoExt.equalsIgnoreCase(cleanClass) ||
                relPathStr.equalsIgnoreCase(cleanClass) ||
                relPathStr.equalsIgnoreCase(cleanClass + ".java") ||
                fullClassName.equalsIgnoreCase(cleanClass)) {
                return true;
            }
        }

        return false;
    }

    private static String fetchUrlContent(String urlStr) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .followRedirects(java.net.http.HttpClient.Redirect.ALWAYS)
                    .build();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(urlStr))
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                return response.body();
            }
        } catch (Exception e) {
            // Ignore HTTP errors for fallback
        }
        return null;
    }

    private static String fetchGitHubFile(String relativePath) {
        String mainUrl = "https://raw.githubusercontent.com/avbravo/jettrahub/main/" + relativePath;
        String content = fetchUrlContent(mainUrl);
        if (content == null) {
            String masterUrl = "https://raw.githubusercontent.com/avbravo/jettrahub/master/" + relativePath;
            content = fetchUrlContent(masterUrl);
        }
        return content;
    }

    private static void listPlugins() {
        System.out.println("Connecting to https://github.com/avbravo/jettrahub ...");
        String markdown = fetchGitHubFile("README.md");
        if (markdown == null || markdown.isBlank()) {
            System.err.println("Could not fetch README.md from https://github.com/avbravo/jettrahub");
            return;
        }

        String[] lines = markdown.split("\\r?\\n");
        List<String[]> tableRows = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                if (trimmed.matches("^\\|[\\s\\-:\\|]+\\|$")) {
                    continue; // Skip markdown separator row |---|---|
                }
                String[] parts = trimmed.split("\\|");
                List<String> cells = new ArrayList<>();
                for (String part : parts) {
                    cells.add(part.trim());
                }
                if (!cells.isEmpty() && cells.get(0).isEmpty()) {
                    cells.remove(0);
                }
                if (!cells.isEmpty() && cells.get(cells.size() - 1).isEmpty()) {
                    cells.remove(cells.size() - 1);
                }
                if (cells.size() >= 4) {
                    tableRows.add(cells.toArray(new String[0]));
                }
            }
        }

        if (tableRows.isEmpty()) {
            System.out.println("No plugin table found in README.md");
            return;
        }

        System.out.println("\nAvailable Plugins (JettraHub):");
        System.out.println("====================================================================================================");

        int count = 0;
        for (String[] row : tableRows) {
            String col0 = row.length > 0 ? row[0] : "";
            String col1 = row.length > 1 ? row[1] : "";
            String col2 = row.length > 2 ? row[2] : "";
            String col3 = row.length > 3 ? row[3] : "";
            String col4 = row.length > 4 ? row[4] : "";

            if (col0.equalsIgnoreCase("#") || col1.equalsIgnoreCase("Plugin")) {
                continue; // Skip header row
            }
            if (col1.isBlank()) {
                continue; // Skip empty rows
            }

            // Clean markdown links e.g. [url](url) -> url
            if (col3.startsWith("[") && col3.contains("](") && col3.endsWith(")")) {
                int start = col3.indexOf("](");
                col3 = col3.substring(start + 2, col3.length() - 1);
            }

            count++;
            System.out.println("  " + col0 + ". " + col1);
            if (!col2.isBlank()) System.out.println("     Descripción : " + col2);
            if (!col3.isBlank()) System.out.println("     URL         : " + col3);
            if (!col4.isBlank()) System.out.println("     Autor       : " + col4);
            System.out.println("----------------------------------------------------------------------------------------------------");
        }

        if (count == 0) {
            System.out.println("No plugins available.");
        }
        System.out.println();
    }

    private static void getPlugin(String pluginName) {
        System.out.println("Fetching plugin configuration for '" + pluginName + "' from https://github.com/avbravo/jettrahub ...");
        String jsonContent = fetchGitHubFile(pluginName + ".json");
        if (jsonContent == null || jsonContent.isBlank()) {
            System.err.println("Could not fetch plugin definition '" + pluginName + ".json' from https://github.com/avbravo/jettrahub");
            return;
        }

        Path localPom = Paths.get("pom.xml");
        if (!Files.exists(localPom)) {
            System.err.println("No pom.xml found in current directory.");
            return;
        }

        try {
            String pomContent = new String(Files.readAllBytes(localPom), StandardCharsets.UTF_8);

            // Parse repository
            String repoObj = extractJsonObject(jsonContent, "repository");
            if (repoObj != null) {
                String repoId = extractJsonField(repoObj, "id");
                String repoUrl = extractJsonField(repoObj, "url");

                if (repoId != null && repoUrl != null) {
                    boolean repoExists = pomContent.contains("<id>" + repoId + "</id>") || pomContent.contains("<url>" + repoUrl + "</url>");
                    if (!repoExists) {
                        String repoXml = "        <repository>\n" +
                                         "            <id>" + repoId + "</id>\n" +
                                         "            <url>" + repoUrl + "</url>\n" +
                                         "        </repository>\n";
                        if (pomContent.contains("</repositories>")) {
                            pomContent = pomContent.replaceFirst("</repositories>", repoXml + "    </repositories>");
                        } else if (pomContent.contains("<dependencies>")) {
                            String reposXml = "    <repositories>\n" + repoXml + "    </repositories>\n\n";
                            pomContent = pomContent.replaceFirst("<dependencies>", reposXml + "    <dependencies>");
                        } else {
                            String reposXml = "    <repositories>\n" + repoXml + "    </repositories>\n";
                            pomContent = pomContent.replaceFirst("</project>", reposXml + "</project>");
                        }
                        System.out.println("Added repository '" + repoId + "' (" + repoUrl + ") to pom.xml.");
                    } else {
                        System.out.println("Repository '" + repoId + "' already exists in pom.xml.");
                    }
                }
            }

            // Parse dependency
            String depObj = extractJsonObject(jsonContent, "dependency");
            if (depObj != null) {
                String groupId = extractJsonField(depObj, "groupId");
                String artifactId = extractJsonField(depObj, "artifactId");
                String newVersion = extractJsonField(depObj, "version");

                if (groupId != null && artifactId != null && newVersion != null) {
                    Pattern depPattern = Pattern.compile("(?s)<dependency>\\s*<groupId>\\s*" + Pattern.quote(groupId) + "\\s*</groupId>\\s*<artifactId>\\s*" + Pattern.quote(artifactId) + "\\s*</artifactId>\\s*<version>\\s*(.*?)\\s*</version>\\s*</dependency>");
                    Matcher depMatcher = depPattern.matcher(pomContent);

                    if (depMatcher.find()) {
                        String currentVersion = depMatcher.group(1).trim();
                        if (isNewerVersion(newVersion, currentVersion)) {
                            String updatedDep = "<dependency>\n            <groupId>" + groupId + "</groupId>\n            <artifactId>" + artifactId + "</artifactId>\n            <version>" + newVersion + "</version>\n        </dependency>";
                            pomContent = depMatcher.replaceFirst(Matcher.quoteReplacement(updatedDep));
                            System.out.println("Updated dependency '" + groupId + ":" + artifactId + "' version from '" + currentVersion + "' to '" + newVersion + "' in pom.xml.");
                        } else {
                            System.out.println("Dependency '" + groupId + ":" + artifactId + "' version '" + currentVersion + "' is up to date (JSON version: '" + newVersion + "').");
                        }
                    } else {
                        String depXml = "        <dependency>\n" +
                                        "            <groupId>" + groupId + "</groupId>\n" +
                                        "            <artifactId>" + artifactId + "</artifactId>\n" +
                                        "            <version>" + newVersion + "</version>\n" +
                                        "        </dependency>\n";
                        if (pomContent.contains("</dependencies>")) {
                            pomContent = pomContent.replaceFirst("</dependencies>", depXml + "    </dependencies>");
                        } else {
                            String depsXml = "    <dependencies>\n" + depXml + "    </dependencies>\n";
                            pomContent = pomContent.replaceFirst("</project>", depsXml + "</project>");
                        }
                        System.out.println("Added dependency '" + groupId + ":" + artifactId + ":" + newVersion + "' to pom.xml.");
                    }
                }
            }

            Files.write(localPom, pomContent.getBytes(StandardCharsets.UTF_8));
            System.out.println("pom.xml updated successfully for plugin '" + pluginName + "'.");

        } catch (Exception e) {
            System.err.println("Error updating pom.xml: " + e.getMessage());
        }
    }

    private static String extractJsonField(String json, String fieldName) {
        if (json == null) return null;
        Pattern p = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"?([^\",\\}\\s]+)\"?");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private static String extractJsonObject(String json, String objectName) {
        if (json == null) return null;
        Pattern p = Pattern.compile("\"" + Pattern.quote(objectName) + "\"\\s*:\\s*(\\{[^\\}]*\\})");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private static boolean isNewerVersion(String vNew, String vCurrent) {
        if (vNew == null || vNew.isBlank()) return false;
        if (vCurrent == null || vCurrent.isBlank()) return true;
        if (vNew.trim().equalsIgnoreCase(vCurrent.trim())) return false;

        String cleanNew = vNew.trim().replaceAll("^v", "");
        String cleanCurr = vCurrent.trim().replaceAll("^v", "");

        String[] partsNew = cleanNew.split("[-.]");
        String[] partsCurr = cleanCurr.split("[-.]");

        int maxLen = Math.max(partsNew.length, partsCurr.length);
        for (int i = 0; i < maxLen; i++) {
            String pNew = i < partsNew.length ? partsNew[i] : "0";
            String pCurr = i < partsCurr.length ? partsCurr[i] : "0";

            boolean isNumNew = pNew.matches("\\d+");
            boolean isNumCurr = pCurr.matches("\\d+");

            if (isNumNew && isNumCurr) {
                long numNew = Long.parseLong(pNew);
                long numCurr = Long.parseLong(pCurr);
                if (numNew > numCurr) return true;
                if (numNew < numCurr) return false;
            } else {
                if (pNew.equalsIgnoreCase("SNAPSHOT") && !pCurr.equalsIgnoreCase("SNAPSHOT")) {
                    return false;
                }
                if (!pNew.equalsIgnoreCase("SNAPSHOT") && pCurr.equalsIgnoreCase("SNAPSHOT")) {
                    return true;
                }
                int comp = pNew.compareToIgnoreCase(pCurr);
                if (comp > 0) return true;
                if (comp < 0) return false;
            }
        }
        return false;
    }
}
