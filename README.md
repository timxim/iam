# INDIGO Identity and Access Management (IAM) service

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.1874791.svg)](https://doi.org/10.5281/zenodo.1874791)
[![travis-build-tatus](https://travis-ci.org/indigo-iam/iam.svg?branch=develop)](https://travis-ci.org/indigo-iam/iam)
[![sonarqube-qg](https://sonarcloud.io/api/project_badges/measure?project=indigo-iam_iam&metric=alert_status)](https://sonarcloud.io/dashboard?id=indigo-iam_iam)
[![sonarqube-coverage](https://sonarcloud.io/api/project_badges/measure?project=indigo-iam_iam&metric=coverage)](https://sonarcloud.io/dashboard?id=indigo-iam_iam)
[![sonarqube-maintainability](https://sonarcloud.io/api/project_badges/measure?project=indigo-iam_iam&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=indigo-iam_iam)

The INDIGO IAM is an Identity and Access Management service first developed in the
context of the [INDIGO-Datacloud H2020 project][indigo-datacloud].

## Main features

- OpenID connect provider based on the [MitreID OpenID connect library][mitreid]
- [SCIM][scim] user provisioning and management APIs
- SAML authentication support
- Google authentication support 
- [OAuth token exchange][token-exchange] support

## What's new

See the [changelog](CHANGELOG.md).

## Documentation

See the [IAM documentation][iam-doc].

## License

[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)

[indigo-datacloud]: https://www.indigo-datacloud.eu/ 
[mitreid]: https://github.com/mitreid-connect/OpenID-Connect-Java-Spring-Server
[scim]: http://www.simplecloud.info/
[token-exchange]: https://tools.ietf.org/html/draft-ietf-oauth-token-exchange-09
[iam-doc]: https://indigo-iam.github.io/docs
