package se.bjurr.gradle.updateversions;

import com.github.benmanes.gradle.versions.reporter.result.Dependency;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

/**
 * Builds {@link RewritePlan}s for outdated/downgradable/missing dependencies and applies or reports
 * them against the project's build files.
 */
final class DependencyRewriter {

  private DependencyRewriter() {}

  record RewritePlan(
      String kind,
      String prettyPrint,
      List<String> fromPatterns,
      String toGroovy,
      String toKotlin) {}

  static List<File> getGradleFiles(Project project) {
    List<File> files = new ArrayList<>();
    File srcFile = project.getBuildscript().getSourceFile();
    if (srcFile != null && srcFile.exists()) {
      files.add(srcFile);
    }
    return files;
  }

  static RewritePlan buildRewritePlan(Dependency dep, String newVersion, String kind) {
    if (UpdateVersionsPlugin.isPluginMarker(dep)) {
      String pluginId = dep.getGroup();
      return new RewritePlan(
          kind,
          "plugin " + pluginId + ":" + dep.getVersion() + " -> " + newVersion,
          List.of(
              // Groovy DSL
              "id\\s+['\"]"
                  + pluginId
                  + "['\"]\\s+version\\s+['\"]"
                  + dep.getVersion()
                  + "['\"](\\s+apply\\s+false)?",
              // Kotlin DSL
              "id\\(\\s*['\"]"
                  + pluginId
                  + "['\"]\\s*\\)\\s+version\\s+['\"]"
                  + dep.getVersion()
                  + "['\"](\\s+apply\\s+false)?"),
          "id \"" + pluginId + "\" version \"" + newVersion + "\"",
          "id(\"" + pluginId + "\") version \"" + newVersion + "\"");
    }
    return new RewritePlan(
        kind,
        UpdateVersionsPlugin.coordinateWithVersion(dep) + " -> " + newVersion,
        List.of(
            // Flexible pattern for 'group:name:version' in single or double quotes
            "['\"]\\s*" + UpdateVersionsPlugin.coordinateWithVersion(dep) + "\\s*['\"]",
            // Flexible pattern for map notation: group: '...', name: '...', version: '...'
            "group:\\s*['\"]"
                + dep.getGroup()
                + "['\"],\\s*name:\\s*['\"]"
                + dep.getName()
                + "['\"],\\s*version:\\s*['\"]"
                + dep.getVersion()
                + "['\"]"),
        "\"" + dep.getGroup() + ":" + dep.getName() + ":" + newVersion + "\"",
        "\"" + dep.getGroup() + ":" + dep.getName() + ":" + newVersion + "\"");
  }

  static RewritePlan buildMissingVersionRewritePlan(Dependency dep, String newVersion) {
    return new RewritePlan(
        "missing",
        dep.getGroup() + ":" + dep.getName() + " -> " + newVersion,
        List.of(
            // String notation with no version at all: 'group:name'
            "['\"]\\s*" + dep.getGroup() + ":" + dep.getName() + "\\s*['\"]",
            // String notation with an empty trailing version: 'group:name:'
            "['\"]\\s*" + dep.getGroup() + ":" + dep.getName() + ":\\s*['\"]",
            // Map notation without a version key at all (group/name must be the fixed order
            // used elsewhere in this plugin, and must not be followed by a version key)
            "group:\\s*['\"]"
                + dep.getGroup()
                + "['\"],\\s*name:\\s*['\"]"
                + dep.getName()
                + "['\"](?!\\s*,\\s*version)"),
        "\"" + dep.getGroup() + ":" + dep.getName() + ":" + newVersion + "\"",
        "\"" + dep.getGroup() + ":" + dep.getName() + ":" + newVersion + "\"");
  }

  static List<RewritePlan> filterToRewritablePlans(Project project, List<RewritePlan> plans) {
    List<File> gradleFiles = getGradleFiles(project);
    if (gradleFiles.isEmpty()) {
      return List.of();
    }

    List<String> fileContents = gradleFiles.stream().map(DependencyRewriter::readText).toList();

    return plans.stream()
        .filter(
            plan ->
                fileContents.stream()
                    .anyMatch(
                        content ->
                            plan.fromPatterns().stream()
                                .anyMatch(
                                    pattern ->
                                        Pattern.compile(pattern, Pattern.DOTALL)
                                            .matcher(content)
                                            .find())))
        .toList();
  }

  static void applyChanges(Project project, List<RewritePlan> changes, Logger logger) {
    List<String> appliedSummaries = new ArrayList<>();

    for (File file : getGradleFiles(project)) {
      String content = readText(file);
      boolean isKotlin = file.getName().endsWith(".kts");
      List<String> fileSummaries = new ArrayList<>();

      for (RewritePlan plan : changes) {
        logger.info("Checking " + file.getName() + " for updates to " + plan.prettyPrint());
        for (String patternStr : plan.fromPatterns()) {
          logger.info("  Using pattern: " + patternStr);
          Pattern pattern = Pattern.compile(patternStr, Pattern.DOTALL);
          Matcher matcher = pattern.matcher(content);
          boolean isPluginPattern = patternStr.contains("id\\s+") || patternStr.contains("id\\(");

          if (isPluginPattern) {
            // Plugin patterns need special handling for "apply false"
            int matchCount = 0;
            int maxMatches = 100; // Safety limit to prevent infinite loops
            while (matcher.find() && matchCount < maxMatches) {
              logger.info("    Found match for pattern: " + patternStr);
              String applyFalse =
                  matcher.groupCount() >= 1 && matcher.group(1) != null ? matcher.group(1) : "";
              String replacementBase = isKotlin ? plan.toKotlin() : plan.toGroovy();
              String newContent = matcher.replaceFirst(matchResult -> replacementBase + applyFalse);

              if (newContent.equals(content)) {
                logger.info(
                    "    Replacement did not change content, breaking loop to prevent infinite loop");
                break;
              }
              content = newContent;
              fileSummaries.add("  " + emojiFor(plan.kind()) + " " + plan.prettyPrint());

              // Create new matcher with updated content
              matcher = pattern.matcher(content);
              matchCount++;
            }

            if (matchCount >= maxMatches) {
              logger.warn(
                  "    Reached maximum match limit ("
                      + maxMatches
                      + "), stopping to prevent infinite loop");
            }
          } else if (matcher.find()) {
            // For dependencies, we can use replaceAll since there are no capture groups
            logger.info("    Found match for pattern: " + patternStr);
            String replacement = isKotlin ? plan.toKotlin() : plan.toGroovy();
            content = pattern.matcher(content).replaceAll(replacement);
            fileSummaries.add("  " + emojiFor(plan.kind()) + " " + plan.prettyPrint());
          }
        }
      }

      if (!fileSummaries.isEmpty()) {
        writeText(file, content);
        appliedSummaries.addAll(fileSummaries);
      }
    }

    if (!appliedSummaries.isEmpty()) {
      String joined = new TreeSet<>(appliedSummaries).stream().collect(Collectors.joining("\n"));
      logger.lifecycle(
          """
					📝 Updated dependencies:

					"""
              + joined
              + "\n");
    }
  }

  static void reportChanges(List<RewritePlan> changes, Logger logger) {
    logger.lifecycle(
        """
				📝 There are dependencies that can be updated:
				""");

    String joined =
        changes.stream()
            .map(plan -> "  " + emojiFor(plan.kind()) + " " + plan.prettyPrint())
            .collect(Collectors.toCollection(TreeSet::new))
            .stream()
            .collect(Collectors.joining("\n"));
    logger.lifecycle(joined);

    logger.lifecycle(
        """

				🔄 Update dependencies with:

				  ./gradlew updateDependencies

				""");
  }

  private static String emojiFor(String kind) {
    if ("downgrade".equals(kind)) {
      return "⬇️";
    }
    if ("missing".equals(kind)) {
      return "➕";
    }
    return "📦";
  }

  private static String readText(File file) {
    try {
      return Files.readString(file.toPath());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void writeText(File file, String content) {
    try {
      Files.writeString(file.toPath(), content);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
