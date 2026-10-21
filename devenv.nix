{
  pkgs,
  config,
  ...
}:

{
  android = {
    enable = true;
    cmdLineTools.version = "22.0";
    platforms.version = [ "37.0" ];
    buildTools.version = [ "37.0.0" ];
    ndk = {
      enable = true;
      version = [ "28.2.13676358" ];
    };
    emulator.enable = true;
    systemImages.enable = true;
    systemImageTypes = [ "google_apis" ];
    abis = [ "x86_64" ];
    googleAPIs.enable = true;
    googleTVAddOns.enable = false;
    extras = [ ];
    cmake.version = [ ];
    android-studio.enable = false;
  };

  languages.java = {
    enable = true;
    jdk.package = pkgs.jdk17;
    gradle.enable = true;
  };

  languages.rust = {
    enable = true;
    channel = "stable";
    targets = [
      "aarch64-linux-android"
      "x86_64-linux-android"
    ];
  };

  packages = [
    pkgs.cargo-ndk
    pkgs.pkg-config
  ];

  env.ANDROID_SDK_ROOT = config.env.ANDROID_HOME;
  env.ANDROID_NDK_HOME = config.env.ANDROID_NDK_ROOT;

  # The Android module prepends build-tools and NDK libraries to
  # LD_LIBRARY_PATH. The emulator needs its own libc++ first.
  env.LD_LIBRARY_PATH = "${config.env.ANDROID_HOME}/emulator/lib64";
}
