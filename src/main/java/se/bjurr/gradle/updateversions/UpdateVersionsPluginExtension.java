package se.bjurr.gradle.updateversions;

import org.gradle.api.provider.Property;

public interface UpdateVersionsPluginExtension {
  Property<String> getIgnoreDependenciesRegexp();

  Property<Boolean> getDowngradeUnresolvedDependencies();

  Property<Boolean> getSetMissingVersions();
}
