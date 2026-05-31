const productionMode = config.mode === "production";
config.watch = ! productionMode;

// https://youtrack.jetbrains.com/issue/KT-50826
config.performance = {
    hints: false
};

// Replace webpack's default Terser minifier with esbuild's (10-100x faster minification).
// esbuild ships per-platform native binaries via npm, so this stays Windows/Linux agnostic.
if (productionMode) {
    const { EsbuildPlugin } = require("esbuild-loader");
    config.optimization = config.optimization || {};
    config.optimization.minimizer = [
        new EsbuildPlugin({ target: "es2017", legalComments: "external" })
    ];
}