let
  pkgs = import <nixpkgs> {};
  stdenv = pkgs.stdenv;
  zlib = pkgs.zlib;
  zsh = pkgs.zsh;
in rec {

  #eclipse-ud859 = callPackage ./eclipse-ud859.nix {};

  roboliqEnv = stdenv.mkDerivation rec {
    name = "roboliq-env";
    version = "1.0";
    src = ./.;
    buildInputs = with pkgs; [ scala simpleBuildTool zlib zsh ];
    extraCmds = ''
      # help sbt to find libz.so when using JHDF5
      export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:${zlib}/lib
    '';
    #shell = "${zsh}/bin/zsh";
    #shellHook = ''
    #exec ${zsh}/bin/zsh
    #'';
    #extraCmds = ''
    #${zsh}/bin/zsh
    #'';
  };
}
