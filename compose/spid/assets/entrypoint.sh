#!/bin/bash

cp /trust/igi-test-ca.pem /usr/local/share/ca-certificates/igi-test-ca.crt

update-ca-certificates

python spid-testenv.py $@
