#!/bin/bash

# Chromia CLI 0.27.7
mkdir -p build/chr
curl -fsSL "https://gitlab.com/chromaway/core-tools/chromia-cli/-/package_files/206845853/download" | tar -C build/chr -xzf -

# Postchain Management Console 3.51.1
mkdir -p build/pmc
curl -fsSL "https://gitlab.com/chromaway/core-tools/management-console/-/package_files/221787592/download" | tar -C build/pmc -xzf -
