include_defs('//bucklets/gerrit_plugin.bucklet')

gerrit_plugin(
  name = 'dependson',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/**/*']),
  gwt_module = 'com.googlesource.gerrit.plugins.dependson.DependsOn',
  manifest_entries = [
    'Gerrit-PluginName: dependson',
    'Gerrit-ApiType: plugin',
    'Gerrit-ApiVersion: 2.13-SNAPSHOT',
    'Gerrit-Module: com.googlesource.gerrit.plugins.dependson.Module',
    'Gerrit-HttpModule: com.googlesource.gerrit.plugins.dependson.HttpModule',
  ],
)

# this is required for bucklets/tools/eclipse/project.py to work
java_library(
  name = 'classpath',
  deps = [':dependson__plugin'],
)
