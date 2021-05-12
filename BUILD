load("@rules_java//java:defs.bzl", "java_library")
load("@npm//@bazel/rollup:index.bzl", "rollup_bundle")
load("//tools/bzl:junit.bzl", "junit_tests")
load("//tools/js:eslint.bzl", "eslint")
load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)
load("//tools/bzl:genrule2.bzl", "genrule2")
load("//tools/bzl:js.bzl", "polygerrit_plugin")

gerrit_plugin(
    name = "zuul",
    srcs = glob(["src/main/java/**/*.java"]),
    resources = glob(["src/main/**/*"]),
    deps = ["@commons-lang3//jar"],
    manifest_entries = [
        "Gerrit-PluginName: zuul",
        "Gerrit-Module: com.googlesource.gerrit.plugins.zuul.Module",
        "Gerrit-HttpModule: com.googlesource.gerrit.plugins.zuul.HttpModule",
    ],
    resource_jars = [":gr-zuul-static"],
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
        "@commons-lang3//jar",
    ],
)

genrule2(
    name = "gr-zuul-static",
    srcs = [":gr-zuul"],
    outs = ["gr-zuul-static.jar"],
    cmd = " && ".join([
        "mkdir $$TMP/static",
        "cp $(locations :gr-zuul) $$TMP/static",
        "cd $$TMP",
        "zip -Drq $$ROOT/$@ -g .",
    ]),
)

polygerrit_plugin(
    name = "gr-zuul",
    app = "zuul-bundle.js",
    plugin_name = "zuul",
)

rollup_bundle(
    name = "zuul-bundle",
    srcs = glob(["gr-zuul/*.js"]),
    entry_point = "gr-zuul/plugin.js",
    rollup_bin = "//tools/node_tools:rollup-bin",
    sourcemap = "hidden",
    format = "iife",
    deps = [
        "@tools_npm//rollup-plugin-node-resolve",
    ],
)

# Define the eslinter for the plugin
# The eslint macro creates 2 rules: lint_test and lint_bin
eslint(
    name = "lint",
    srcs = glob([
        "gr-zuul/**/*.js",
    ]),
    config = ".eslintrc.json",
    data = [],
    extensions = [
        ".js",
    ],
    ignore = ".eslintignore",
    plugins = [
        "@npm//eslint-config-google",
        "@npm//eslint-plugin-html",
        "@npm//eslint-plugin-import",
        "@npm//eslint-plugin-jsdoc",
    ],
)

