#!/bin/sh
set -eu
API_URL="${API_URL:-/api}"
# Restrict configuration to a same-origin path and safe JavaScript string characters.
case "$API_URL" in
  /[a-zA-Z0-9]*) ;;
  *) echo 'API_URL must be a same-origin path' >&2; exit 1 ;;
esac
case "$API_URL" in
  *[!a-zA-Z0-9/_-]*) echo 'API_URL contains invalid characters' >&2; exit 1 ;;
esac
printf 'window.__AROME_CONFIG__ = { apiUrl: "%s" };\n' "$API_URL" > /usr/share/nginx/html/config.js
