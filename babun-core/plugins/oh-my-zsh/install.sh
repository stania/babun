#!/bin/bash
set -e -f -o pipefail
source "/usr/local/etc/babun.instance"
source "$babun_tools/script.sh"
source "$babun_tools/git.sh"

src="$babun/external/oh-my-zsh"
dest="$babun/home/oh-my-zsh/.oh-my-zsh"

if [ ! -d "$src" ]; then
	PATH=/usr/bin git config --system http.sslverify false
	PATH=/usr/bin git clone git://github.com/robbyrussell/oh-my-zsh.git "$src" 
	PATH=/usr/bin git config --system http.sslverify true
fi

if [ ! -d "$dest" ]; then
	mkdir -p "$dest"
    /bin/cp -rf "$src/." "$dest"
    /bin/cp "$dest/templates/zshrc.zsh-template" "$babun/home/.zshrc"
    /bin/sed -i 's/ZSH_THEME=".*"/ZSH_THEME="babun"/' "$babun/home/.zshrc"
    /bin/cp -rf "$babun_source/babun-core/plugins/oh-my-zsh/src/babun.zsh-theme" "$dest/custom"
fi
