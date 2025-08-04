package com.amannmalik.mcp;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.*;
import java.util.stream.Collectors;

public class McpFeatureSteps {


    @Before
    public void setupTestEnvironment() {
    }

    @After
    public void cleanupTestEnvironment() {
    }

    /** Parse capabilities table into structured format. */
    private List<Capability> parseCapabilitiesTable(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new Capability(
                        Objects.requireNonNull(row.get("capability")),
                        row.getOrDefault("feature", ""),
                        Boolean.parseBoolean(row.getOrDefault("enabled", "false"))))
                .toList();
    }

    /** Parse resource templates table into template definitions. */
    private List<ResourceTemplateRow> parseResourceTemplates(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new ResourceTemplateRow(
                        Objects.requireNonNull(row.get("template")),
                        Objects.requireNonNull(row.get("description")),
                        Objects.requireNonNull(row.get("mime_type"))))
                .toList();
    }

    /** Parse tool definitions table. */
    private List<ToolDefinition> parseToolDefinitions(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new ToolDefinition(
                        Objects.requireNonNull(row.get("name")),
                        Objects.requireNonNull(row.get("description")),
                        Boolean.parseBoolean(row.getOrDefault("requires_confirmation", "false"))))
                .toList();
    }

    /** Parse input schema table for tool parameters. */
    private List<InputField> parseInputSchema(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new InputField(
                        Objects.requireNonNull(row.get("field")),
                        Objects.requireNonNull(row.get("type")),
                        Boolean.parseBoolean(row.getOrDefault("required", "false")),
                        Objects.requireNonNull(row.get("description"))))
                .toList();
    }

    /** Parse arguments table for tool calls. */
    private Map<String, String> parseArguments(DataTable table) {
        return table.asMaps().stream()
                .collect(Collectors.toMap(
                        row -> Objects.requireNonNull(row.get("field")),
                        row -> Objects.requireNonNull(row.get("value"))));
    }

    /** Parse model preferences table. */
    private List<ModelPreference> parseModelPreferences(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new ModelPreference(
                        Objects.requireNonNull(row.get("preference")),
                        Objects.requireNonNull(row.get("value")),
                        Objects.requireNonNull(row.get("description"))))
                .toList();
    }

    /** Parse log messages table for testing. */
    private List<LogMessage> parseLogMessages(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new LogMessage(
                        Objects.requireNonNull(row.get("level")),
                        Objects.requireNonNull(row.get("logger")),
                        Objects.requireNonNull(row.get("message"))))
                .toList();
    }

    private record Capability(String capability, String feature, boolean enabled) {}

    private record ResourceTemplateRow(String template, String description, String mimeType) {}

    private record ToolDefinition(String name, String description, boolean requiresConfirmation) {}

    private record InputField(String field, String type, boolean required, String description) {}

    private record ModelPreference(String preference, String value, String description) {}

    private record LogMessage(String level, String logger, String message) {}

}