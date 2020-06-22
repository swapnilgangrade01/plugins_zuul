load("//tools/bzl:plugin.bzl", "gerrit_plugin")

gerrit_plugin(
    name = "zuul",
    srcs = glob(["src/main/java/**/*.java"]),
    resources = glob(["src/main/**/*"]),
    manifest_entries = [
        "Gerrit-PluginName: zuul",
        "Gerrit-Module: com.googlesource.gerrit.plugins.zuul.Module",
    ],
)
