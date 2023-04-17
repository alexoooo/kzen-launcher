const productionMode = config.mode === "production";
config.watch = ! productionMode;
