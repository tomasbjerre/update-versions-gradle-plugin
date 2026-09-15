package se.bjurr.gradle.updateversions;

import com.github.benmanes.gradle.versions.reporter.result.Dependency;
import com.github.benmanes.gradle.versions.reporter.result.DependencyLatest;
import com.github.benmanes.gradle.versions.reporter.result.DependencyOutdated;
import com.github.benmanes.gradle.versions.reporter.result.DependencyUnresolved;
import com.github.benmanes.gradle.versions.reporter.result.Result;
import com.github.benmanes.gradle.versions.reporter.result.VersionAvailable;
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ComponentSelection;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.logging.Logger;

public class UpdateVersionsPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply("com.github.ben-manes.versions");

    UpdateVersionsPluginExtension extension =
        project.getExtensions().create("updateVersions", UpdateVersionsPluginExtension.class);
    extension.getIgnoreDependenciesRegexp().convention("");
    extension.getDowngradeUnresolvedDependencies().convention(true);
    extension.getSetMissingVersions().convention(true);

    project
        .getTasks()
        .register(
            "showUpdateableDependencies",
            DependencyUpdatesTask.class,
            task -> configureDependencyUpdatesTask(task, project, extension, false));

    project
        .getTasks()
        .register(
            "updateDependencies",
            DependencyUpdatesTask.class,
            task -> configureDependencyUpdatesTask(task, project, extension, true));

    if (runningInCI()) {
      project
          .getLogger()
          .lifecycle(
              "Running in CI, saving time by not showing updatable dependencies after build.");
    } else {
      project
          .getTasks()
          .named("build")
          .configure(task -> task.finalizedBy("showUpdateableDependencies"));
    }
  }

  private static void configureDependencyUpdatesTask(
      DependencyUpdatesTask task,
      Project project,
      UpdateVersionsPluginExtension extension,
      boolean writeChanges) {
    task.setCheckForGradleUpdate(false);
    task.setGradleReleaseChannel("current");
    task.rejectVersionIf(UpdateVersionsPlugin::isNonStable);
    task.outputFormatter(
        result ->
            handleDependencies(
                project,
                result,
                extension.getIgnoreDependenciesRegexp().get(),
                extension.getDowngradeUnresolvedDependencies().get(),
                extension.getSetMissingVersions().get(),
                writeChanges));
  }

  private record RewritePlan(
      String kind,
      String prettyPrint,
      List<String> fromPatterns,
      String toGroovy,
      String toKotlin) {}

  private record Candidate<D extends Dependency>(D dependency, String newVersion) {}

  private static List<File> getGradleFiles(Project project) {
    List<File> files = new ArrayList<>();
    File srcFile = project.getBuildscript().getSourceFile();
    if (srcFile != null && srcFile.exists()) {
      files.add(srcFile);
    }
    return files;
  }

  private static boolean isNonEmpty(String value) {
    return value != null && !value.isEmpty();
  }

  private static boolean isPluginMarker(Dependency dep) {
    return isNonEmpty(dep.getGroup()) && (dep.getGroup() + ".gradle.plugin").equals(dep.getName());
  }

  private static String coordinateWithVersion(Dependency dep) {
    return dep.getGroup() + ":" + dep.getName() + ":" + dep.getVersion();
  }

  private static String coordinateWithoutVersion(Dependency dep) {
    return dep.getGroup() + ":" + dep.getName() + ":";
  }

  private static boolean isNonStable(ComponentSelection selection) {
    String version = selection.getCandidate().getVersion();
    boolean stableKeyword =
        Stream.of("RELEASE", "FINAL", "GA")
            .anyMatch(keyword -> version.toUpperCase(Locale.ROOT).contains(keyword));
    boolean stableVersion = version.matches("^[0-9,.v-]+(-r)?$");
    return !stableKeyword && !stableVersion;
  }

  private static boolean canResolveDependency(
      Project project, String group, String name, String version) {
    try {
      Configuration conf =
          project
              .getConfigurations()
              .detachedConfiguration(
                  project.getDependencies().create(group + ":" + name + ":" + version));

      // Don't pollute dependency caches
      conf.setTransitive(false);

      conf.resolve();
      return true;
    } catch (Exception e) {
      project
          .getLogger()
          .info("Cannot resolve " + group + ":" + name + ":" + version + ": " + e.getMessage());
      return false;
    }
  }

  private static String getResolvableVersion(Project project, DependencyOutdated dep) {
    VersionAvailable available = dep.getAvailable();
    return Stream.of(available.getRelease(), available.getMilestone(), available.getIntegration())
        .filter(version -> version != null)
        .filter(version -> canResolveDependency(project, dep.getGroup(), dep.getName(), version))
        .findFirst()
        .orElse(null);
  }

  private static String resolveVersionMatching(
      Project project, String group, String name, String versionConstraint) {
    try {
      Configuration conf =
          project
              .getConfigurations()
              .detachedConfiguration(
                  project.getDependencies().create(group + ":" + name + ":" + versionConstraint));

      // Don't pollute dependency caches
      conf.setTransitive(false);

      conf.getResolutionStrategy()
          .componentSelection(
              rules ->
                  rules.all(
                      selection -> {
                        if (isNonStable(selection)) {
                          selection.reject("non-stable");
                        }
                      }));

      Set<ResolvedDependency> resolved =
          conf.getResolvedConfiguration().getFirstLevelModuleDependencies();
      if (resolved.isEmpty()) {
        return null;
      }

      // Resolving the dependency graph above only confirms metadata/POM exists - it doesn't
      // force the jar artifact itself to be fetched, so double check with canResolveDependency
      // the same way getResolvableVersion does for the "outdated" bucket.
      String candidate = resolved.iterator().next().getModuleVersion();
      return canResolveDependency(project, group, name, candidate) ? candidate : null;
    } catch (Exception e) {
      project
          .getLogger()
          .info(
              "Cannot resolve "
                  + group
                  + ":"
                  + name
                  + ":"
                  + versionConstraint
                  + ": "
                  + e.getMessage());
      return null;
    }
  }

  private static String resolveHighestVersionBelow(
      Project project, String group, String name, String version) {
    return resolveVersionMatching(project, group, name, "(," + version + ")");
  }

  private static String resolveLatestVersion(Project project, String group, String name) {
    return resolveVersionMatching(project, group, name, "+");
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

  private static RewritePlan buildRewritePlan(Dependency dep, String newVersion, String kind) {
    if (isPluginMarker(dep)) {
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
        coordinateWithVersion(dep) + " -> " + newVersion,
        List.of(
            // Flexible pattern for 'group:name:version' in single or double quotes
            "['\"]\\s*" + coordinateWithVersion(dep) + "\\s*['\"]",
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

  private static RewritePlan buildMissingVersionRewritePlan(Dependency dep, String newVersion) {
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

  private static List<RewritePlan> filterToRewritablePlans(
      Project project, List<RewritePlan> plans) {
    List<File> gradleFiles = getGradleFiles(project);
    if (gradleFiles.isEmpty()) {
      return List.of();
    }

    List<String> fileContents = gradleFiles.stream().map(UpdateVersionsPlugin::readText).toList();

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

  private static List<RewritePlan> getUpdatableDependencies(
      Project project, Set<DependencyOutdated> dependencies, String ignoreDependenciesRegexp) {
    List<RewritePlan> plans =
        dependencies.stream()
            .map(dep -> new Candidate<DependencyOutdated>(dep, getResolvableVersion(project, dep)))
            .filter(candidate -> candidate.newVersion() != null)
            .filter(
                candidate ->
                    !coordinateWithVersion(candidate.dependency())
                        .matches(ignoreDependenciesRegexp))
            .map(
                candidate ->
                    buildRewritePlan(candidate.dependency(), candidate.newVersion(), "update"))
            .toList();

    return filterToRewritablePlans(project, plans);
  }

  private static List<RewritePlan> getDowngradableDependencies(
      Project project,
      Set<DependencyLatest> exceededDependencies,
      Set<DependencyUnresolved> unresolvedDependencies,
      String ignoreDependenciesRegexp) {
    // "exceeded"/"unresolved" are gradle-versions-plugin's OWN classification, based on its
    // separate dynamic ("+") resolution of "what's out there" - which goes through Gradle's
    // dynamic-version cache (24h by default) and can be stale, reporting a "latest" that is
    // actually older than a since-published declared version. Never trust the bucket alone:
    // only act if the DECLARED version itself is confirmed broken by a fresh resolution check
    // right now, otherwise a merely-stale cache would cause a real, currently-working version
    // to be "fixed" into an older one for no reason.
    Stream<Candidate<Dependency>> fromExceeded =
        exceededDependencies.stream()
            .filter(
                dep ->
                    isNonEmpty(dep.getGroup())
                        && isNonEmpty(dep.getName())
                        && isNonEmpty(dep.getVersion())
                        && isNonEmpty(dep.getLatest()))
            .filter(
                dep ->
                    !canResolveDependency(project, dep.getGroup(), dep.getName(), dep.getVersion()))
            .map(
                dep ->
                    new Candidate<Dependency>(
                        dep,
                        canResolveDependency(
                                project, dep.getGroup(), dep.getName(), dep.getLatest())
                            ? dep.getLatest()
                            : resolveHighestVersionBelow(
                                project, dep.getGroup(), dep.getName(), dep.getVersion())));

    // "unresolved" is reserved for coordinates gradle-versions-plugin couldn't find ANY version
    // for at all (no candidate-version data whatsoever) - fall back to our own range resolution.
    Stream<Candidate<Dependency>> fromUnresolved =
        unresolvedDependencies.stream()
            .filter(
                dep ->
                    isNonEmpty(dep.getGroup())
                        && isNonEmpty(dep.getName())
                        && isNonEmpty(dep.getVersion()))
            .filter(
                dep ->
                    !canResolveDependency(project, dep.getGroup(), dep.getName(), dep.getVersion()))
            .map(
                dep ->
                    new Candidate<Dependency>(
                        dep,
                        resolveHighestVersionBelow(
                            project, dep.getGroup(), dep.getName(), dep.getVersion())));

    List<RewritePlan> plans =
        Stream.concat(fromExceeded, fromUnresolved)
            .filter(candidate -> candidate.newVersion() != null)
            .filter(
                candidate ->
                    !coordinateWithVersion(candidate.dependency())
                        .matches(ignoreDependenciesRegexp))
            .map(
                candidate ->
                    buildRewritePlan(candidate.dependency(), candidate.newVersion(), "downgrade"))
            .toList();

    return filterToRewritablePlans(project, plans);
  }

  private static List<RewritePlan> getMissingVersionDependencies(
      Project project,
      Set<Dependency> undeclaredDependencies,
      String ignoreDependenciesRegexp,
      boolean setMissingVersions,
      boolean downgradeUnresolvedDependencies) {
    // A dependency with no version at all gives gradle-versions-plugin nothing to compare
    // against, so it carries no candidate-version data whatsoever (same situation as
    // "unresolved"). The only way to find one is our own resolution, which is exactly the
    // mechanism downgradeUnresolvedDependencies already provides - so filling in a missing
    // version is gated behind that same flag, not just setMissingVersions on its own.
    if (!setMissingVersions || !downgradeUnresolvedDependencies) {
      return List.of();
    }

    List<RewritePlan> plans =
        undeclaredDependencies.stream()
            .filter(dep -> isNonEmpty(dep.getGroup()) && isNonEmpty(dep.getName()))
            .map(
                dep ->
                    new Candidate<Dependency>(
                        dep, resolveLatestVersion(project, dep.getGroup(), dep.getName())))
            .filter(candidate -> candidate.newVersion() != null)
            .filter(
                candidate ->
                    !coordinateWithoutVersion(candidate.dependency())
                        .matches(ignoreDependenciesRegexp))
            .map(
                candidate ->
                    buildMissingVersionRewritePlan(candidate.dependency(), candidate.newVersion()))
            .toList();

    return filterToRewritablePlans(project, plans);
  }

  private static void handleDependencies(
      Project project,
      Result result,
      String ignoreDependenciesRegexp,
      boolean downgradeUnresolvedDependencies,
      boolean setMissingVersions,
      boolean writeChanges) {
    Logger logger = project.getLogger();
    List<RewritePlan> updatable =
        getUpdatableDependencies(
            project, result.getOutdated().getDependencies(), ignoreDependenciesRegexp);
    List<RewritePlan> downgradable =
        downgradeUnresolvedDependencies
            ? getDowngradableDependencies(
                project,
                result.getExceeded().getDependencies(),
                result.getUnresolved().getDependencies(),
                ignoreDependenciesRegexp)
            : List.<RewritePlan>of();
    List<RewritePlan> missing =
        getMissingVersionDependencies(
            project,
            result.getUndeclared().getDependencies(),
            ignoreDependenciesRegexp,
            setMissingVersions,
            downgradeUnresolvedDependencies);

    List<RewritePlan> changes = new ArrayList<>();
    changes.addAll(updatable);
    changes.addAll(downgradable);
    changes.addAll(missing);

    if (changes.isEmpty()) {
      logger.lifecycle(
          """

					☀️  All dependencies are up-to-date.
					""");
      return;
    }

    if (writeChanges) {
      applyChanges(project, changes, logger);
    } else {
      reportChanges(changes, logger);
    }
  }

  private static void applyChanges(Project project, List<RewritePlan> changes, Logger logger) {
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

  private static void reportChanges(List<RewritePlan> changes, Logger logger) {
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

  private static boolean runningInCI() {
    java.util.Map<String, String> env = System.getenv();
    return toBoolean(env.get("CI"))
        || toBoolean(env.get("GITHUB_ACTIONS"))
        || toBoolean(env.get("GITLAB_CI"))
        || toBoolean(env.get("TRAVIS"))
        || toBoolean(env.get("CIRCLECI"))
        || env.get("JENKINS_URL") != null
        || toBoolean(env.get("TF_BUILD")) // Azure Pipelines
        || toBoolean(env.get("BUILDKITE"));
  }

  private static boolean toBoolean(String value) {
    return value != null && Boolean.parseBoolean(value);
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
