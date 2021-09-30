# !!!! IMPORTANT !!!!

Currently, this module is using `jackson-dataformat-toml` for toml reading and writing, because it
is compatible with toml 1.0.0 . It dumps correct toml syntax, but the syntax dumped is not "pretty"
syntax. For now, we recommend creating the config file by your own, and only load with the available
features. We hope this changes in the future.