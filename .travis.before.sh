#!/bin/bash
echo "[url \"https://github.com/kryptco\"]\n\tinsteadOf = git@github.com:kryptco" >> ~/.gitconfig
git submodule update --init --recursive
