package com.amannmalik.mcp.test;

import java.io.File;
import java.nio.file.Path;

final class CommandSpecs {
    private CommandSpecs() {
    }

    static String stdioServer() {
        var java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        var mainJar = Path.of("build", "libs", "mcp-0.1.0.jar").toAbsolutePath().toString();
        var testClasses = Path.of("build", "classes", "java", "test").toAbsolutePath().toString();
        var testResources = Path.of("build", "resources", "test").toAbsolutePath().toString();
        var classPath = String.join(File.pathSeparator, mainJar, testClasses, testResources);
        return String.join(" ",
                java,
                "-cp", classPath,
                "com.amannmalik.mcp.cli.Entrypoint",
                "server",
                "--stdio",
                "--test-mode");
    }
}
