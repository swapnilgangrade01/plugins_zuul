include_defs('//bucklets/gerrit_plugin.bucklet')

gerrit_plugin(
  name = 'zuul',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/**/*']),
  gwt_module = 'com.googlesource.gerrit.plugins.zuul.Zuul',
  manifest_entries = [
    'Gerrit-PluginName: zuul',
    'Gerrit-ApiType: plugin',
    'Gerrit-ApiVersion: 2.14-SNAPSHOT',
    'Gerrit-Module: com.googlesource.gerrit.plugins.zuul.Module',
    'Gerrit-HttpModule: com.googlesource.gerrit.plugins.zuul.HttpModule',
  ],
)

# this is required for bucklets/tools/eclipse/project.py to work
java_library(
  name = 'classpath',
  deps = [':zuul__plugin'],
)
