#!/usr/bin/env bash
# See https://gist.github.com/bittner/5436f3dc011d43ab7551
# cross-OS compatibility (greadlink, gsed, zcat are GNU implementations for OS X)
[[ `uname` == 'Darwin' ]] && {
    which greadlink gsed gzcat > /dev/null && {
        echo "GNU utils available"
    } || {
        echo 'ERROR: GNU utils required for Mac. You may use homebrew to install them:'
        echo 'brew install coreutils gnu-sed'
        echo 'or you may use Mac Ports to install them:'
        echo 'sudo port install coreutils gsed'
        exit 1
    }
}
