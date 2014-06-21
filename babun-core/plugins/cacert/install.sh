#!/bin/bash
set -e -f -o pipefail
source "/usr/local/etc/babun.instance"
source "$babun_tools/script.sh"

cd /usr/ssl/certs
curl -4 http://curl.haxx.se/ca/cacert.pem | /bin/awk 'split_after==1{n++;split_after=0} /-----END CERTIFICATE-----/ {split_after=1} {print > "cert" n ".pem"}'
c_rehash
