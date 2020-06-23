load("@rules_java//java:defs.bzl", "java_library")
load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)

gerrit_plugin(
    name = "zuul",
    srcs = glob(["src/main/java/**/*.java"]),
    resources = glob(["src/main/**/*"]),
    manifest_entries = [
        "Gerrit-PluginName: zuul",
        "Gerrit-Module: com.googlesource.gerrit.plugins.zuul.Module",
    ],
)

junit_tests(
    name = "zuul_tests",
    testonly = 1,
    srcs = glob(["src/test/java/**/*.java"]),
    tags = ["zuul"],
    deps = [
        ":zuul__plugin_test_deps",
    ],
)

java_library(
    name = "zuul__plugin_test_deps",
    testonly = 1,
    visibility = ["//visibility:public"],
    exports = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":zuul__plugin",
    ],
)

