#!/usr/bin/env bash
#
# Runs F-Droid's own `fdroid lint` against the recipe in fdroid/metadata/.
#
# This checks the things a fdroiddata reviewer checks first: required fields,
# valid anti-features and categories, a licence the F-Droid catalog accepts, a
# plausible Builds entry. It does NOT build the app and it does NOT sign
# anything — F-Droid's build server does that after the merge request is merged.
#
# Everything it needs lives under .tmp_fdroid/ (gitignored): a throwaway Python
# virtualenv and a miniature "repo" holding our metadata plus the anti-feature
# and category lists fetched from fdroiddata, which is what lint validates
# against. Nothing outside this folder is installed or modified.
#
#   bash tools/fdroid-lint.sh            # lint every app in fdroid/metadata
#   bash tools/fdroid-lint.sh <appid>    # lint one app
#
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
work="$here/.tmp_fdroid"
venv="$work/venv"
repo="$work/repo"
upstream="https://gitlab.com/fdroid/fdroiddata/-/raw/master"

# ---------------------------------------------------------------- 1. fdroidserver
if [ -x "$venv/bin/python" ]; then
    bin="$venv/bin"
elif [ -x "$venv/Scripts/python.exe" ]; then
    bin="$venv/Scripts"
else
    py=""
    for candidate in python3 python; do
        if command -v "$candidate" >/dev/null 2>&1; then
            py="$candidate"
            break
        fi
    done
    if [ -z "$py" ]; then
        echo "Python 3.9+ bulunamadı; fdroidserver kurulamaz." >&2
        exit 1
    fi
    echo "→ sanal ortam oluşturuluyor ($py)"
    "$py" -m venv "$venv"
    bin="$venv/bin"
    [ -x "$bin/python" ] || bin="$venv/Scripts"
fi

if [ ! -x "$bin/fdroid" ] && [ ! -f "$bin/fdroid.exe" ]; then
    echo "→ fdroidserver kuruluyor"
    "$bin/python" -m pip install --quiet --upgrade pip
    "$bin/python" -m pip install --quiet fdroidserver
fi

fdroid="$bin/fdroid"
[ -x "$fdroid" ] || fdroid="$bin/fdroid.exe"

# ------------------------------------------------------------- 2. miniature repo
# lint reads its rules from the repo it is run in, so the anti-feature and
# category catalogs have to be the real ones from fdroiddata, not a guess.
mkdir -p "$repo/metadata" "$repo/config"
cp "$here"/fdroid/metadata/*.yml "$repo/metadata/"

fetch() {
    echo "→ $2"
    curl -fsSL -o "$repo/$2" "$upstream/$1"
}
fetch config.yml config.yml
fetch config/antiFeatures.yml config/antiFeatures.yml
fetch config/categories.yml config/categories.yml

# The catalogs reference icons; lint copies them while loading. Empty files are
# enough to satisfy the copy, and this keeps the checkout free of build assets.
grep -hE '^[[:space:]]*icon:' "$repo"/config/*.yml 2>/dev/null \
    | sed 's/.*icon:[[:space:]]*//' \
    | tr -d "\"' \r" \
    | sort -u > "$work/icons.txt" || true

while IFS= read -r icon; do
    if [ -n "$icon" ] && [ ! -f "$repo/config/$icon" ]; then
        : > "$repo/config/$icon"
    fi
done < "$work/icons.txt"

# ------------------------------------------------------------------- 3. run lint
cd "$repo"
"$fdroid" lint "$@"
