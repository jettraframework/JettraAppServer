package io.jettra.server.cli;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FluxCLI {

    public static void main(String[] args) {
        if (args.length == 0) {
            showHelp();
            return;
        }

        List<String> argList = new ArrayList<>();
        for (String arg : args) {
            argList.add(arg);
        }

        for (String arg : argList) {
            if ("-help".equalsIgnoreCase(arg) || "--help".equalsIgnoreCase(arg) || "help".equalsIgnoreCase(arg) || "-h".equalsIgnoreCase(arg)) {
                showHelp();
                return;
            }
        }

        String command = argList.get(0);
        String sourceRecord = null;
        String sourcePackageRecord = null;
        boolean isModel = false;
        boolean isProperties = false;
        boolean isRest = false;
        boolean isServices = false;
        boolean isConverter = false;
        boolean isPage = false;
        boolean isPageCrud = false;
        boolean isTestRest = false;
        boolean isTestService = false;
        boolean isTestPage = false;
        boolean isRepository = false;
        boolean isController = false;

        for (int i = 0; i < argList.size(); i++) {
            String arg = argList.get(i);
            String nextArg = (i + 1 < argList.size()) ? argList.get(i + 1) : null;

            if ("-source-record".equalsIgnoreCase(arg) || "-from-record".equalsIgnoreCase(arg) || "source-record".equalsIgnoreCase(arg) || "from-record".equalsIgnoreCase(arg)) {
                if (nextArg != null) sourceRecord = nextArg;
            } else if ("-source-package-record".equalsIgnoreCase(arg) || "-from-package-record".equalsIgnoreCase(arg) || "source-package-record".equalsIgnoreCase(arg) || "from-package-record".equalsIgnoreCase(arg)) {
                if (nextArg != null) sourcePackageRecord = nextArg;
            } else if ("-model".equalsIgnoreCase(arg) || "model".equalsIgnoreCase(arg)) {
                isModel = true;
            } else if ("-properties".equalsIgnoreCase(arg) || "properties".equalsIgnoreCase(arg)) {
                isProperties = true;
            } else if ("-rest".equalsIgnoreCase(arg) || "rest".equalsIgnoreCase(arg)) {
                isRest = true;
            } else if ("-services".equalsIgnoreCase(arg) || "services".equalsIgnoreCase(arg)) {
                isServices = true;
            } else if ("-converter".equalsIgnoreCase(arg) || "converter".equalsIgnoreCase(arg)) {
                isConverter = true;
            } else if ("-page".equalsIgnoreCase(arg) || "page".equalsIgnoreCase(arg)) {
                isPage = true;
            } else if ("-page-crud".equalsIgnoreCase(arg) || "page-crud".equalsIgnoreCase(arg)) {
                isPageCrud = true;
            } else if ("-test-rest".equalsIgnoreCase(arg) || "test-rest".equalsIgnoreCase(arg)) {
                isTestRest = true;
            } else if ("-test-service".equalsIgnoreCase(arg) || "test-service".equalsIgnoreCase(arg)) {
                isTestService = true;
            } else if ("-repository".equalsIgnoreCase(arg) || "repository".equalsIgnoreCase(arg)) {
                isRepository = true;
            } else if ("-controller".equalsIgnoreCase(arg) || "controller".equalsIgnoreCase(arg)) {
                isController = true;
            } else if ("-test-page".equalsIgnoreCase(arg) || "test-page".equalsIgnoreCase(arg)) {
                isTestPage = true;
            }
        }

        List<String> sourceRecords = new ArrayList<>();
        if (sourceRecord != null) {
            sourceRecords.add(sourceRecord);
        }
        if (sourcePackageRecord != null) {
            String relativePath = "src/main/java/" + sourcePackageRecord.replace(".", "/");
            Path packagePath = Paths.get(relativePath);
            if (!Files.exists(packagePath) || !Files.isDirectory(packagePath)) {
                System.out.println("Error: Source package directory not found at " + relativePath);
            } else {
                try (java.util.stream.Stream<Path> stream = Files.list(packagePath)) {
                    List<Path> javaFiles = stream.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".java"))
                            .sorted()
                            .collect(java.util.stream.Collectors.toList());
                    java.util.regex.Pattern recordPattern = java.util.regex.Pattern.compile("\\brecord\\s+\\w+");
                    for (Path file : javaFiles) {
                        String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                        if (recordPattern.matcher(content).find()) {
                            String fileName = file.getFileName().toString();
                            String className = fileName.substring(0, fileName.length() - 5);
                            sourceRecords.add(sourcePackageRecord + "." + className);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error reading package directory: " + e.getMessage());
                }
            }
        }

        switch (command.toLowerCase()) {
            case "-create-code":
            case "create-code":
                if (!sourceRecords.isEmpty()) {
                    for (String rec : sourceRecords) {
                        List<String[]> parsedFields = parseRecordFields(rec);
                        if (parsedFields != null) {
                            if (isModel) {
                                generateViewModel(rec, parsedFields, isProperties);
                            }
                            if (isConverter) {
                                generateConverter(rec, parsedFields);
                            }
                            if (isRest) {
                                generateRestClient(rec, parsedFields);
                            }
                            if (isServices) {
                                generateService(rec, parsedFields);
                            }
                            if (isPage) {
                                generatePage(rec, parsedFields, false);
                            }
                            if (isPageCrud) {
                                generatePage(rec, parsedFields, true);
                            }
                            if (isTestRest) {
                                generateTestRest(rec, parsedFields);
                            }
                            if (isTestService) {
                                generateTestService(rec, parsedFields);
                            }
                            if (isTestPage) {
                                generateTestPage(rec, parsedFields);
                            }
                            if (isRepository) {
                                generateRepository(rec, parsedFields);
                            }
                            if (isController) {
                                generateController(rec, parsedFields);
                            }
                        }
                    }
                } else {
                    System.out.println("Missing arguments. Usage:");
                    System.out.println("  ./mvn-flux -create-code -source-record <Paquete.Record> -model [-properties] [-converter] [-rest] [-services] [-page] [-page-crud] [-test-rest] [-test-service] [-test-page] [-repository] [-controller]");
                    System.out.println("  ./mvn-flux -create-code -source-package-record <Paquete> -model [-properties] [-converter] [-rest] [-services] [-page] [-page-crud] [-test-rest] [-test-service] [-test-page] [-repository] [-controller]");
                }
                break;
            case "-initialize-front-end":
            case "--initialize-front-end":
            case "initialize-front-end":
                initializeFrontEnd();
                break;
            case "-generate-theme-project":
                if (argList.size() > 1) {
                    String projectName = argList.get(1);
                    String pathDir = null;
                    String urlSource = null;
                    for (int j = 2; j < argList.size(); j++) {
                        if ("-path".equalsIgnoreCase(argList.get(j)) && j + 1 < argList.size()) {
                            pathDir = argList.get(j + 1);
                        } else if ("-url-source".equalsIgnoreCase(argList.get(j)) && j + 1 < argList.size()) {
                            urlSource = argList.get(j + 1);
                        }
                    }
                    if (pathDir == null) {
                        pathDir = System.getProperty("user.dir");
                    }
                    generateThemeProject(projectName, pathDir, urlSource);
                } else {
                    System.out.println("Error: Missing project name for -generate-theme-project");
                }
                break;
            default:
                System.out.println("Unknown command: " + command);
                System.out.println();
                showHelp();
        }
    }

    private static void showHelp() {
        System.out.println("====================================================================================================");
        System.out.println("                            JettraFlux CLI - Menú de Ayuda");
        System.out.println("====================================================================================================");
        System.out.println("Uso: ./mvn-flux <comando> [parámetros/opciones]\n");
        System.out.println("COMANDOS DISPONIBLES:");
        System.out.println("  -create-code           Genera código fuente automáticamente a partir de entidades (records).");
        System.out.println("  -initialize-front-end  Inicializa la estructura frontend completa del proyecto pom.xml, jettra-config.properties, App.java y paquetes.");
        System.out.println("  -generate-theme-project <nombre> [-path <ruta>] [-url-source <url>] Genera un proyecto Maven independiente para un plugin de tema.");
        System.out.println("  -help                  Muestra este menú de ayuda explicativo en la consola.\n");
        System.out.println("PARÁMETROS Y OPCIONES PARA -create-code:");
        System.out.println("  -source-record <FQN>            Especifica la ruta completa (Fully Qualified Name) de un record.");
        System.out.println("                                  (Ejemplo: com.example.entity.Person)");
        System.out.println("                                  Alias: -from-record, source-record, from-record\n");
        System.out.println("  -source-package-record <Pkg>    Especifica el paquete para procesar masivamente todos sus records.");
        System.out.println("                                  (Ejemplo: com.example.entity)");
        System.out.println("                                  Alias: -from-package-record, source-package-record, from-package-record\n");
        System.out.println("  -model                          [Requerido] Genera la clase ViewModel (<Nombre>Model.java).");
        System.out.println("  -properties                     Escanea y actualiza los archivos messages*.properties con las etiquetas.");
        System.out.println("  -converter                      Genera la clase conversora (<Nombre>ModelConverter.java).");
        System.out.println("  -rest                           Genera la interfaz cliente REST (<Nombre>RestClient.java).");
        System.out.println("  -services                       Genera la clase de servicio (<Nombre>Service.java).");
        System.out.println("  -page                           Genera la página básica (<Nombre>Page.java).");
        System.out.println("  -page-crud                      Genera la página CRUD completa (<Nombre>CrudPage.java).");
        System.out.println("  -test-rest                      Genera las pruebas para los clientes REST.");
        System.out.println("  -test-service                   Genera las pruebas para los servicios.");
        System.out.println("  -test-page                      Genera las pruebas para las páginas.");
        System.out.println("  -repository                     Genera el repositorio (Interface e Impl) para un record.");
        System.out.println("  -controller                     Genera el controlador REST para un record.\n");
        System.out.println("EJEMPLOS DE USO:");
        System.out.println("  1. Por un Record individual:");
        System.out.println("     ./mvn-flux -create-code -source-record com.example.entity.Person -model -properties -converter -rest -services\n");
        System.out.println("  2. Por un paquete completo de Records:");
        System.out.println("     ./mvn-flux -create-code -source-package-record com.example.entity -model -properties -converter -rest -services\n");
        System.out.println("  3. Generar un plugin de tema:");
        System.out.println("     ./mvn-flux -generate-theme-project SkyRed -url-source https://primeui.store/templates/angular/freya");
        System.out.println("====================================================================================================");
    }

    private static List<String> extractRecordImports(String sourceRecord, List<String[]> parsedFields) {
        List<String> imports = new ArrayList<>();
        int lastDot = sourceRecord.lastIndexOf('.');
        String originalPackage = (lastDot != -1) ? sourceRecord.substring(0, lastDot) : "";

        try {
            String relativePath = "src/main/java/" + sourceRecord.replace(".", "/") + ".java";
            Path recordPath = Paths.get(relativePath);
            if (Files.exists(recordPath)) {
                String content = new String(Files.readAllBytes(recordPath), StandardCharsets.UTF_8);
                java.util.regex.Matcher importMatcher = java.util.regex.Pattern.compile("import\\s+([^;]+);").matcher(content);
                while (importMatcher.find()) {
                    String imp = importMatcher.group(1).trim();
                    if (!imports.contains(imp)) {
                        imports.add(imp);
                    }
                }
            }
        } catch (Exception ignored) {}

        if (parsedFields != null) {
            for (String[] field : parsedFields) {
                String type = field[0];
                String coreType = type;
                if (type.startsWith("List<") || type.startsWith("Set<") || type.startsWith("Collection<")) {
                    coreType = type.substring(type.indexOf('<') + 1, type.indexOf('>')).trim();
                }
                if (coreType.contains("<")) {
                    coreType = coreType.substring(0, coreType.indexOf('<')).trim();
                }

                addStandardImport(imports, coreType, originalPackage);
            }
        }

        return imports;
    }

    private static String generatePersonPageTestClass(String pkg) {
        return "package " + pkg + ".page;\n\n" +
               "import " + pkg + ".model.PersonModel;\n" +
               "import io.jettra.flux.binding.FluxBinder;\n" +
               "import io.jettra.rules.core.RuleResult;\n" +
               "import io.jettra.test.annotation.JettraTest;\n" +
               "import io.jettra.test.core.JettraAssert;\n" +
               "import io.jettra.test.wui.WuiTestRunner;\n\n" +
               "import java.util.HashMap;\n" +
               "import java.util.List;\n" +
               "import java.util.Map;\n\n" +
               "public class PersonPageTest {\n\n" +
               "    @JettraTest\n" +
               "    public void testPersonModelJavaMethodValidation() {\n" +
               "        PersonModel model = new PersonModel();\n" +
               "        Map<String, String> params = new HashMap<>();\n" +
               "        params.put(\"name\", \"Juan Perez\");\n" +
               "        params.put(\"email\", \"juan@example.com\");\n" +
               "        params.put(\"age\", \"30\");\n\n" +
               "        List<RuleResult> results = new FluxBinder(model)\n" +
               "                .bind(params)\n" +
               "                .compute()\n" +
               "                .validate();\n\n" +
               "        JettraAssert.assertTrue(results.stream().allMatch(RuleResult::isValid), \"Method-level validation for PersonModel should pass\");\n\n" +
               "        // Test invalid email\n" +
               "        PersonModel invalidModel = new PersonModel();\n" +
               "        Map<String, String> invalidParams = new HashMap<>();\n" +
               "        invalidParams.put(\"name\", \"Juan Perez\");\n" +
               "        invalidParams.put(\"email\", \"invalid-email-address\");\n" +
               "        invalidParams.put(\"age\", \"30\");\n\n" +
               "        List<RuleResult> invalidResults = new FluxBinder(invalidModel)\n" +
               "                .bind(invalidParams)\n" +
               "                .compute()\n" +
               "                .validate();\n\n" +
               "        JettraAssert.assertTrue(invalidResults.stream().anyMatch(r -> !r.isValid()), \"Method-level validation should fail for invalid email\");\n" +
               "    }\n\n" +
               "    @JettraTest\n" +
               "    public void testPersonModelWebScriptGeneration() {\n" +
               "        String script = FluxBinder.generateWebRulesScript(PersonModel.class, \"personForm\");\n" +
               "        JettraAssert.assertTrue(script != null && script.contains(\"function validateModelRules\"), \"Web-level JS validation script should be generated for PersonModel\");\n" +
               "        JettraAssert.assertTrue(script.contains(\"correo electrónico válido\") || script.contains(\"valid email\"), \"Web-level validation for Email should be included in JS script\");\n" +
               "    }\n\n" +
               "    @JettraTest\n" +
               "    public void testPersonPageSimulation() {\n" +
               "        WuiTestRunner wuiRunner = new WuiTestRunner();\n" +
               "        wuiRunner.validateInterface(\"PersonPage\");\n" +
               "        wuiRunner.registerFormData(\"personForm\", \"{\\\"name\\\":\\\"Juan\\\", \\\"email\\\":\\\"juan@example.com\\\", \\\"age\\\":\\\"25\\\"}\");\n" +
               "        JettraAssert.assertTrue(true, \"Simulación de PersonPage ejecutada exitosamente\");\n" +
               "    }\n" +
               "}\n";
    }

    private static boolean isBasicType(String type) {
        if (type == null) return false;
        if (type.startsWith("List<") || type.startsWith("Set<") || type.startsWith("Collection<") || type.startsWith("Map<")) {
            return false;
        }
        return type.equals("String") || type.equals("Character") || type.equals("char")
            || type.equals("Integer") || type.equals("int")
            || type.equals("Long") || type.equals("long")
            || type.equals("Double") || type.equals("double")
            || type.equals("Float") || type.equals("float")
            || type.equals("Short") || type.equals("short")
            || type.equals("Byte") || type.equals("byte")
            || type.equals("Boolean") || type.equals("boolean")
            || type.equals("BigDecimal") || type.equals("BigInteger")
            || type.equals("UUID")
            || type.equals("LocalDate") || type.equals("LocalDateTime") || type.equals("LocalTime")
            || type.equals("ZonedDateTime") || type.equals("OffsetDateTime") || type.equals("Instant")
            || type.equals("Date");
    }

    private static void addStandardImport(List<String> imports, String type, String originalPackage) {
        String imp = null;
        switch (type) {
            case "LocalDate":
                imp = "java.time.LocalDate";
                break;
            case "LocalDateTime":
                imp = "java.time.LocalDateTime";
                break;
            case "LocalTime":
                imp = "java.time.LocalTime";
                break;
            case "ZonedDateTime":
                imp = "java.time.ZonedDateTime";
                break;
            case "OffsetDateTime":
                imp = "java.time.OffsetDateTime";
                break;
            case "Instant":
                imp = "java.time.Instant";
                break;
            case "Date":
                imp = "java.util.Date";
                break;
            case "UUID":
                imp = "java.util.UUID";
                break;
            case "BigDecimal":
                imp = "java.math.BigDecimal";
                break;
            case "BigInteger":
                imp = "java.math.BigInteger";
                break;
            case "Set":
                imp = "java.util.Set";
                break;
            case "List":
                imp = "java.util.List";
                break;
            case "Map":
                imp = "java.util.Map";
                break;
            default:
                if (!isBasicType(type) && !originalPackage.isEmpty()) {
                    boolean exists = false;
                    for (String existing : imports) {
                        if (existing.endsWith("." + type)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        imp = originalPackage + "." + type;
                    }
                }
                break;
        }

        if (imp != null && !imports.contains(imp)) {
            imports.add(imp);
        }
    }

    private static List<String[]> parseRecordFields(String sourceRecord) {
        try {
            String relativePath = "src/main/java/" + sourceRecord.replace(".", "/") + ".java";
            Path recordPath = Paths.get(relativePath);
            if (!Files.exists(recordPath)) {
                System.out.println("Error: Source record file not found at " + relativePath);
                return null;
            }
            String content = new String(Files.readAllBytes(recordPath), StandardCharsets.UTF_8);
            int recordIdx = content.indexOf("record ");
            if (recordIdx == -1) return null;
            int nameStart = recordIdx + "record ".length();
            int parenStart = content.indexOf("(", nameStart);
            if (parenStart == -1) return null;
            int parenEnd = -1;
            int openCount = 0;
            for (int i = parenStart; i < content.length(); i++) {
                if (content.charAt(i) == '(') openCount++;
                else if (content.charAt(i) == ')') {
                    openCount--;
                    if (openCount == 0) { parenEnd = i; break; }
                }
            }
            if (parenEnd == -1) return null;
            String fieldsContent = content.substring(parenStart + 1, parenEnd).trim();
            List<String[]> parsedFields = new ArrayList<>();
            if (!fieldsContent.isEmpty()) {
                List<String> rawFieldsList = new ArrayList<>();
                StringBuilder currentField = new StringBuilder();
                int openParen = 0;
                for (int i = 0; i < fieldsContent.length(); i++) {
                    char c = fieldsContent.charAt(i);
                    if (c == '(') openParen++;
                    else if (c == ')') openParen--;
                    if (c == ',' && openParen == 0) {
                        rawFieldsList.add(currentField.toString());
                        currentField.setLength(0);
                    } else { currentField.append(c); }
                }
                if (currentField.length() > 0) rawFieldsList.add(currentField.toString());
                for (String rawField : rawFieldsList) {
                    rawField = rawField.trim().replaceAll("@\\w+(?:\\([^)]*\\))?\\s+", "");
                    String[] parts = rawField.split("\\s+");
                    if (parts.length >= 2) {
                        String type = parts[parts.length - 2];
                        String name = parts[parts.length - 1];
                        parsedFields.add(new String[]{type, name});
                    }
                }
            }
            return parsedFields;
        } catch (Exception e) { return null; }
    }

    private static void generateViewModel(String sourceRecord, List<String[]> parsedFields, boolean generateProperties) {
        try {
            String relativePath = "src/main/java/" + sourceRecord.replace(".", "/") + ".java";
            Path recordPath = Paths.get(relativePath);
            if (!Files.exists(recordPath)) return;
            String content = new String(Files.readAllBytes(recordPath), StandardCharsets.UTF_8);
            String originalPackage = "";
            java.util.regex.Matcher pkgMatcher = java.util.regex.Pattern.compile("package\\s+([^;]+);").matcher(content);
            if (pkgMatcher.find()) originalPackage = pkgMatcher.group(1).trim();
            int recordIdx = content.indexOf("record ");
            int nameStart = recordIdx + "record ".length();
            int parenStart = content.indexOf("(", nameStart);
            String recordName = content.substring(nameStart, parenStart).trim();
            if (recordName.contains("<")) recordName = recordName.substring(0, recordName.indexOf("<")).trim();
            String modelPackage = originalPackage.replace(".entity", ".model");
            String modelClassName = recordName + "Model";
            StringBuilder sb = new StringBuilder();
            sb.append("package ").append(modelPackage).append(";\n\n");
            
            // Generate imports
            sb.append("import ").append(sourceRecord).append(";\n");
            sb.append("import io.jettra.flux.annotations.JettraViewModel;\n");
            sb.append("import io.jettra.flux.annotations.PropertiesInRecord;\n");
            sb.append("import io.jettra.flux.annotations.PropertiesLabel;\n");
            sb.append("import io.jettra.flux.annotations.ViewSelectOne;\n");
            sb.append("import io.jettra.flux.annotations.ViewSelectMany;\n");
            sb.append("import io.jettra.flux.annotations.TableColumnField;\n");
            sb.append("import io.jettra.rules.validations.NotNull;\n");
            
            List<String> existingImports = new ArrayList<>();
            java.util.regex.Matcher importMatcher = java.util.regex.Pattern.compile("import\\s+([^;]+);").matcher(content);
            while (importMatcher.find()) {
                String imp = importMatcher.group(1).trim();
                existingImports.add(imp);
                sb.append("import ").append(imp).append(";\n");
            }

            List<String> recordImports = extractRecordImports(sourceRecord, parsedFields);
            for (String imp : recordImports) {
                if (!imp.equals(sourceRecord) && !imp.startsWith("io.jettra.flux.") && !imp.startsWith("io.jettra.rules.")) {
                    boolean exists = false;
                    for (String existing : existingImports) {
                        if (existing.equals(imp)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        sb.append("import ").append(imp).append(";\n");
                        existingImports.add(imp);
                    }
                }
            }
            
            // Identify referenced entities and add imports for them
            for (String[] field : parsedFields) {
                String type = field[0];
                String entityType = type;
                if (type.startsWith("List<") || type.startsWith("Set<") || type.startsWith("Collection<")) {
                    entityType = type.substring(type.indexOf('<') + 1, type.indexOf('>'));
                }
                
                if (!isBasicType(entityType)) {
                    boolean alreadyImported = false;
                    for (String imp : existingImports) {
                        if (imp.endsWith("." + entityType)) {
                            alreadyImported = true;
                            break;
                        }
                    }
                    if (!alreadyImported) {
                        sb.append("import ").append(originalPackage).append(".").append(entityType).append(";\n");
                        existingImports.add(originalPackage + "." + entityType);
                    }
                }
            }
            sb.append("\n");

            sb.append("@JettraViewModel\n");
            sb.append("public class ").append(modelClassName).append(" {\n\n");

            for (String[] field : parsedFields) {
                String type = field[0];
                String name = field[1];
                
                sb.append("    @PropertiesInRecord\n");
                sb.append("    @PropertiesLabel(value = \"").append(recordName.toLowerCase()).append(".").append(name).append("\", label = \"").append(capitalize(name)).append("\")\n");
                
                if (isBasicType(type)) {
                    if (type.equals("String")) {
                        sb.append("    @NotNull\n");
                    }
                } else if (type.startsWith("List<") || type.startsWith("Set<") || type.startsWith("Collection<")) {
                    String genericType = type.substring(type.indexOf('<') + 1, type.indexOf('>'));
                    String servicePkg = originalPackage.replace(".entity", ".services");
                    String serviceFqn = servicePkg + "." + genericType + "Service";
                    sb.append("    @ViewSelectMany(label = \"name\", source = \"").append(serviceFqn).append("\", method = \"findAll\")\n");
                    sb.append("    @TableColumnField(field = \"name\")\n");
                } else {
                    String servicePkg = originalPackage.replace(".entity", ".services");
                    String serviceFqn = servicePkg + "." + type + "Service";
                    sb.append("    @ViewSelectOne(label = \"name\", source = \"").append(serviceFqn).append("\", method = \"findAll\")\n");
                }
                
                sb.append("    private ").append(type).append(" ").append(name).append(";\n\n");
            }

            sb.append("    public ").append(modelClassName).append("() {}\n\n");

            for (String[] field : parsedFields) {
                String type = field[0];
                String name = field[1];
                String capName = capitalize(name);

                sb.append("    public ").append(type).append(" get").append(capName).append("() {\n");
                sb.append("        return ").append(name).append(";\n");
                sb.append("    }\n\n");

                sb.append("    public void set").append(capName).append("(").append(type).append(" ").append(name).append(") {\n");
                sb.append("        this.").append(name).append(" = ").append(name).append(";\n");
                sb.append("    }\n\n");
            }
            
            sb.append("}\n");

            String outputRelativePath = "src/main/java/" + modelPackage.replace(".", "/") + "/" + modelClassName + ".java";
            Path outputPath = Paths.get(outputRelativePath);
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, sb.toString().getBytes(StandardCharsets.UTF_8));
            
            System.out.println("Generated ViewModel: " + outputRelativePath);

            if (generateProperties) {
                updatePropertiesFiles(recordName, parsedFields);
            }
            
        } catch (Exception e) {
            System.err.println("Failed to generate ViewModel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void updatePropertiesFiles(String recordName, List<String[]> parsedFields) {
        try {
            Path resourcesDir = Paths.get("src/main/resources");
            if (!Files.exists(resourcesDir)) {
                System.out.println("Directory src/main/resources does not exist. Skipping properties generation.");
                return;
            }

            Files.list(resourcesDir)
                 .filter(p -> p.getFileName().toString().startsWith("messages") && p.getFileName().toString().endsWith(".properties"))
                 .forEach(p -> appendProperties(p, recordName, parsedFields));

        } catch (Exception e) {
            System.err.println("Failed to update properties files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void appendProperties(Path propFile, String recordName, List<String[]> parsedFields) {
        try {
            List<String> lines = Files.readAllLines(propFile, StandardCharsets.UTF_8);
            boolean modified = false;
            String prefix = recordName.toLowerCase() + ".";

            for (String[] field : parsedFields) {
                String name = field[1];
                String propKey = prefix + name;
                String propValue = capitalize(name);

                boolean exists = false;
                for (String line : lines) {
                    if (line.trim().startsWith(propKey + "=") || line.trim().startsWith(propKey + " =")) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    lines.add(propKey + " = " + propValue);
                    modified = true;
                }
            }

            if (modified) {
                Files.write(propFile, lines, StandardCharsets.UTF_8);
                System.out.println("Updated properties file: " + propFile.toString());
            }

        } catch (Exception e) {
            System.err.println("Failed to write to properties file " + propFile.toString() + ": " + e.getMessage());
        }
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private static void generateRestClient(String sourceRecord, List<String[]> parsedFields) {
        try {
            int lastDot = sourceRecord.lastIndexOf('.');
            String originalPackage = sourceRecord.substring(0, lastDot);
            String recordName = sourceRecord.substring(lastDot + 1);

            String restClientPackage = originalPackage.replace(".entity", ".restclient");
            String clientClassName = recordName + "RestClient";

            StringBuilder sb = new StringBuilder();
            sb.append("package ").append(restClientPackage).append(";\n\n");
            
            sb.append("import ").append(sourceRecord).append(";\n");
            sb.append("import io.jettra.rest.annotations.DELETE;\n");
            sb.append("import io.jettra.rest.annotations.GET;\n");
            sb.append("import io.jettra.rest.annotations.POST;\n");
            sb.append("import io.jettra.rest.annotations.PUT;\n");
            sb.append("import io.jettra.rest.annotations.Path;\n");
            sb.append("import io.jettra.rest.annotations.PathParam;\n");
            sb.append("import io.jettra.rest.client.RestClient;\n");
            sb.append("import java.util.List;\n");

            List<String> recordImports = extractRecordImports(sourceRecord, parsedFields);
            for (String imp : recordImports) {
                if (!imp.startsWith("io.jettra.rest.") && !imp.equals("java.util.List") && !imp.equals(sourceRecord)) {
                    sb.append("import ").append(imp).append(";\n");
                }
            }
            sb.append("\n");

            String baseUriModule = "api";
            String[] pkgParts = originalPackage.split("\\.");
            if (pkgParts.length > 2) {
                baseUriModule = pkgParts[pkgParts.length - 2];
            }
            String pluralEndpoint = "/api/" + baseUriModule + "/" + recordName.toLowerCase() + "s";

            sb.append("@RestClient(baseUri = \"").append(pluralEndpoint).append("\")\n");
            sb.append("public interface ").append(clientClassName).append(" {\n\n");

            sb.append("    @GET\n");
            sb.append("    List<").append(recordName).append("> findAll();\n\n");

            sb.append("    @POST\n");
            sb.append("    void save(").append(recordName).append(" ").append(recordName.toLowerCase()).append(");\n\n");

            sb.append("    @PUT\n");
            sb.append("    void update(").append(recordName).append(" ").append(recordName.toLowerCase()).append(");\n\n");

            sb.append("    @DELETE\n");
            sb.append("    @Path(\"/{id}\")\n");
            sb.append("    void delete(@PathParam(\"id\") String id);\n");
            
            for (String[] field : parsedFields) {
                String type = field[0];
                String name = field[1];
                sb.append("\n    @GET\n");
                sb.append("    @Path(\"/").append(name.toLowerCase()).append("/{").append(name).append("}\")\n");
                sb.append("    List<").append(recordName).append("> findBy").append(capitalize(name)).append("(@PathParam(\"").append(name).append("\") ").append(type).append(" ").append(name).append(");\n");
            }
            
            sb.append("}\n");

            String outputRelativePath = "src/main/java/" + restClientPackage.replace(".", "/") + "/" + clientClassName + ".java";
            Path outputPath = Paths.get(outputRelativePath);
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, sb.toString().getBytes(StandardCharsets.UTF_8));
            
            System.out.println("Generated RestClient: " + outputRelativePath);

        } catch (Exception e) {
            System.err.println("Failed to generate RestClient: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void generateService(String sourceRecord, List<String[]> parsedFields) {
        try {
            int lastDot = sourceRecord.lastIndexOf('.');
            String originalPackage = sourceRecord.substring(0, lastDot);
            String recordName = sourceRecord.substring(lastDot + 1);

            String servicePackage = originalPackage.replace(".entity", ".services");
            String serviceClassName = recordName + "Service";
            
            String restClientPackage = originalPackage.replace(".entity", ".restclient");
            String clientClassName = recordName + "RestClient";

            StringBuilder sb = new StringBuilder();
            sb.append("package ").append(servicePackage).append(";\n\n");
            
            sb.append("import ").append(sourceRecord).append(";\n");
            sb.append("import ").append(restClientPackage).append(".").append(clientClassName).append(";\n");
            sb.append("import io.jettra.core.inject.annotation.Inject;\n");
            sb.append("import java.util.List;\n");

            List<String> recordImports = extractRecordImports(sourceRecord, parsedFields);
            for (String imp : recordImports) {
                if (!imp.equals(sourceRecord) && !imp.equals("java.util.List") && !imp.startsWith("io.jettra.core.")) {
                    sb.append("import ").append(imp).append(";\n");
                }
            }
            sb.append("\n");

            sb.append("public class ").append(serviceClassName).append(" {\n\n");

            sb.append("    @Inject\n");
            sb.append("    private ").append(clientClassName).append(" client;\n\n");

            sb.append("    public List<").append(recordName).append("> findAll() {\n");
            sb.append("        List<").append(recordName).append("> records = client.findAll();\n");
            sb.append("        if (records == null) return List.of();\n");
            sb.append("        return records;\n");
            sb.append("    }\n\n");

            sb.append("    public void save(").append(recordName).append(" record) {\n");
            sb.append("        client.save(record);\n");
            sb.append("    }\n\n");

            sb.append("    public void delete(String id) {\n");
            sb.append("        client.delete(id);\n");
            sb.append("    }\n");

            if (parsedFields != null) {
                for (String[] field : parsedFields) {
                    String type = field[0];
                    String name = field[1];
                    sb.append("\n    public List<").append(recordName).append("> findBy").append(capitalize(name)).append("(").append(type).append(" ").append(name).append(") {\n");
                    sb.append("        List<").append(recordName).append("> records = client.findBy").append(capitalize(name)).append("(").append(name).append(");\n");
                    sb.append("        if (records == null) return List.of();\n");
                    sb.append("        return records;\n");
                    sb.append("    }\n");
                }
            }

            sb.append("}\n");

            String outputRelativePath = "src/main/java/" + servicePackage.replace(".", "/") + "/" + serviceClassName + ".java";
            Path outputPath = Paths.get(outputRelativePath);
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, sb.toString().getBytes(StandardCharsets.UTF_8));
            
            System.out.println("Generated Service: " + outputRelativePath);

        } catch (Exception e) {
            System.err.println("Failed to generate Service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void generateConverter(String sourceRecord, List<String[]> parsedFields) {
        try {
            int lastDot = sourceRecord.lastIndexOf('.');
            String originalPackage = sourceRecord.substring(0, lastDot);
            String recordName = sourceRecord.substring(lastDot + 1);

            String converterPackage = originalPackage.replace(".entity", ".converter");
            String converterClassName = recordName + "ModelConverter";
            String modelPackage = originalPackage.replace(".entity", ".model");
            String modelClassName = recordName + "Model";

            StringBuilder sb = new StringBuilder();
            sb.append("package ").append(converterPackage).append(";\n\n");
            
            sb.append("import ").append(sourceRecord).append(";\n");
            sb.append("import ").append(modelPackage).append(".").append(modelClassName).append(";\n");
            sb.append("import io.jettra.scoped.ApplicationScoped;\n");

            List<String> recordImports = extractRecordImports(sourceRecord, parsedFields);
            for (String imp : recordImports) {
                if (!imp.equals(sourceRecord) && !imp.startsWith("io.jettra.scoped.")) {
                    sb.append("import ").append(imp).append(";\n");
                }
            }
            sb.append("\n");

            sb.append("@ApplicationScoped\n");
            sb.append("public class ").append(converterClassName).append(" {\n");
            
            sb.append("  public ").append(modelClassName).append(" toModel(").append(recordName).append(" record) {\n");
            sb.append("    if (record == null) {\n      return null;\n    }\n");
            sb.append("    ").append(modelClassName).append(" model = new ").append(modelClassName).append("();\n");
            for (String[] field : parsedFields) {
                String name = field[1];
                sb.append("    model.set").append(capitalize(name)).append("(record.").append(name).append("());\n");
            }
            sb.append("    return model;\n");
            sb.append("  }\n\n");
            
            sb.append("  public ").append(recordName).append(" toRecord(").append(modelClassName).append(" model) {\n");
            sb.append("    if (model == null) {\n      return null;\n    }\n");
            sb.append("    return new ").append(recordName).append("(\n");
            for (int i = 0; i < parsedFields.size(); i++) {
                String name = parsedFields.get(i)[1];
                sb.append("          model.get").append(capitalize(name)).append("()");
                if (i < parsedFields.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("        );\n");
            sb.append("  }\n");
            sb.append("}\n");

            String outputRelativePath = "src/main/java/" + converterPackage.replace(".", "/") + "/" + converterClassName + ".java";
            Path outputPath = Paths.get(outputRelativePath);
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, sb.toString().getBytes(StandardCharsets.UTF_8));
            
            System.out.println("Generated Converter: " + outputRelativePath);

        } catch (Exception e) {
            System.err.println("Failed to generate Converter: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private static void generatePage(String sourceRecord, List<String[]> parsedFields, boolean isCrud) {
        try {
            int lastDot = sourceRecord.lastIndexOf('.');
            String originalPackage = sourceRecord.substring(0, lastDot);
            String recordName = sourceRecord.substring(lastDot + 1);

            String pagePackage = originalPackage.replace(".entity", ".pages");
            String pageClassName = recordName + (isCrud ? "CrudPage" : "Page");

            StringBuilder sb = new StringBuilder();
            sb.append("package ").append(pagePackage).append(";\n\n");
            
            sb.append("import ").append(sourceRecord).append(";\n");
            sb.append("import io.jettra.flux.widgets.*;\n");
            sb.append("import io.jettra.core.server.Page;\n");
            sb.append("import io.jettra.flux.core.Widget;\n");
            sb.append("import com.sun.net.httpserver.HttpExchange;\n");
            sb.append("import java.util.Map;\n\n");

            sb.append("@Page(path = \"/").append(recordName.toLowerCase()).append(isCrud ? "-crud" : "").append("\")\n");
            sb.append("public class ").append(pageClassName).append(" {\n\n");
            
            sb.append("    protected Widget buildCenter(HttpExchange exchange, Map<String, String> params, String currentTheme) {\n");
            
            if (isCrud) {
                sb.append("        return Column.of(Header.of(2, \"Gestión de ").append(recordName).append("\"),\n");
                sb.append("                Paragraph.of(\"CRUD autogenerado para ").append(recordName).append("\")\n");
                sb.append("        );\n");
            } else {
                sb.append("        return Column.of(Header.of(2, \"Página ").append(recordName).append("\"),\n");
                sb.append("                Paragraph.of(\"Página básica autogenerada para ").append(recordName).append("\")\n");
                sb.append("        );\n");
            }
            
            sb.append("    }\n");
            sb.append("}\n");

            String outputRelativePath = "src/main/java/" + pagePackage.replace(".", "/") + "/" + pageClassName + ".java";
            Path outputPath = Paths.get(outputRelativePath);
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, sb.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("Generated Page: " + outputRelativePath);
        } catch (Exception e) {
            System.err.println("Failed to generate Page: " + e.getMessage());
        }
    }

    private static void generateTestRest(String sourceRecord, List<String[]> parsedFields) {
        try {
            int lastDot = sourceRecord.lastIndexOf('.');
            String originalPackage = sourceRecord.substring(0, lastDot);
            String recordName = sourceRecord.substring(lastDot + 1);

            String testPackage = originalPackage.replace(".entity", ".restclient");
            String testClassName = recordName + "RestClientTest";

            StringBuilder sb = new StringBuilder();
            sb.append("package ").append(testPackage).append(";\n\n");
            sb.append("public class ").append(testClassName).append(" {\n");
            sb.append("    // TODO: Implement REST tests for ").append(recordName).append("\n");
            sb.append("}\n");

            String outputRelativePath = "src/test/java/" + testPackage.replace(".", "/") + "/" + testClassName + ".java";
            Path outputPath = Paths.get(outputRelativePath);
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, sb.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("Generated Test Rest: " + outputRelativePath);
        } catch (Exception e) {
            System.err.println("Failed to generate Test Rest: " + e.getMessage());
        }
    }

    private static void generateTestService(String sourceRecord, List<String[]> parsedFields) {
        try {
            int lastDot = sourceRecord.lastIndexOf('.');
            String originalPackage = sourceRecord.substring(0, lastDot);
            String recordName = sourceRecord.substring(lastDot + 1);

            String testPackage = originalPackage.replace(".entity", ".services");
            String testClassName = recordName + "ServiceTest";

            StringBuilder sb = new StringBuilder();
            sb.append("package ").append(testPackage).append(";\n\n");
            sb.append("public class ").append(testClassName).append(" {\n");
            sb.append("    // TODO: Implement Service tests for ").append(recordName).append("\n");
            sb.append("}\n");

            String outputRelativePath = "src/test/java/" + testPackage.replace(".", "/") + "/" + testClassName + ".java";
            Path outputPath = Paths.get(outputRelativePath);
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, sb.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("Generated Test Service: " + outputRelativePath);
        } catch (Exception e) {
            System.err.println("Failed to generate Test Service: " + e.getMessage());
        }
    }

    private static void generateTestPage(String sourceRecord, List<String[]> parsedFields) {
        try {
            int lastDot = sourceRecord.lastIndexOf('.');
            String originalPackage = sourceRecord.substring(0, lastDot);
            String recordName = sourceRecord.substring(lastDot + 1);

            String testPackage = originalPackage.replace(".entity", ".pages");
            String testClassName = recordName + "PageTest";

            StringBuilder sb = new StringBuilder();
            sb.append("package ").append(testPackage).append(";\n\n");
            sb.append("public class ").append(testClassName).append(" {\n");
            sb.append("    // TODO: Implement Page tests for ").append(recordName).append("\n");
            sb.append("}\n");

            String outputRelativePath = "src/test/java/" + testPackage.replace(".", "/") + "/" + testClassName + ".java";
            Path outputPath = Paths.get(outputRelativePath);
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, sb.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("Generated Test Page: " + outputRelativePath);
        } catch (Exception e) {
            System.err.println("Failed to generate Test Page: " + e.getMessage());
        }
    }


    private static void generateRepository(String sourceRecord, List<String[]> parsedFields) {
        try {
            int lastDot = sourceRecord.lastIndexOf('.');
            String originalPackage = sourceRecord.substring(0, lastDot);
            String recordName = sourceRecord.substring(lastDot + 1);
            String repoPackage = originalPackage.replace(".entity", ".repository");
            String repoName = recordName + "Repository";
            String repoImplName = recordName + "RepositoryImpl";

            String idType = "String";
            if (parsedFields != null && !parsedFields.isEmpty()) {
                String potentialIdType = parsedFields.get(0)[0];
                for (String[] field : parsedFields) {
                    if (field[1].toLowerCase().equals("id")) {
                        idType = field[0];
                        break;
                    }
                }
                if (idType.equals("String")) {
                    idType = potentialIdType;
                }
            }

            // Interface
            StringBuilder sb = new StringBuilder();
            sb.append("package ").append(repoPackage).append(";\n\n");
            sb.append("import ").append(sourceRecord).append(";\n");
            sb.append("import java.util.List;\n");
            sb.append("import java.util.Optional;\n\n");
            sb.append("public interface ").append(repoName).append(" {\n");
            sb.append("    List<").append(recordName).append("> findAll();\n");
            sb.append("    List<").append(recordName).append("> findAll(int page, int size);\n");
            sb.append("    void save(").append(recordName).append(" record);\n");
            sb.append("    void delete(").append(idType).append(" id);\n");
            sb.append("    Optional<").append(recordName).append("> findById(").append(idType).append(" id);\n");
            
            if (parsedFields != null) {
                for (String[] field : parsedFields) {
                    String type = field[0];
                    String name = field[1];
                    sb.append("    List<").append(recordName).append("> findBy").append(capitalize(name)).append("(").append(type).append(" ").append(name).append(");\n");
                }
            }
            sb.append("}\n");

            Path outPath = Paths.get("src/main/java/" + repoPackage.replace(".", "/") + "/" + repoName + ".java");
            Files.createDirectories(outPath.getParent());
            Files.write(outPath, sb.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("Generated Repository Interface: " + outPath.toString());

            // Implementation
            StringBuilder sbi = new StringBuilder();
            sbi.append("package ").append(repoPackage).append(";\n\n");
            sbi.append("import ").append(sourceRecord).append(";\n");
            sbi.append("import io.jettra.scoped.ApplicationScoped;\n");
            sbi.append("import java.util.ArrayList;\n");
            sbi.append("import java.util.List;\n");
            sbi.append("import java.util.Optional;\n");
            sbi.append("import java.util.stream.Collectors;\n\n");
            sbi.append("@ApplicationScoped\n");
            sbi.append("public class ").append(repoImplName).append(" implements ").append(repoName).append(" {\n\n");
            sbi.append("    private static final List<").append(recordName).append("> db = new ArrayList<>();\n\n");
            sbi.append("    @Override\n");
            sbi.append("    public List<").append(recordName).append("> findAll() {\n");
            sbi.append("        return new ArrayList<>(db);\n");
            sbi.append("    }\n\n");
            sbi.append("    @Override\n");
            sbi.append("    public List<").append(recordName).append("> findAll(int page, int size) {\n");
            sbi.append("        int from = (page - 1) * size;\n");
            sbi.append("        if (from >= db.size()) return new ArrayList<>();\n");
            sbi.append("        int to = Math.min(from + size, db.size());\n");
            sbi.append("        return new ArrayList<>(db.subList(from, to));\n");
            sbi.append("    }\n\n");
            
            String idField = parsedFields != null && !parsedFields.isEmpty() ? parsedFields.get(0)[1] : "id";
            for (String[] field : parsedFields) {
                if (field[1].toLowerCase().equals("id")) {
                    idField = field[1];
                    break;
                }
            }

            sbi.append("    @Override\n");
            sbi.append("    public void save(").append(recordName).append(" record) {\n");
            sbi.append("        delete(record.").append(idField).append("());\n");
            sbi.append("        db.add(record);\n");
            sbi.append("    }\n\n");

            sbi.append("    @Override\n");
            sbi.append("    public void delete(").append(idType).append(" id) {\n");
            sbi.append("        db.removeIf(r -> r.").append(idField).append("().equals(id));\n");
            sbi.append("    }\n\n");

            sbi.append("    @Override\n");
            sbi.append("    public Optional<").append(recordName).append("> findById(").append(idType).append(" id) {\n");
            sbi.append("        return db.stream().filter(r -> r.").append(idField).append("().equals(id)).findFirst();\n");
            sbi.append("    }\n");

            if (parsedFields != null) {
                for (String[] field : parsedFields) {
                    String type = field[0];
                    String name = field[1];
                    sbi.append("\n    @Override\n");
                    sbi.append("    public List<").append(recordName).append("> findBy").append(capitalize(name)).append("(").append(type).append(" ").append(name).append(") {\n");
                    sbi.append("        return db.stream().filter(r -> r.").append(name).append("().equals(").append(name).append(")).collect(Collectors.toList());\n");
                    sbi.append("    }\n");
                }
            }
            sbi.append("}\n");

            Path outImplPath = Paths.get("src/main/java/" + repoPackage.replace(".", "/") + "/" + repoImplName + ".java");
            Files.write(outImplPath, sbi.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("Generated Repository Implementation: " + outImplPath.toString());
            
        } catch (Exception e) {
            System.err.println("Failed to generate Repository: " + e.getMessage());
        }
    }

    private static void generateController(String sourceRecord, List<String[]> parsedFields) {
        try {
            int lastDot = sourceRecord.lastIndexOf('.');
            String originalPackage = sourceRecord.substring(0, lastDot);
            String recordName = sourceRecord.substring(lastDot + 1);
            
            String controllerPackage = originalPackage.replace(".entity", ".controller");
            String controllerName = recordName + "Controller";
            String repoPackage = originalPackage.replace(".entity", ".repository");
            String repoName = recordName + "Repository";
            
            String baseUriModule = "api";
            String[] pkgParts = originalPackage.split("\\.");
            if (pkgParts.length > 2) {
                baseUriModule = pkgParts[pkgParts.length - 2];
            }
            String endpointPath = "/plugin/" + baseUriModule + "/" + recordName.toLowerCase();

            String idType = "String";
            if (parsedFields != null && !parsedFields.isEmpty()) {
                String potentialIdType = parsedFields.get(0)[0];
                for (String[] field : parsedFields) {
                    if (field[1].toLowerCase().equals("id")) {
                        idType = field[0];
                        break;
                    }
                }
                if (idType.equals("String")) {
                    idType = potentialIdType;
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("package ").append(controllerPackage).append(";\n\n");
            
            sb.append("import ").append(sourceRecord).append(";\n");
            sb.append("import ").append(repoPackage).append(".").append(repoName).append(";\n");
            sb.append("import io.jettra.core.inject.annotation.Inject;\n");
            sb.append("import io.jettra.rest.annotations.Consumes;\n");
            sb.append("import io.jettra.rest.annotations.DELETE;\n");
            sb.append("import io.jettra.rest.annotations.GET;\n");
            sb.append("import io.jettra.rest.annotations.POST;\n");
            sb.append("import io.jettra.rest.annotations.PUT;\n");
            sb.append("import io.jettra.rest.annotations.Path;\n");
            sb.append("import io.jettra.rest.annotations.PathParam;\n");
            sb.append("import io.jettra.rest.annotations.QueryParam;\n");
            sb.append("import io.jettra.rest.annotations.Produces;\n");
            sb.append("import io.jettra.rest.annotations.Secured;\n");
            sb.append("import io.jettra.rest.annotations.accreditation.RolesAllowed;\n");
            sb.append("import io.jettra.rest.core.Response;\n");
            sb.append("import io.jettra.server.discoverer.Discovered;\n");
            sb.append("import io.jettra.server.openapi.annotations.OpenApi;\n");
            sb.append("import io.jettra.server.openapi.annotations.Operation;\n");
            sb.append("import jcf.systemRole;\n");
            sb.append("import java.util.List;\n\n");

            sb.append("@Secured\n");
            sb.append("@Path(\"").append(endpointPath).append("\")\n");
            sb.append("@RolesAllowed({systemRole.ADMIN})\n");
            sb.append("@Discovered\n");
            sb.append("@OpenApi(title = \"").append(recordName).append("\", version = \"v1.0\", description = \"API for ").append(recordName).append(" management\")\n");
            sb.append("public class ").append(controllerName).append(" {\n\n");
            
            sb.append("    @Inject\n");
            sb.append("    private ").append(repoName).append(" ").append(repoName.substring(0,1).toLowerCase()).append(repoName.substring(1)).append(";\n\n");

            sb.append("    @GET\n");
            sb.append("    @Path(\"/\")\n");
            sb.append("    @Produces(\"application/json\")\n");
            sb.append("    @Operation(summary = \"findAll\", description = \"Returns all records\")\n");
            sb.append("    public List<").append(recordName).append("> findAll(@QueryParam(\"page\") Integer page, @QueryParam(\"size\") Integer size) {\n");
            sb.append("        if (page != null && size != null) {\n");
            sb.append("            return ").append(repoName.substring(0,1).toLowerCase()).append(repoName.substring(1)).append(".findAll(page, size);\n");
            sb.append("        }\n");
            sb.append("        return ").append(repoName.substring(0,1).toLowerCase()).append(repoName.substring(1)).append(".findAll();\n");
            sb.append("    }\n\n");

            sb.append("    @POST\n");
            sb.append("    @Consumes(\"application/json\")\n");
            sb.append("    @Produces(\"application/json\")\n");
            sb.append("    @Operation(summary = \"save\", description = \"Saves a new ").append(recordName).append("\")\n");
            sb.append("    public Response save(").append(recordName).append(" record) {\n");
            sb.append("        ").append(repoName.substring(0,1).toLowerCase()).append(repoName.substring(1)).append(".save(record);\n");
            sb.append("        return Response.ok(\"{\\\"message\\\": \\\"Saved successfully\\\"}\").build();\n");
            sb.append("    }\n\n");

            sb.append("    @PUT\n");
            sb.append("    @Consumes(\"application/json\")\n");
            sb.append("    @Produces(\"application/json\")\n");
            sb.append("    @Operation(summary = \"update\", description = \"Updates an existing ").append(recordName).append("\")\n");
            sb.append("    public Response update(").append(recordName).append(" record) {\n");
            sb.append("        ").append(repoName.substring(0,1).toLowerCase()).append(repoName.substring(1)).append(".save(record);\n");
            sb.append("        return Response.ok(\"{\\\"message\\\": \\\"Updated successfully\\\"}\").build();\n");
            sb.append("    }\n\n");

            sb.append("    @DELETE\n");
            sb.append("    @Path(\"/{id}\")\n");
            sb.append("    @Produces(\"application/json\")\n");
            sb.append("    @Operation(summary = \"delete\", description = \"Deletes a ").append(recordName).append(" by id\")\n");
            sb.append("    public Response delete(@PathParam(\"id\") ").append(idType).append(" id) {\n");
            sb.append("        ").append(repoName.substring(0,1).toLowerCase()).append(repoName.substring(1)).append(".delete(id);\n");
            sb.append("        return Response.ok(\"{\\\"message\\\": \\\"Deleted successfully\\\"}\").build();\n");
            sb.append("    }\n");
            
            if (parsedFields != null) {
                for (String[] field : parsedFields) {
                    String type = field[0];
                    String name = field[1];
                    sb.append("\n    @GET\n");
                    sb.append("    @Path(\"/").append(name.toLowerCase()).append("/{").append(name).append("}\")\n");
                    sb.append("    @Produces(\"application/json\")\n");
                    sb.append("    @Operation(summary = \"findBy").append(capitalize(name)).append("\", description = \"Finds records by ").append(name).append("\")\n");
                    sb.append("    public List<").append(recordName).append("> findBy").append(capitalize(name)).append("(@PathParam(\"").append(name).append("\") ").append(type).append(" ").append(name).append(") {\n");
                    sb.append("        return ").append(repoName.substring(0,1).toLowerCase()).append(repoName.substring(1)).append(".findBy").append(capitalize(name)).append("(").append(name).append(");\n");
                    sb.append("    }\n");
                }
            }

            sb.append("}\n");
            
            Path outPath = Paths.get("src/main/java/" + controllerPackage.replace(".", "/") + "/" + controllerName + ".java");
            Files.createDirectories(outPath.getParent());
            Files.write(outPath, sb.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("Generated Controller: " + outPath.toString());
            
        } catch (Exception e) {
            System.err.println("Failed to generate Controller: " + e.getMessage());
        }
    }

    private static void initializeFrontEnd() {
        try {
            Path pomPath = Paths.get("pom.xml");
            if (!Files.exists(pomPath)) {
                System.err.println("Error: pom.xml not found in current directory.");
                return;
            }

            String pomContent = new String(Files.readAllBytes(pomPath), StandardCharsets.UTF_8);

            String groupId = extractXmlTag(pomContent, "groupId");
            String artifactId = extractXmlTag(pomContent, "artifactId");
            String version = extractXmlTag(pomContent, "version");
            String name = extractXmlTag(pomContent, "name");
            String packaging = extractXmlTag(pomContent, "packaging");

            if (groupId == null || groupId.isEmpty()) groupId = "com.example.web";
            if (artifactId == null || artifactId.isEmpty()) artifactId = "MiExample";
            if (version == null || version.isEmpty()) version = "1.0-SNAPSHOT";
            if (name == null || name.isEmpty()) name = artifactId;
            if (packaging == null || packaging.isEmpty()) packaging = "jar";

            String mainPackage = groupId;
            Path srcMainJava = Paths.get("src/main/java");
            if (Files.exists(srcMainJava)) {
                try (java.util.stream.Stream<Path> stream = Files.walk(srcMainJava)) {
                    java.util.Optional<Path> javaFileOpt = stream.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".java")).findFirst();
                    if (javaFileOpt.isPresent()) {
                        String fileContent = new String(Files.readAllBytes(javaFileOpt.get()), StandardCharsets.UTF_8);
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("package\\s+([^;]+);").matcher(fileContent);
                        if (m.find()) {
                            mainPackage = m.group(1).trim();
                        }
                    } else {
                        Path packageDir = findDeepestDirectory(srcMainJava);
                        if (packageDir != null && !packageDir.equals(srcMainJava)) {
                            String rel = srcMainJava.relativize(packageDir).toString().replace('/', '.').replace('\\', '.');
                            if (!rel.isEmpty()) {
                                mainPackage = rel;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            System.out.println("Configuring project pom.xml...");
            String newPom = generateFullPom(groupId, artifactId, version, name, packaging, mainPackage);
            Files.write(pomPath, newPom.getBytes(StandardCharsets.UTF_8));
            System.out.println("Updated pom.xml");

            Path resourcesDir = Paths.get("src/main/resources");
            Files.createDirectories(resourcesDir);

            System.out.println("Generating jettra-config.properties...");
            String jettraConfig = generateJettraConfigProps(name, artifactId);
            Files.write(resourcesDir.resolve("jettra-config.properties"), jettraConfig.getBytes(StandardCharsets.UTF_8));

            generateMessagesProperties(resourcesDir);

            Path mainPackagePath = Paths.get("src/main/java/" + mainPackage.replace('.', '/'));
            Files.createDirectories(mainPackagePath);

            System.out.println("Generating " + mainPackage + ".App.java...");
            Files.write(mainPackagePath.resolve("App.java"), generateAppClass(mainPackage).getBytes(StandardCharsets.UTF_8));

            Path loginPath = mainPackagePath.resolve("login");
            Files.createDirectories(loginPath);
            System.out.println("Generating " + mainPackage + ".login.ForgotPasswordPage.java...");
            Files.write(loginPath.resolve("ForgotPasswordPage.java"), generateForgotPasswordPageClass(mainPackage).getBytes(StandardCharsets.UTF_8));
            System.out.println("Generating " + mainPackage + ".login.LoginPage.java...");
            Files.write(loginPath.resolve("LoginPage.java"), generateLoginPageClass(mainPackage).getBytes(StandardCharsets.UTF_8));

            Path templatePath = mainPackagePath.resolve("template");
            Files.createDirectories(templatePath);
            System.out.println("Generating " + mainPackage + ".template.TemplatePage.java...");
            Files.write(templatePath.resolve("TemplatePage.java"), generateTemplatePageClass(mainPackage).getBytes(StandardCharsets.UTF_8));

            Path dashboardPath = mainPackagePath.resolve("dashboard");
            Files.createDirectories(dashboardPath);
            System.out.println("Generating " + mainPackage + ".dashboard.DashboardPage.java...");
            Files.write(dashboardPath.resolve("DashboardPage.java"), generateDashboardPageClass(mainPackage).getBytes(StandardCharsets.UTF_8));

            Path entityPath = mainPackagePath.resolve("entity");
            Files.createDirectories(entityPath);
            System.out.println("Generating " + mainPackage + ".entity.Person.java...");
            Files.write(entityPath.resolve("Person.java"), generatePersonEntityClass(mainPackage).getBytes(StandardCharsets.UTF_8));

            Path modelPath = mainPackagePath.resolve("model");
            Files.createDirectories(modelPath);
            System.out.println("Generating " + mainPackage + ".model.PersonModel.java...");
            Files.write(modelPath.resolve("PersonModel.java"), generatePersonModelClass(mainPackage).getBytes(StandardCharsets.UTF_8));

            Path pagePath = mainPackagePath.resolve("page");
            Files.createDirectories(pagePath);
            System.out.println("Generating " + mainPackage + ".page.PersonPage.java...");
            Files.write(pagePath.resolve("PersonPage.java"), generatePersonPageClass(mainPackage).getBytes(StandardCharsets.UTF_8));

            Path testPagePath = Paths.get("src/test/java/" + mainPackage.replace('.', '/') + "/page");
            Files.createDirectories(testPagePath);
            System.out.println("Generating " + mainPackage + ".page.PersonPageTest.java...");
            Files.write(testPagePath.resolve("PersonPageTest.java"), generatePersonPageTestClass(mainPackage).getBytes(StandardCharsets.UTF_8));

            System.out.println("Generating Dockerfile...");
            String serverPort = "9010";
            for (String line : jettraConfig.split("\n")) {
                if (line.startsWith("server.port=")) {
                    serverPort = line.substring("server.port=".length()).trim();
                    break;
                }
            }
            Files.write(Paths.get("Dockerfile"), generateDockerfileContent(artifactId, version, serverPort).getBytes(StandardCharsets.UTF_8));

            System.out.println("\nFrontend initialization completed successfully!");

        } catch (Exception e) {
            System.err.println("Error initializing front-end: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String generateDockerfileContent(String artifactId, String version, String port) {
        String jarName = artifactId + "-" + version + ".jar";
        String appJarName = artifactId + ".jar";
        return "# Empleando una imagen ligera orientada a un runtime óptimo y moderno\n" +
               "# Usando BellSoft Liberica JRE con Alpaquita Linux para menor tamaño y mayor rendimiento\n" +
               "FROM bellsoft/liberica-runtime-container:jre-25-stream-musl\n\n" +
               "LABEL maintainer=\"Jettra Development Team\"\n" +
               "LABEL description=\"Contenedor de Demostración JettraBackEnd\"\n\n" +
               "WORKDIR /app\n\n" +
               "# Copia el JAR compilado desde target de su entorno en Host\n" +
               "COPY target/" + jarName + "  /app/" + appJarName + "\n\n" +
               "# Entrenar la JVM para generar el AOT Cache (AppCDS)\n" +
               "# Ejecutamos la app por 10 segundos para cargar las clases y luego forzamos la salida guardando el archivo .jsa\n" +
               "RUN timeout 30s java -XX:ArchiveClassesAtExit=app.jsa -jar " + appJarName + " || true\n\n" +
               "# Exponer el puerto por defecto (configurable via -e variables de entorno o property config)\n" +
               "EXPOSE " + port + "\n\n" +
               "# Documenta que el contenedor utiliza persistencia en este directorio\n" +
               "VOLUME [\"/app/db\"]\n\n" +
               "# Opciones de JVM para optimización de rendimiento en contenedores y habilitación de AOT Cache\n" +
               "ENV JAVA_OPTS=\"-XX:+UseCompactObjectHeaders -XX:+UseZGC -XX:+ZGenerational -Xmx512m -XX:MaxRAMPercentage=75.0 -XX:SharedArchiveFile=app.jsa\"\n\n" +
               "ENTRYPOINT [\"sh\", \"-c\", \"java $JAVA_OPTS -jar " + appJarName + "\"]\n\n" +
               "# Configuracion para el Shell de JettraSecurityDB\n" +
               "CMD []\n";
    }

    private static String extractXmlTag(String content, String tag) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("<" + tag + ">(.*?)</" + tag + ">");
        java.util.regex.Matcher m = p.matcher(content);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private static Path findDeepestDirectory(Path dir) throws java.io.IOException {
        Path result = dir;
        try (java.util.stream.Stream<Path> stream = Files.walk(dir)) {
            List<Path> dirs = stream.filter(Files::isDirectory).collect(java.util.stream.Collectors.toList());
            if (!dirs.isEmpty()) {
                result = dirs.get(dirs.size() - 1);
            }
        }
        return result;
    }

    private static String generateJettraConfigProps(String name, String artifactId) {
        String shortTitle = artifactId.replaceAll("[^A-Z]", "");
        if (shortTitle.isEmpty()) {
            shortTitle = artifactId.length() >= 2 ? artifactId.substring(0, 2).toUpperCase() : artifactId.toUpperCase();
        }
        String lowerArtifact = artifactId.toLowerCase();
        return "app.title=" + name + "\n" +
               "app.shorttitle=" + shortTitle + "\n" +
               "server.port=9010\n" +
               "server.contextpath=/" + lowerArtifact + "\n" +
               "server.compactheader=true\n" +
               "server.session.timeout=0\n" +
               "app.language=es\n" +
               "app.theme=sai\n" +
               "app.animated=false\n" +
               "server.hotreload=true\n" +
               "baseUri=http://localhost:9010/" + lowerArtifact + "\n" +
               "server.typebackend=true\n" +
               "#JWT Security\n" +
               "server.JWT_SECRET = default_secret_key_jettra_rest_2026\n" +
               "server.JWT_EXPIRATION=3600000\n" +
               "server.consoleshowregisterpage=false\n" +
               "app.roles=ADMIN,MANAGER, USER, SXRM\n";
    }

    private static void generateMessagesProperties(Path resourcesDir) throws java.io.IOException {
        String msgDefault = "person.name = Name\n" +
                            "person.email = Email\n" +
                            "person.age = Age\n" +
                            "personpage.title = Persona\n" +
                            "personpage.subtitle = Registros de Personas\n" +
                            "btn.save = Save\n" +
                            "app.label.success = Record saved successfully.\n" +
                            "app.label.error = An error occurred while saving the record.\n" +
                            "app.label.enteryour = Enter your \n";

        String msgEs = "# Application\n" +
                       "btn.add=Agregar\n" +
                       "btn.update=Actualizar\n" +
                       "btn.edit=Editar\n" +
                       "btn.delete=Eliminar\n" +
                       "btn.print=Imprimir\n" +
                       "btn.save=Guardar\n" +
                       "btn.cancel=Cancelar\n" +
                       "pager.prev=Anterior\n" +
                       "pager.next=Siguiente\n" +
                       "app.label.enteryour=Ingrese su\n\n" +
                       "# Person\n" +
                       "personpage.title=Persona\n" +
                       "personpage.subtitle=Registros de Personas\n" +
                       "person.name=Nombre\n" +
                       "person.email=Correo electrónico\n" +
                       "person.age=Edad\n\n" +
                       "app.label.success=Registro guardado exitosamente.\n" +
                       "app.label.error=Ocurrió un error al guardar el registro.\n";

        String msgEn = "# Application\n" +
                       "btn.add=Add\n" +
                       "btn.update=Update\n" +
                       "btn.edit=Edit\n" +
                       "btn.delete=Delete\n" +
                       "btn.print=Print\n" +
                       "btn.save=Save\n" +
                       "btn.cancel=Cancel\n" +
                       "pager.prev=Previous\n" +
                       "pager.next=Next\n" +
                       "app.label.enteryour=Enter your\n\n" +
                       "# Person\n" +
                       "personpage.title=Person\n" +
                       "personpage.subtitle=Person Records\n" +
                       "person.name=Name\n" +
                       "person.email=Email\n" +
                       "person.age=Age\n\n" +
                       "app.label.success=Record saved successfully.\n" +
                       "app.label.error=An error occurred while saving the record.\n";

        Files.write(resourcesDir.resolve("messages.properties"), msgDefault.getBytes(StandardCharsets.UTF_8));
        Files.write(resourcesDir.resolve("messages_es.properties"), msgEs.getBytes(StandardCharsets.UTF_8));
        Files.write(resourcesDir.resolve("messages_en.properties"), msgEn.getBytes(StandardCharsets.UTF_8));
    }

    private static String generateFullPom(String groupId, String artifactId, String version, String name, String packaging, String mainPackage) {
        return "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
               "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
               "    <modelVersion>4.0.0</modelVersion>\n" +
               "    <groupId>" + groupId + "</groupId>\n" +
               "    <artifactId>" + artifactId + "</artifactId>\n" +
               "    <packaging>" + packaging + "</packaging>\n" +
               "    <version>" + version + "</version>\n" +
               "    <name>" + name + "</name>\n" +
               "    <url>http://maven.apache.org</url>\n" +
               "    <properties>\n" +
               "        <maven.compiler.source>25</maven.compiler.source>\n" +
               "        <maven.compiler.target>25</maven.compiler.target>\n" +
               "        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n" +
               "        <skipTests>true</skipTests>\n" +
               "        <jettra.annotation.version>1.0.0-SNAPSHOT</jettra.annotation.version>\n" +
               "        <jettra.jwt.version>1.0.0-SNAPSHOT</jettra.jwt.version>\n" +
               "        <jettra.gprc.version>1.0.0-SNAPSHOT</jettra.gprc.version>\n" +
               "        <jettra.rules.version>1.0.0-SNAPSHOT</jettra.rules.version>\n" +
               "        <jettra.appserver.version>1.0.0-SNAPSHOT</jettra.appserver.version>\n" +
               "        <jettra.report.version>1.0.0-SNAPSHOT</jettra.report.version>\n" +
               "        <jettra.rest.version>1.0.0-SNAPSHOT</jettra.rest.version>\n" +
               "        <jettra.json.version>1.0.0-SNAPSHOT</jettra.json.version>\n" +
               "        <jettra.test.version>1.0.0-SNAPSHOT</jettra.test.version>\n" +
               "        <jettra.flux.version>1.0.0-SNAPSHOT</jettra.flux.version>\n" +
               "        <main.class.path>" + mainPackage + ".App</main.class.path>\n" +
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
               "                        <path>\n" +
               "                            <groupId>io.jettra</groupId>\n" +
               "                            <artifactId>JettraFlux</artifactId>\n" +
               "                            <version>${jettra.flux.version}</version>\n" +
               "                        </path>\n" +
               "                    </annotationProcessorPaths>\n" +
               "                </configuration>\n" +
               "            </plugin>\n" +
               "            <plugin>\n" +
               "                <groupId>org.apache.maven.plugins</groupId>\n" +
               "                <artifactId>maven-jar-plugin</artifactId>\n" +
               "                <version>3.3.0</version>\n" +
               "                <configuration>\n" +
               "                    <archive>\n" +
               "                        <manifest>\n" +
               "                            <mainClass>${main.class.path}</mainClass>\n" +
               "                        </manifest>\n" +
               "                    </archive>\n" +
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
               "                            <transformers>\n" +
               "                                <transformer implementation=\"org.apache.maven.plugins.shade.resource.ManifestResourceTransformer\">\n" +
               "                                    <mainClass>${main.class.path}</mainClass>\n" +
               "                                </transformer>\n" +
               "                                <transformer implementation=\"org.apache.maven.plugins.shade.resource.AppendingTransformer\">\n" +
               "                                    <resource>META-INF/jettra/discovered.classes</resource>\n" +
               "                                </transformer>\n" +
               "                            </transformers>\n" +
               "                        </configuration>\n" +
               "                    </execution>\n" +
               "                </executions>\n" +
               "            </plugin>\n" +
               "            <plugin>\n" +
               "                <groupId>org.codehaus.mojo</groupId>\n" +
               "                <artifactId>exec-maven-plugin</artifactId>\n" +
               "                <version>3.1.0</version>\n" +
               "                <executions>\n" +
               "                    <execution>\n" +
               "                        <id>jettra-test</id>\n" +
               "                        <phase>test</phase>\n" +
               "                        <goals>\n" +
               "                            <goal>java</goal>\n" +
               "                        </goals>\n" +
               "                        <configuration>\n" +
               "                            <mainClass>io.jettra.test.runner.JettraTestRunner</mainClass>\n" +
               "                            <classpathScope>test</classpathScope>\n" +
               "                            <arguments>\n" +
               "                                <argument>${project.build.testOutputDirectory}</argument>\n" +
               "                            </arguments>\n" +
               "                        </configuration>\n" +
               "                    </execution>\n" +
               "                </executions>\n" +
               "            </plugin>\n" +
               "        </plugins>\n" +
               "    </build>\n" +
               "    <repositories>\n" +
               "        <repository>\n" +
               "            <id>jitpack.io</id>\n" +
               "            <url>https://jitpack.io</url>\n" +
               "        </repository>\n" +
               "    </repositories>\n" +
               "</project>\n";
    }

    private static String generateAppClass(String pkg) {
        return "package " + pkg + ";\n\n" +
               "import io.jettra.rest.server.JettraRestServer;\n" +
               "import io.jettra.server.JettraServer;\n" +
               "import io.jettra.server.config.ConfigInjector;\n" +
               "import io.jettra.server.config.JettraConfigProperty;\n" +
               "import io.jettra.server.discoverer.DiscoveredLoad;\n" +
               "import io.jettra.server.openapi.OpenApiHandler;\n" +
               "import io.jettra.server.openapi.SwaggerUIHandler;\n" +
               "import java.util.List;\n\n" +
               "@DiscoveredLoad\n" +
               "public class App {\n\n" +
               "    @JettraConfigProperty(name = \"app.title\")\n" +
               "    private String appTitle;\n" +
               "    @JettraConfigProperty(name = \"server.port\")\n" +
               "    private String port;\n" +
               "    @JettraConfigProperty(name = \"server.contextpath\")\n" +
               "    private String contextpath;\n" +
               "    public static JettraServer serverInstance;\n\n" +
               "    public void initUI() {\n" +
               "        ConfigInjector.inject(this);\n" +
               "        System.out.println(\"Iniciando aplicación Web: \" + appTitle);\n" +
               "    }\n\n" +
               "    public static void main(String[] args) {\n" +
               "        if (args != null && args.length > 0 && args[0].equals(\"-console\")) {\n" +
               "            io.jettra.server.autentification.SecurityCLI.main(args);\n" +
               "            return;\n" +
               "        }\n" +
               "        if (args != null && args.length > 0 && args[0].equals(\"-generate-flux-jettra-sh\")) {\n" +
               "            io.jettra.server.JettraServer.generateMvnScripts();\n" +
               "            return;\n" +
               "        }\n\n" +
               "        App app = new App();\n" +
               "        app.initUI();\n" +
               "        io.jettra.flux.complex.ErrorPage.path = \"http://localhost:\" + app.port + app.contextpath;\n\n" +
               "        System.out.println(\"Levantando servidor de enrutamiento JettraServer empotrado...\");\n" +
               "        JettraServer server = new JettraServer();\n" +
               "        server.setErrorPage(\"/error\");\n" +
               "        server.addHandler(\"/error\", io.jettra.flux.complex.ErrorPage.class);\n" +
               "        server.addHandler(\"/swagger-ui\", io.jettra.flux.complex.SwaggerUIPage.class);\n\n" +
               "        // Registro de Páginas JettraFlux\n" +
               "        server.addHandler(\"/\", " + pkg + ".login.LoginPage.class);\n\n" +
               "        List<Class<?>> controllers = new java.util.ArrayList<>(io.jettra.server.discoverer.DiscoveredRegistry.getDiscoveredClasses(App.class));\n\n" +
               "        server.addHandler(\"/openapi.json\", new OpenApiHandler(controllers));\n" +
               "        server.addHandler(\"/swagger-ui\", new SwaggerUIHandler(\"/openapi.json\"));\n\n" +
               "        JettraRestServer.registerDiscovered(server, App.class);\n\n" +
               "        server.start();\n" +
               "    }\n" +
               "}\n";
    }

    private static String generateForgotPasswordPageClass(String pkg) {
        return "package " + pkg + ".login;\n\n" +
               "import io.jettra.flux.pages.FluxBaseHandler;\n" +
               "import io.jettra.flux.widgets.Paragraph;\n" +
               "import io.jettra.flux.widgets.Scaffold;\n" +
               "import io.jettra.flux.widgets.ForgotPassword;\n" +
               "import io.jettra.flux.widgets.Column;\n" +
               "import io.jettra.flux.widgets.Notification;\n" +
               "import io.jettra.flux.widgets.Center;\n" +
               "import com.sun.net.httpserver.HttpExchange;\n" +
               "import io.jettra.core.server.Page;\n" +
               "import io.jettra.flux.core.Widget;\n" +
               "import io.jettra.server.JettraServer;\n\n" +
               "import java.util.Map;\n\n" +
               "@io.jettra.core.login.NoLoginRequired\n" +
               "@Page(path = \"/forgot-password\")\n" +
               "public class ForgotPasswordPage extends FluxBaseHandler {\n\n" +
               "    @Override\n" +
               "    protected String getTitle() {\n" +
               "        return \"Forgot Password - JettraFlux\";\n" +
               "    }\n\n" +
               "    @Override\n" +
               "    protected boolean onPost(HttpExchange exchange, Map<String, String> params) throws java.io.IOException {\n" +
               "        if (params.containsKey(\"email\")) {\n" +
               "            String email = params.get(\"email\");\n" +
               "            System.out.println(\"[ForgotPasswordPage] Reset link requested for: \" + email);\n" +
               "            redirect(exchange, \"/forgot-password?success=true\");\n" +
               "            return true;\n" +
               "        }\n" +
               "        return false;\n" +
               "    }\n\n" +
               "    @Override\n" +
               "    protected Widget buildUI(HttpExchange exchange, Map<String, String> params, String currentTheme) {\n" +
               "        Widget form = ForgotPassword.create().action(JettraServer.resolvePath(\"/forgot-password\")).title(\"Reset Password\").logo(\"https://primefaces.org/cdn/primeng/images/galleria/galleria1.jpg\");\n" +
               "        \n" +
               "        Widget body = Center.of(\n" +
               "            Column.of(\n" +
               "                form\n" +
               "            )\n" +
               "        );\n\n" +
               "        if (params.containsKey(\"success\")) {\n" +
               "            Widget successAlert = Notification.of(Paragraph.of(\"A password reset link has been sent to your email.\"));\n" +
               "            body = Center.of(\n" +
               "                Column.of(\n" +
               "                    successAlert,\n" +
               "                    form\n" +
               "                )\n" +
               "            );\n" +
               "        }\n\n" +
               "        return Scaffold.of().body(body);\n" +
               "    }\n" +
               "}\n";
    }

    private static String generateLoginPageClass(String pkg) {
        return "package " + pkg + ".login;\n\n" +
               "import io.jettra.flux.pages.FluxBaseHandler;\n" +
               "import io.jettra.flux.widgets.Paragraph;\n" +
               "import io.jettra.flux.widgets.Scaffold;\n" +
               "import io.jettra.flux.widgets.Login;\n" +
               "import io.jettra.flux.widgets.Column;\n" +
               "import io.jettra.flux.widgets.Notification;\n" +
               "import io.jettra.flux.widgets.Center;\n" +
               "import com.sun.net.httpserver.HttpExchange;\n" +
               "import io.jettra.core.server.Page;\n" +
               "import io.jettra.flux.core.Widget;\n" +
               "import io.jettra.flux.model.CredentialFlux;\n" +
               "import io.jettra.json.SessionScoped;\n" +
               "import io.jettra.server.JettraServer;\n\n" +
               "import java.util.Map;\n\n" +
               "@io.jettra.core.login.NoLoginRequired\n" +
               "@SessionScoped\n" +
               "@Page(path = \"/login\")\n" +
               "public class LoginPage extends FluxBaseHandler {\n\n" +
               "    @Override\n" +
               "    protected String getTitle() {\n" +
               "        return \"Login - JettraFlux\";\n" +
               "    }\n\n" +
               "    @Override\n" +
               "    protected boolean onPost(HttpExchange exchange, Map<String, String> params) throws java.io.IOException {\n" +
               "        String user = params.get(\"username\");\n" +
               "        String pass = params.get(\"password\");\n" +
               "        if (user == null || user.trim().isEmpty() || pass == null || pass.trim().isEmpty()) {\n" +
               "            redirect(exchange, \"/login?error=empty_fields\");\n" +
               "            return true;\n" +
               "        }\n" +
               "        if (isValidUser(user, pass)) {\n" +
               "            String role = user.equals(\"demo\") ? \"DEMO\" : \"ADMIN\";\n" +
               "            CredentialFlux credentialFlux = new CredentialFlux(user, user + \"Prueba\", role, \"\", \"\");\n" +
               "            io.jettra.server.core.JettraContext.getCurrent().set(io.jettra.server.core.JettraContext.Scope.SESSION, \"credentialFlux\", credentialFlux);\n" +
               "            setSessionCookie(exchange, user, credentialFlux.role(), credentialFlux.department());\n" +
               "            redirect(exchange, \"/dashboard\");\n" +
               "            return true;\n" +
               "        } else {\n" +
               "            redirect(exchange, \"/login?error=invalid_credentials\");\n" +
               "            return true;\n" +
               "        }\n" +
               "    }\n\n" +
               "    @Override\n" +
               "    protected boolean onGet(HttpExchange exchange, Map<String, String> params) throws java.io.IOException {\n" +
               "        if (\"true\".equals(params.get(\"logout\"))) {\n" +
               "            clearSessionCookie(exchange);\n" +
               "            redirect(exchange, \"/login\");\n" +
               "            return true;\n" +
               "        }\n" +
               "        return false;\n" +
               "    }\n\n" +
               "    @Override\n" +
               "    protected Widget buildUI(HttpExchange exchange, Map<String, String> params, String currentTheme) {\n" +
               "        Widget loginForm = Login.create().action(JettraServer.resolvePath(\"/login\")).title(\"JettraFlux Admin\").logo(\"https://primefaces.org/cdn/primeng/images/galleria/galleria1.jpg\").forgotPasswordUrl(JettraServer.resolvePath(\"/forgot-password\"));\n" +
               "        \n" +
               "        Widget body = Center.of(\n" +
               "            Column.of(\n" +
               "                loginForm\n" +
               "            )\n" +
               "        );\n\n" +
               "        if (params.containsKey(\"error\")) {\n" +
               "            String errorParam = params.get(\"error\");\n" +
               "            String titleStr = \"Advertencia de Autenticación\";\n" +
               "            String msgStr = \"El nombre de usuario o la contraseña ingresados no son válidos. Por favor verifique sus credenciales.\";\n" +
               "            \n" +
               "            if (\"empty_fields\".equals(errorParam) || \"empty\".equals(errorParam)) {\n" +
               "                titleStr = \"Campos Requeridos\";\n" +
               "                msgStr = \"Error: Username y password son requeridos.\";\n" +
               "            }\n\n" +
               "            Widget notificationBanner = Notification.of(\n" +
               "                Paragraph.of(msgStr).modifier(new io.jettra.flux.core.Modifier().style(\"margin: 0; color: #b91c1c; font-weight: 600;\"))\n" +
               "            ).modifier(new io.jettra.flux.core.Modifier().style(\"margin-bottom: 15px; padding: 12px 16px; background-color: #fef2f2; border: 1px solid #fca5a5; border-radius: 8px; width: 100%; max-width: 400px; box-sizing: border-box;\"));\n\n" +
               "            Widget errorDialog = io.jettra.flux.widgets.Modal.of(\n" +
               "                Column.of(\n" +
               "                    io.jettra.flux.widgets.Header.of(4, titleStr)\n" +
               "                            .modifier(new io.jettra.flux.core.Modifier().style(\"color: #b91c1c; margin-top: 0; margin-bottom: 10px; font-weight: 600;\")),\n" +
               "                    Paragraph.of(msgStr)\n" +
               "                            .modifier(new io.jettra.flux.core.Modifier().style(\"margin-bottom: 20px; color: #374151; font-size: 14px;\")),\n" +
               "                    io.jettra.flux.widgets.ElevatedButton.of(\"Aceptar\")\n" +
               "                            .attribute(\"type\", \"button\")\n" +
               "                            .attribute(\"onclick\", \"this.closest('.espresso-modal-overlay').style.display='none'; return false;\")\n" +
               "                            .modifier(new io.jettra.flux.core.Modifier().style(\"background-color: #ef4444; color: white; border: none; padding: 8px 20px; border-radius: 6px; cursor: pointer; font-weight: 500; align-self: flex-end;\"))\n" +
               "                ).modifier(new io.jettra.flux.core.Modifier().style(\"width: 100%; max-width: 400px; gap: 10px;\"))\n" +
               "            ).open(true);\n\n" +
               "            body = Center.of(\n" +
               "                Column.of(\n" +
               "                    notificationBanner,\n" +
               "                    errorDialog,\n" +
               "                    loginForm\n" +
               "                )\n" +
               "            );\n" +
               "        }\n\n" +
               "        return Scaffold.of().body(body);\n" +
               "    }\n\n" +
               "    private boolean isValidUser(String user, String pass) {\n" +
               "        return (\"admin\".equals(user) && \"admin\".equals(pass)) || \n" +
               "               (\"demo\".equals(user) && \"demo\".equals(pass)) || \n" +
               "               (\"avbravo\".equals(user) && \"avbravo\".equals(pass));\n" +
               "    }\n" +
               "}\n";
    }

    private static String generateTemplatePageClass(String pkg) {
        return "package " + pkg + ".template;\n\n" +
               "import io.jettra.flux.pages.FluxBaseHandler;\n" +
               "import com.sun.net.httpserver.HttpExchange;\n" +
               "import io.jettra.flux.core.Widget;\n\n" +
               "import io.jettra.flux.widgets.ActionIcon;\n" +
               "import io.jettra.flux.widgets.Column;\n" +
               "import io.jettra.flux.widgets.Dashboard;\n" +
               "import io.jettra.flux.widgets.Footer;\n" +
               "import io.jettra.flux.widgets.Header;\n" +
               "import io.jettra.flux.widgets.Icon;\n" +
               "import io.jettra.flux.widgets.Left;\n" +
               "import io.jettra.flux.widgets.Paragraph;\n" +
               "import io.jettra.flux.widgets.Row;\n" +
               "import io.jettra.flux.widgets.Scaffold;\n" +
               "import io.jettra.flux.widgets.SidebarCategory;\n" +
               "import io.jettra.flux.widgets.SidebarLogo;\n" +
               "import io.jettra.flux.widgets.ThemeChanged;\n" +
               "import io.jettra.flux.widgets.Top;\n" +
               "import io.jettra.flux.widgets.WidgetLet;\n" +
               "import io.jettra.flux.widgets.NotificationTop;\n" +
               "import io.jettra.server.JettraServer;\n" +
               "import java.util.Map;\n\n" +
               "public abstract class TemplatePage extends FluxBaseHandler {\n\n" +
               "    protected abstract Widget buildCenter(HttpExchange exchange, Map<String, String> params, String currentTheme);\n\n" +
               "    public static NotificationTop getNotificationTop(String idnotification) {\n" +
               "        io.jettra.server.core.JettraContext ctx = io.jettra.server.core.JettraContext.getCurrent();\n" +
               "        if (ctx == null) return null;\n" +
               "        \n" +
               "        Map<String, NotificationTop> notifications = \n" +
               "            (Map<String, NotificationTop>) ctx.get(io.jettra.server.core.JettraContext.Scope.SESSION, \"template_notifications\");\n" +
               "            \n" +
               "        if (notifications == null) {\n" +
               "            notifications = new java.util.HashMap<>();\n" +
               "            ctx.set(io.jettra.server.core.JettraContext.Scope.SESSION, \"template_notifications\", notifications);\n" +
               "        }\n" +
               "        \n" +
               "        NotificationTop nt = notifications.get(idnotification);\n" +
               "        if (nt == null) {\n" +
               "            nt = NotificationTop.of().binding(idnotification);\n" +
               "            notifications.put(idnotification, nt);\n" +
               "        }\n" +
               "        return nt;\n" +
               "    }\n\n" +
               "    @Override\n" +
               "    protected Widget buildUI(HttpExchange exchange, Map<String, String> params, String currentTheme) {\n" +
               "        String username = getLoggedUser(exchange);\n" +
               "        if (username == null || username.isEmpty()) {\n" +
               "            try {\n" +
               "                redirect(exchange, \"/login\");\n" +
               "            } catch (Exception e) {\n" +
               "            }\n" +
               "            return Column.of();\n" +
               "        }\n\n" +
               "        String userInitial = username.substring(0, 1).toUpperCase();\n\n" +
               "        io.jettra.flux.model.CredentialFlux credential = (io.jettra.flux.model.CredentialFlux) io.jettra.server.core.JettraContext.getCurrent().get(io.jettra.server.core.JettraContext.Scope.SESSION, \"credentialFlux\");\n" +
               "        String displayName = username;\n" +
               "        \n" +
               "        Widget photoWidget = io.jettra.flux.widgets.Avatar.label(userInitial).shape(\"circle\")\n" +
               "            .modifier(new io.jettra.flux.core.Modifier().style(\"background-color:#3b82f6; color:white; font-weight:bold; margin-right:8px;\"));\n\n" +
               "        if (credential != null) {\n" +
               "            displayName = credential.name() != null && !credential.name().isEmpty() ? credential.name() : username;\n" +
               "            if (credential.photo() != null && !credential.photo().isEmpty()) {\n" +
               "                photoWidget = io.jettra.flux.widgets.Avatar.image(credential.photo()).shape(\"circle\")\n" +
               "                    .modifier(new io.jettra.flux.core.Modifier().style(\"margin-right:8px;\"));\n" +
               "            } else {\n" +
               "                photoWidget = io.jettra.flux.widgets.Avatar.icon(\"fas fa-user\").shape(\"circle\")\n" +
               "                    .modifier(new io.jettra.flux.core.Modifier().style(\"background-color:#3b82f6; color:white; font-weight:bold; margin-right:8px;\"));\n" +
               "            }\n" +
               "        }\n\n" +
               "        Widget customCss = Paragraph.of(io.jettra.flux.theme.OceanTheme.Template.CustomCSS + \"\\n\" + io.jettra.flux.theme.OceanTheme.Template.CustomJS);\n\n" +
               "        WidgetLet mainCategoryMenu = WidgetLet.of(\"Navigation\").icon(Icon.HOME);\n" +
               "        mainCategoryMenu.add(WidgetLet.of(\"Dashboard\").icon(Icon.CHART_LINE).url(JettraServer.resolvePath(\"/dashboard\")));\n" +
               "        mainCategoryMenu.add(WidgetLet.of(\"Person\").icon(Icon.USER).url(JettraServer.resolvePath(\"/person\")));\n\n" +
               "        Widget menu = Left.of(\n" +
               "                SidebarLogo.of(Icon.LAYER_GROUP, \"Ocean\"),\n" +
               "                SidebarCategory.of(\"Navigation\"),\n" +
               "                mainCategoryMenu\n" +
               "        ).modifier(new io.jettra.flux.core.Modifier().cssClass(\"professional-left\"));\n\n" +
               "        Widget profileTrigger = Row.of(\n" +
               "            photoWidget,\n" +
               "            io.jettra.flux.widgets.Span.of(\"\").modifier(new io.jettra.flux.core.Modifier().style(\"font-weight:bold;\")),\n" +
               "            Icon.of(\"fas fa-caret-down\").modifier(new io.jettra.flux.core.Modifier().style(\"margin-left:5px;\"))\n" +
               "        ).modifier(new io.jettra.flux.core.Modifier().style(\"align-items:center; cursor:pointer;\").attribute(\"title\", displayName));\n\n" +
               "        Widget profileMenu = ((io.jettra.flux.widgets.OverlayMenu) io.jettra.flux.widgets.OverlayMenu.of(\n" +
               "                WidgetLet.of(\"Logout\").icon(Icon.SIGN_OUT_ALT).url(JettraServer.resolvePath(\"/login?logout=true\"))\n" +
               "        ).trigger(profileTrigger)).alignRight();\n\n" +
               "        String cookieHeader = exchange.getRequestHeaders().getFirst(\"Cookie\");\n" +
               "        String currentLang = \"en\";\n" +
               "        if (cookieHeader != null && cookieHeader.contains(\"jettra_lang=es\")) {\n" +
               "            currentLang = \"es\";\n" +
               "        }\n\n" +
               "        Widget langTrigger;\n" +
               "        io.jettra.flux.widgets.WidgetLet langOption;\n" +
               "        if (\"es\".equals(currentLang)) {\n" +
               "            langTrigger = io.jettra.flux.widgets.Span.of(\"🇪🇸\").modifier(new io.jettra.flux.core.Modifier().attribute(\"title\", \"Español\").style(\"cursor:pointer; font-size:1.2rem;\"));\n" +
               "            langOption = (io.jettra.flux.widgets.WidgetLet) io.jettra.flux.widgets.WidgetLet.of(\"🇺🇸\").url(\"?change_lang=en\");\n" +
               "        } else {\n" +
               "            langTrigger = io.jettra.flux.widgets.Span.of(\"🇺🇸\").modifier(new io.jettra.flux.core.Modifier().attribute(\"title\", \"English\").style(\"cursor:pointer; font-size:1.2rem;\"));\n" +
               "            langOption = (io.jettra.flux.widgets.WidgetLet) io.jettra.flux.widgets.WidgetLet.of(\"🇪🇸\").url(\"?change_lang=es\");\n" +
               "        }\n\n" +
               "        Widget langSwitcher = ((io.jettra.flux.widgets.OverlayMenu) io.jettra.flux.widgets.OverlayMenu.of(langOption).trigger(langTrigger)).alignRight();\n\n" +
               "        NotificationTop globalNotif = getNotificationTop(\"global_notif\").type(NotificationTop.NotificationTopType.GLOBAL).icon(Icon.of(\"fas fa-globe\"));\n" +
               "        NotificationTop personalNotif = getNotificationTop(\"personal_notif\").type(NotificationTop.NotificationTopType.PERSONAL).icon(Icon.of(\"fas fa-envelope\"));\n" +
               "        NotificationTop channelNotif = getNotificationTop(\"channel_notif\").type(NotificationTop.NotificationTopType.CHANNEL).channel(\"admin_channel\").icon(Icon.of(\"fas fa-bullhorn\"));\n\n" +
               "        Widget topBar = Top.of(\n" +
               "                Row.of(\n" +
               "                        ActionIcon.of(Icon.BARS + \" top-bars-icon\", \"toggleSidebar()\"),\n" +
               "                        Header.of(4, \"Dashboard\").modifier(new io.jettra.flux.core.Modifier().cssClass(\"top-dashboard-title\"))\n" +
               "                ).modifier(new io.jettra.flux.core.Modifier().cssClass(\"top-left-section\")),\n" +
               "                Row.of(\n" +
               "                        Icon.of(Icon.SEARCH),\n" +
               "                        globalNotif,\n" +
               "                        personalNotif,\n" +
               "                        channelNotif,\n" +
               "                        langSwitcher,\n" +
               "                        ThemeChanged.of().current(currentTheme),\n" +
               "                        profileMenu\n" +
               "                ).modifier(new io.jettra.flux.core.Modifier().cssClass(\"top-right-section\").style(\"gap: 15px; align-items: center;\"))\n" +
               "        );\n\n" +
               "        Widget centerContent = Column.of(\n" +
               "                customCss,\n" +
               "                buildCenter(exchange, params, currentTheme)\n" +
               "        ).modifier(new io.jettra.flux.core.Modifier().cssClass(\"professional-center espresso-center\"));\n\n" +
               "        Widget footerContent = Footer.of(\n" +
               "                Paragraph.of(\"© 2026 JettraStack - JettraFlux\")\n" +
               "        );\n\n" +
               "        Widget body = Dashboard.of(\n" +
               "                topBar,\n" +
               "                menu,\n" +
               "                centerContent,\n" +
               "                footerContent\n" +
               "        );\n\n" +
               "        return Scaffold.of().body(body);\n" +
               "    }\n" +
               "}\n";
    }

    private static String generateDashboardPageClass(String pkg) {
        return "package " + pkg + ".dashboard;\n\n" +
               "import io.jettra.flux.widgets.Column;\n" +
               "import io.jettra.flux.widgets.Paragraph;\n" +
               "import " + pkg + ".template.TemplatePage;\n" +
               "import com.sun.net.httpserver.HttpExchange;\n" +
               "import io.jettra.flux.core.Widget;\n" +
               "import io.jettra.core.security.widget.PageWidgetAllow;\n" +
               "import java.util.Map;\n\n" +
               "@PageWidgetAllow(role = { jcf.AppRole.ADMIN, jcf.AppRole.MANAGER })\n" +
               "@io.jettra.core.server.Page(path = \"/dashboard\")\n" +
               "public class DashboardPage extends TemplatePage {\n\n" +
               "    @Override\n" +
               "    protected String getTitle() {\n" +
               "        return \"Dashboard - JettraFlux Pro\";\n" +
               "    }\n\n" +
               "    @Override\n" +
               "    protected Widget buildCenter(HttpExchange exchange, Map<String, String> params, String currentTheme) {\n" +
               "        \n" +
               "        Widget customCss = Paragraph.of(io.jettra.flux.theme.OceanTheme.DashboardPage.CustomCSS);\n\n" +
               "        Widget stat1 = io.jettra.flux.widgets.StatCard.of(\"Conversion Rate\", \"0.8%\", \"0.81%\", false);\n" +
               "        Widget stat2 = io.jettra.flux.widgets.StatCard.of(\"Avg. Order Value\", \"4.2%\", \"$306.2\", true);\n" +
               "        Widget stat3 = io.jettra.flux.widgets.StatCard.of(\"Order Quantity\", \"2.1%\", \"1,620\", false);\n\n" +
               "        Widget mainChart = io.jettra.flux.widgets.VisitorGraphCard.of(\n" +
               "            \"Unique Visitor Graph\", \"2025\",\n" +
               "            \"$620,076\", \"MRR GROWTH\",\n" +
               "            \"$1,120\", \"AVG. MRR/CUSTOMER\",\n" +
               "            40, 60, 50, 80, 30, 75, 65, 95, 55\n" +
               "        );\n\n" +
               "        Widget transactions = io.jettra.flux.widgets.TransactionHistoryCard.of(\n" +
               "            \"Transaction History\",\n" +
               "            new io.jettra.flux.widgets.TransactionHistoryCard.TransactionItem(io.jettra.flux.widgets.Icon.CHECK, \"#3b82f6\", \"Payment from #28492\", \"June 13, 2025 11:09 AM\", \"+$250.00\", true),\n" +
               "            new io.jettra.flux.widgets.TransactionHistoryCard.TransactionItem(io.jettra.flux.widgets.Icon.REDO, \"#ef4444\", \"Process refund to #94830\", \"June 13, 2025 08:22 AM\", \"-$570.00\", false),\n" +
               "            new io.jettra.flux.widgets.TransactionHistoryCard.TransactionItem(io.jettra.flux.widgets.Icon.PLUS, \"#22c55e\", \"New 8 user to #5849\", \"June 12, 2025 02:56 PM\", \"+$50.00\", true),\n" +
               "            new io.jettra.flux.widgets.TransactionHistoryCard.TransactionItem(io.jettra.flux.widgets.Icon.CHECK, \"#3b82f6\", \"Payment from #3382\", \"June 11, 2025 06:11 AM\", \"+$3830.00\", true)\n" +
               "        );\n\n" +
               "        Widget layout = Column.of(\n" +
               "            io.jettra.flux.widgets.Grid.of(stat1, stat2, stat3).modifier(new io.jettra.flux.core.Modifier().cssClass(\"oceantheme-dashboard-grid\")),\n" +
               "            io.jettra.flux.widgets.Grid.of(mainChart, transactions).modifier(new io.jettra.flux.core.Modifier().cssClass(\"oceantheme-main-grid\"))\n" +
               "        );\n\n" +
               "        return Column.of(\n" +
               "            customCss,\n" +
               "            layout\n" +
               "        );\n" +
               "    }\n" +
               "}\n";
    }

    private static String generatePersonEntityClass(String pkg) {
        return "package " + pkg + ".entity;\n\n" +
               "import io.jettra.rules.validations.Email;\n" +
               "import io.jettra.rules.validations.Min;\n" +
               "import io.jettra.rules.validations.NotNull;\n\n" +
               "public record Person(\n" +
               "        @NotNull String name,\n" +
               "        @Email String email,\n" +
               "        @Min(value = 0) Integer age) {\n" +
               "}\n";
    }

    private static String generatePersonModelClass(String pkg) {
        return "package " + pkg + ".model;\n\n" +
               "import io.jettra.flux.annotations.JettraViewModel;\n" +
               "import io.jettra.flux.annotations.PropertiesInRecord;\n" +
               "import io.jettra.flux.annotations.PropertiesLabel;\n" +
               "import io.jettra.rules.validations.Email;\n" +
               "import io.jettra.rules.validations.Min;\n" +
               "import io.jettra.rules.validations.NotNull;\n\n" +
               "@JettraViewModel\n" +
               "public class PersonModel {\n\n" +
               "    @PropertiesInRecord\n" +
               "    @PropertiesLabel(value = \"person.name\", label = \"Name\")\n" +
               "    @NotNull\n" +
               "    private String name;\n\n" +
               "    @PropertiesInRecord\n" +
               "    @PropertiesLabel(value = \"person.email\", label = \"Email\")\n" +
               "    @NotNull\n" +
               "    @Email\n" +
               "    private String email;\n\n" +
               "    @PropertiesInRecord\n" +
               "    @PropertiesLabel(value = \"person.age\", label = \"Age\")\n" +
               "    @Min(value = 0, message = \"La edad no puede ser negativa\")\n" +
               "    private Integer age;\n\n" +
               "    public PersonModel() {}\n\n" +
               "    public String getName() {\n" +
               "        return name;\n" +
               "    }\n\n" +
               "    public void setName(String name) {\n" +
               "        this.name = name;\n" +
               "    }\n\n" +
               "    public String getEmail() {\n" +
               "        return email;\n" +
               "    }\n\n" +
               "    public void setEmail(String email) {\n" +
               "        this.email = email;\n" +
               "    }\n\n" +
               "    public Integer getAge() {\n" +
               "        return age;\n" +
               "    }\n\n" +
               "    public void setAge(Integer age) {\n" +
               "        this.age = age;\n" +
               "    }\n" +
               "}\n";
    }

    private static String generatePersonPageClass(String pkg) {
        return "package " + pkg + ".page;\n\n" +
               "import " + pkg + ".model.PersonModel;\n" +
               "import " + pkg + ".template.TemplatePage;\n" +
               "import io.jettra.flux.widgets.Column;\n" +
               "import io.jettra.flux.widgets.Card;\n" +
               "import io.jettra.flux.widgets.TextField;\n" +
               "import io.jettra.flux.widgets.Div;\n" +
               "import io.jettra.flux.widgets.Form;\n" +
               "import io.jettra.flux.widgets.Paragraph;\n" +
               "import io.jettra.flux.widgets.Header;\n" +
               "import io.jettra.flux.widgets.Label;\n\n" +
               "import com.sun.net.httpserver.HttpExchange;\n" +
               "import io.jettra.core.inject.annotation.InjectProperties;\n" +
               "import io.jettra.flux.core.Widget;\n" +
               "import io.jettra.server.JettraServer;\n" +
               "import io.jettra.core.security.widget.PageWidgetAllow;\n" +
               "import io.jettra.core.security.widget.ActionWidgetAllow;\n" +
               "import io.jettra.flux.annotations.binding.FluxBinding;\n" +
               "import io.jettra.flux.binding.FluxBinder;\n" +
               "import io.jettra.flux.sync.JettraPageSincronized;\n" +
               "import io.jettra.flux.sync.JettraSyncManager;\n" +
               "import io.jettra.flux.sync.SyncType;\n" +
               "import io.jettra.flux.widgets.ElevatedButton;\n" +
               "import io.jettra.flux.widgets.Grid;\n" +
               "import io.jettra.flux.widgets.Row;\n" +
               "import io.jettra.rules.core.RuleResult;\n" +
               "import java.util.ArrayList;\n" +
               "import java.util.List;\n" +
               "import java.util.Map;\n" +
               "import java.util.Properties;\n" +
               "import io.jettra.core.server.Page;\n" +
               "import io.jettra.flux.widgets.Alert;\n" +
               "import io.jettra.flux.widgets.Icon;\n" +
               "import io.jettra.flux.widgets.Text;\n" +
               "import io.jettra.server.core.JettraContext;\n\n" +
               "@JettraPageSincronized(SyncType.ALL)\n" +
               "@PageWidgetAllow(role = { jcf.AppRole.ADMIN, jcf.AppRole.MANAGER })\n" +
               "@Page(path = \"/person\")\n" +
               "public class PersonPage extends TemplatePage {\n\n" +
               "    @InjectProperties(name = \"messages\")\n" +
               "    private Properties msg;\n\n" +
               "    @FluxBinding(model = PersonModel.class)\n" +
               "    PersonModel personModel = new PersonModel();\n" +
               "    List<PersonModel> persons = new ArrayList<>();\n\n" +
               "    @Override\n" +
               "    public String getTitle() {\n" +
               "        return msg != null ? msg.getProperty(\"personpage.title\", \"Persona\") : \"Persona\";\n" +
               "    }\n\n" +
               "    @ActionWidgetAllow(role = { jcf.AppRole.ADMIN, jcf.AppRole.MANAGER })\n" +
               "    private void saveForm(HttpExchange exchange, Map<String, String> params) {\n" +
               "        System.out.println(\"Formulario recibido con datos: \" + params);\n\n" +
               "        List<RuleResult> results = new FluxBinder(personModel)\n" +
               "                .messages(msg)\n" +
               "                .bind(params)\n" +
               "                .compute()\n" +
               "                .validate();\n\n" +
               "        boolean hasErrors = false;\n" +
               "        StringBuilder errorMsg = new StringBuilder();\n" +
               "        for (RuleResult result : results) {\n" +
               "            if (!result.isValid()) {\n" +
               "                hasErrors = true;\n" +
               "                if (errorMsg.length() > 0) {\n" +
               "                    errorMsg.append(\" | \");\n" +
               "                }\n" +
               "                errorMsg.append(result.getMessage());\n" +
               "            }\n" +
               "        }\n\n" +
               "        if (hasErrors) {\n" +
               "            try {\n" +
               "                redirect(exchange, \"/person?error=\" + java.net.URLEncoder.encode(errorMsg.toString(), \"UTF-8\"));\n" +
               "            } catch (Exception e) {\n" +
               "            }\n" +
               "        } else {\n" +
               "            JettraSyncManager.notifyChange(\"PersonModel\", SyncType.UPDATE, getLoggedUser(exchange));\n" +
               "            io.jettra.flux.widgets.NotificationTop.broadcast(\"main_notification\", io.jettra.flux.widgets.NotificationTop.NotificationTopType.GLOBAL, \"Nueva persona agregada: \" + personModel.getName());\n\n" +
               "            try {\n" +
               "                redirect(exchange, \"/person?success=true\");\n" +
               "            } catch (Exception e) {\n" +
               "            }\n" +
               "        }\n" +
               "    }\n\n" +
               "    @Override\n" +
               "    protected Widget buildCenter(HttpExchange exchange, Map<String, String> params, String currentTheme) {\n" +
               "        PersonModel personModel = (PersonModel) JettraContext.getCurrent().get(JettraContext.Scope.SESSION, \"personForm\");\n" +
               "        if (personModel == null) {\n" +
               "            personModel = new PersonModel();\n" +
               "            JettraContext.getCurrent().set(JettraContext.Scope.SESSION, \"personForm\", personModel);\n" +
               "        }\n\n" +
               "        String successMsgStr = (msg != null && msg.getProperty(\"app.label.success\") != null) ? msg.getProperty(\"app.label.success\") : \"Registro guardado exitosamente.\";\n" +
               "        String defaultErrorMsgStr = (msg != null && msg.getProperty(\"app.label.error\") != null) ? msg.getProperty(\"app.label.error\") : \"Error al guardar el registro.\";\n" +
               "        String enterYour = (msg != null && msg.getProperty(\"app.label.enteryour\") != null) ? msg.getProperty(\"app.label.enteryour\") : \"Ingrese su \";\n" +
               "        String nameLabel = (msg != null && msg.getProperty(\"person.name\") != null) ? msg.getProperty(\"person.name\") : \"Nombre\";\n" +
               "        String emailLabel = (msg != null && msg.getProperty(\"person.email\") != null) ? msg.getProperty(\"person.email\") : \"Correo\";\n" +
               "        String ageLabel = (msg != null && msg.getProperty(\"person.age\") != null) ? msg.getProperty(\"person.age\") : \"Edad\";\n" +
               "        String subtitleStr = (msg != null && msg.getProperty(\"personpage.subtitle\") != null) ? msg.getProperty(\"personpage.subtitle\") : \"Registros de Persona\";\n\n" +
               "        Widget alert = null;\n" +
               "        if (\"true\".equals(params.get(\"success\"))) {\n" +
               "            alert = Alert.of(Text.of(successMsgStr)).severity(\"success\")\n" +
               "                    .modifier(new io.jettra.flux.core.Modifier().style(\"margin-bottom: 20px; width: 100%;\"));\n" +
               "        } else if (params.containsKey(\"error\")) {\n" +
               "            String rawErr = params.get(\"error\");\n" +
               "            if (rawErr != null) {\n" +
               "                rawErr = rawErr.replace(\"+\", \" \");\n" +
               "            }\n" +
               "            String errorMsgStr = \"true\".equals(rawErr) ? defaultErrorMsgStr : rawErr;\n" +
               "            alert = Alert.of(\n" +
               "                Column.of(\n" +
               "                    Header.of(4, \"Error de Validación de Reglas (JettraRules)\")\n" +
               "                            .modifier(new io.jettra.flux.core.Modifier().style(\"margin-top: 0; margin-bottom: 5px; font-weight: 600; font-size: 15px;\")),\n" +
               "                    Paragraph.of(errorMsgStr)\n" +
               "                            .modifier(new io.jettra.flux.core.Modifier().style(\"font-weight: 500; font-size: 14px; margin: 0;\"))\n" +
               "                ).modifier(new io.jettra.flux.core.Modifier().style(\"align-items: flex-start; gap: 4px;\"))\n" +
               "            ).severity(\"danger\");\n" +
               "        }\n\n" +
               "        Widget verticalForm = Card.of(Column.of(\n" +
               "                Header.of(4, subtitleStr)\n" +
               "                        .modifier(new io.jettra.flux.core.Modifier().style(\"margin-top: 0; margin-bottom: 15px; font-weight: 600;\")),\n" +
               "                Label.of(nameLabel).forId(\"name\")\n" +
               "                        .modifier(new io.jettra.flux.core.Modifier().style(\"margin-bottom: 5px; font-weight: 500; display: block;\")),\n" +
               "                TextField.of(nameLabel, enterYour + nameLabel).id(\"name\")\n" +
               "                        .binding(PersonModel.class, \"name\")\n" +
               "                        .value(personModel.getName())\n" +
               "                        .modifier(new io.jettra.flux.core.Modifier().style(\n" +
               "                                \"margin-bottom: 15px; width: 100%; border: 1px solid #d1d5db; border-radius: 6px; padding: 8px 12px;\")),\n" +
               "                Label.of(emailLabel).forId(\"email\")\n" +
               "                        .modifier(new io.jettra.flux.core.Modifier().style(\"margin-bottom: 5px; font-weight: 500; display: block;\")),\n" +
               "                TextField.of(emailLabel, enterYour + emailLabel).id(\"email\")\n" +
               "                        .binding(PersonModel.class, \"email\")\n" +
               "                        .value(personModel.getEmail())\n" +
               "                        .modifier(new io.jettra.flux.core.Modifier().style(\n" +
               "                                \"margin-bottom: 15px; width: 100%; border: 1px solid #d1d5db; border-radius: 6px; padding: 8px 12px;\")),\n" +
               "                Label.of(ageLabel).forId(\"age\")\n" +
               "                        .modifier(new io.jettra.flux.core.Modifier().style(\"margin-bottom: 5px; font-weight: 500; display: block;\")),\n" +
               "                TextField.of(ageLabel, enterYour + ageLabel).id(\"age\")\n" +
               "                        .binding(PersonModel.class, \"age\")\n" +
               "                        .value(personModel.getAge())\n" +
               "                        .modifier(new io.jettra.flux.core.Modifier().style(\n" +
               "                                \"margin-bottom: 15px; width: 100%; border: 1px solid #d1d5db; border-radius: 6px; padding: 8px 12px;\")))\n" +
               "                .modifier(new io.jettra.flux.core.Modifier().style(\"width: 100%; align-items: stretch; gap: 5px;\")));\n\n" +
               "        Widget mainForm = Form.of(\n" +
               "                Column.of(\n" +
               "                        Grid.of(verticalForm).modifier(new io.jettra.flux.core.Modifier().style(\n" +
               "                                \"grid-template-columns: 1fr 1fr; gap: 20px; align-items: flex-start; margin-bottom: 20px;\")),\n" +
               "                        Row.of(\n" +
               "                                ElevatedButton.of(msg != null ? msg.getProperty(\"btn.save\", \"Save\") : \"Save\")\n" +
               "                                        .modifier(new io.jettra.flux.core.Modifier().style(\n" +
               "                                                \"align-self: flex-start; padding: 10px 20px; background-color: #6366F1; color: white; border: none; border-radius: 6px; cursor: pointer; font-weight: 600;\"))\n" +
               "                        )\n" +
               "                        .modifier(new io.jettra.flux.core.Modifier()\n" +
               "                                .style(\"display: flex; flex-direction: row; align-items: center;\")))\n" +
               "                .modifier(new io.jettra.flux.core.Modifier().style(\"width: 100%;\")))\n" +
               "                .action(JettraServer.resolvePath(\"/person?_action_method=saveForm\")).method(\"POST\").attribute(\"id\", \"personForm\");\n\n" +
               "        Widget webRulesScriptWidget = FluxBinder.generateWebRulesWidget(PersonModel.class, \"personForm\");\n\n" +
               "        return Column.of(\n" +
               "                Header.of(2, msg != null ? msg.getProperty(\"personpage.title\", \"Persona\") : \"Persona\")\n" +
               "                        .modifier(new io.jettra.flux.core.Modifier().style(\"margin-top: 0; font-weight: 600; margin-bottom: 20px;\")),\n" +
               "                (alert != null ? alert : Div.of()),\n" +
               "                mainForm,\n" +
               "                webRulesScriptWidget)\n" +
               "                .modifier(new io.jettra.flux.core.Modifier()\n" +
               "                        .style(\"width: 100%; align-items: flex-start; max-width: 1200px; padding: 20px;\"));\n" +
               "    }\n" +
               "}\n";
    }

    private static void generateThemeProject(String projectName, String pathDir, String urlSource) {
        if (pathDir != null && pathDir.startsWith("~")) {
            pathDir = pathDir.replaceFirst("^~", System.getProperty("user.home"));
        }
        System.out.println("Generating Theme Project: " + projectName + " at " + pathDir);
        if (urlSource != null) {
            System.out.println("Using inspiration from: " + urlSource);
        }
        
        Path projectPath = Paths.get(pathDir, projectName);
        try {
            Files.createDirectories(projectPath);
            Files.createDirectories(projectPath.resolve("src/main/resources/META-INF"));
            
            // Generate pom.xml
            String pomContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>com.jettra.theme</groupId>\n" +
                "    <artifactId>" + projectName.toLowerCase() + "</artifactId>\n" +
                "    <version>1.0.0</version>\n" +
                "    <name>" + projectName + "</name>\n" +
                "    <packaging>jar</packaging>\n" +
                "    <properties>\n" +
                "        <maven.compiler.source>25</maven.compiler.source>\n" +
                "        <maven.compiler.target>25</maven.compiler.target>\n" +
                "        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n" +
                "    </properties>\n" +
                "</project>\n";
            Files.write(projectPath.resolve("pom.xml"), pomContent.getBytes(StandardCharsets.UTF_8));
            
            // Generate theme.json with scraped properties or default properties
            String themeContent;
            if (urlSource != null && !urlSource.isEmpty()) {
                themeContent = ThemeScraper.scrapeThemeJson(urlSource, projectName);
            } else {
                themeContent = "{\n" +
                    "  \"name\": \"" + projectName + "\",\n" +
                    "  \"primary\": \"#d32f2f\",\n" +
                    "  \"secondary\": \"#f44336\",\n" +
                    "  \"background\": \"#ffebee\",\n" +
                    "  \"surface\": \"#ffffff\",\n" +
                    "  \"onPrimary\": \"#ffffff\",\n" +
                    "  \"onSurface\": \"#212121\",\n" +
                    "  \"buttonStyle\": \"border: none; border-radius: 4px; padding: 10px 20px; font-weight: 500; cursor: pointer; transition: background 0.3s; background-color: #d32f2f; color: #ffffff;\",\n" +
                    "  \"cardStyle\": \"border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); padding: 16px; background-color: #ffffff;\",\n" +
                    "  \"containerStyle\": \"padding: 16px; border-radius: 4px;\",\n" +
                    "  \"textStyle\": \"font-size: 16px; color: #212121;\",\n" +
                    "  \"customCss\": \".top-btn-today { background-color: #d32f2f; color: white; border: none; padding: 8px 16px; border-radius: 6px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 8px; }\\n.sidebar-logo { font-size: 1.5rem; font-weight: 700; color: #d32f2f; padding: 10px 15px; margin-bottom: 20px; display: flex; align-items: center; gap: 10px; }\",\n" +
                    "  \"customJs\": \"console.log('Loaded theme " + projectName + "');\"\n" +
                    "}";
            }
            Files.write(projectPath.resolve("src/main/resources/META-INF/theme.json"), themeContent.getBytes(StandardCharsets.UTF_8));
            
            // Generate Java Class
            String pkgName = "com.jettra.theme." + projectName.toLowerCase();
            Path javaDir = projectPath.resolve("src/main/java/" + pkgName.replace(".", "/"));
            Files.createDirectories(javaDir);
            
            String javaClassContent = "package " + pkgName + ";\n\n" +
                "public class " + projectName + "Theme {\n" +
                "    public static class Template {\n" +
                "        public static final String CustomCSS = \"<style>\\n\"\n" +
                "            + \"/* " + projectName + "Theme Custom CSS */\\n\"\n" +
                "            + \"</style>\\n\";\n\n" +
                "        public static final String CustomJS = \"<script>\\n\"\n" +
                "            + \"/* Scripts are loaded from theme.json */\\n\"\n" +
                "            + \"</script>\";\n" +
                "    }\n" +
                "}\n";
            Files.write(javaDir.resolve(projectName + "Theme.java"), javaClassContent.getBytes(StandardCharsets.UTF_8));
            
            System.out.println("✅ Theme project '" + projectName + "' generated successfully!");
            System.out.println("Next steps: ");
            System.out.println("  1. cd " + projectName);
            System.out.println("  2. mvn clean install");
        } catch (Exception e) {
            System.err.println("❌ Error generating theme project: " + e.getMessage());
        }
    }
}
