<img align="right" src="https://github.com/finos/TimeBase-CE/blob/main/timebase-logo.svg">

# TimeBase CE

[![FINOS - Incubating](https://cdn.jsdelivr.net/gh/finos/contrib-toolbox@master/images/badge-incubating.svg)](https://finosfoundation.atlassian.net/wiki/display/FINOS/Incubating)

[![Maven Central](https://img.shields.io/maven-central/v/org.finos/finos.svg?maxAge=2592000)](https://search.maven.org/search?q=g:org.finos.timebase-ce)

## Business Problem

Time-series data is foundational to both buy-side and sell-side financial services firms. The need for time-series data management systems is well understood and is well-served by products provided under commercial licenses. There are also many open-source time-series data systems but their uptake in financial services firms is less extensive, possibly due to insufficient performance and scalability or lack of appropriateness for financial services time-series data.
There is constant demand to reduce total cost of operating technology stacks, whilst at the same time implementing changes to serve new opportunities or meet new obligations. Implementing change in a cost-effective manner has led to an increase in the use of cloud and open-source technologies, and less of a “one size fits all” approach.

## Proposed Solution

Having been battle-tested in trading firms for over 10 years under a commercial license, TimeBase is now offered under an open-source license as TimeBase Community Edition”. TimeBase Community Edition was designed from inception for managing time-series data found in financial services.

The goal is to provide enterprise grade time-series data management and messaging capability which enables financial services firms to use with other open-source and commercial components to augment existing solutions, create new solutions.

TimeBase can be used, simultaneously, as a traditional time-series database and real-time data messaging/streaming server. Data structures (“streams”) are user-defined. There is a rich API for interacting (ingesting, streaming) with TimeBase, which provide the necessary hooks for data population and data utilization.

## FINOS TimeBase-CE Resources

| Episode                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | Description                                                                                                                                                                                                                                                                                                                                                       |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| <a href="https://resources.finos.org/content/video-osff-nyc-2021-leveraging-the-open-source-timebase-ce-to-powercloud-hosted-analytical-application-ilya-gorelik-2021/?projects-sigs=timebase-ce"><img src="https://www.finos.org/hubfs/FINOS/2021%20OSSF%20NYC%20Working%20Folder/OSSF%202021%20Videos/Leveraging%20the%20Open-source%20TimeBase%20CE%20Event-oriented%20Time-series%20Database%20to%20PowerCloud%20Hosted%20Analytical%20Applications%20-%20Ilya%20Gorelik%2c%20EPAM%20Systems_e3.png" width="500"></a> | <a href="https://resources.finos.org/content/video-osff-nyc-2021-leveraging-the-open-source-timebase-ce-to-powercloud-hosted-analytical-application-ilya-gorelik-2021/?projects-sigs=timebase-ce">Leveraging TimeBase-CE event-oriented time-series database to PowerCloud hosted analytical application.</a>                                                     |
| <a href="https://resources.finos.org/content/timebase-ce-with-connectivity-to-crypto-exchanges/?projects-sigs=timebase-ce"><img src="https://www.finos.org/hubfs/OSFF%202022%20London/Videos/Click%20to%20View%20Recording/Matthew%20Gorelik%20-%20OSFF%20London%202022-3.png" width="500"></a>                                                                                                                                                                                                                           | <a href="https://resources.finos.org/content/timebase-ce-with-connectivity-to-crypto-exchanges/?projects-sigs=timebase-ce" rel="bookmark">In this session, Matt will talk about TimeBase Community Edition and how the Open Source Community can leverage this technology to build their own analytical models, data visualizations, and unique applications.</a> |

## Current State

### History:

- 2006 – Timebase 1.0 (historical data analytics, SQL)
- 2010 – Timebase 2.0 (timeseries data base, live streaming)
- 2015 – Timebase 5.0 (Very large databases, HDFS)
- 2018 – IPC/UDP low latency mode (Timebase topics)
- 2019 – Cloud support (REST/WS, Docker, Kuber)
- 2021 – Timebase 6.0 Community Edition (open source) and Clustering

## Roadmap

1. Replace Apache Tomcat
2. TimeBase Cluster
3. NIO Support for network layer

## Existing Materials

- [TimeBase repository](https://github.com/epam/TimeBase)
- [Web site](http://timebase.info)
- [Documentation](https://kb.timebase.info)

## Build

Build the project with [Gradle](http://gradle.org/) using this
[build.gradle](https://github.com/finos/TimeBase-CE/blob/main/build.gradle) file.

You will require the Java 11+ to build TimeBase:

Full clean and build of all modules

```shell
    $ ./gradlew clean build
```

## Run

Start TimeBase using [Docker](https://www.docker.com/)

```
docker run --rm -d \
    -p 8011:8011 \
    --name=timebase-server \
    --ulimit nofile=65536:65536 \
    finos/timebase-ce-server:6.1
```

## Usage example

https://kb.timebase.info/community/development/libs

## Releases

The following file lists known Timebase CE vulnerabilities:

https://github.com/finos/TimeBase-CE/blob/main/CVE.md

## Contributing

1. Fork it (<https://github.com/finos/TimeBase-CE/fork>)
2. Create your feature branch (`git checkout -b feature/fooBar`)
3. Read our [contribution guidelines](.github/CONTRIBUTING.md) and [Community Code of Conduct](https://www.finos.org/code-of-conduct)
4. Commit your changes (`git commit -am 'Add some fooBar'`)
5. Push to the branch (`git push origin feature/fooBar`)
6. Create a new Pull Request

_NOTE:_ Commits and pull requests to FINOS repositories will only be accepted from those contributors with an active, executed Individual Contributor License Agreement (ICLA) with FINOS OR who are covered under an existing and active Corporate Contribution License Agreement (CCLA) executed with FINOS. Commits from individuals not covered under an ICLA or CCLA will be flagged and blocked by the FINOS Clabot tool. Please note that some CCLAs require individuals/employees to be explicitly named on the CCLA. The EasyCLA bot will monitor all pull requests to ensure compliance.

_Need an ICLA? Unsure if you are covered under an existing CCLA? Email [help@finos.org](mailto:help@finos.org)_

## Development Team

- [Stuart Farr](https://github.com/stuartfarr) - Business
- Matthew Gorelik - Product Manager
- [Aliaksei Vavilov](https://github.com/avavilau) - Delivery Manager
- [Alex Karpovich](https://github.com/alex-karpovich) - Chief Solution Architect / Main Developer / Maintainer
- [Alexei Osipov](https://github.com/alexei-osipov) - Solution Architect / Main Developer
- [Andy Malakov](https://github.com/andymalakov) - Solution Architect
- [Raman Kisel](https://github.com/Romkisel) - Backend Developer
- [Daniil Yarmalkevich](https://github.com/ypldan) - Backend Developer
- [Maxim Gadalov](https://github.com/Maxim-Gadalov) - Backend Developer
- [Dzmitry Barkouski](https://github.com/MitoZ) - Front-end Developer
- [Vitalii Zavarskii](https://github.com/antshater) - Front-end Developer
- [Maksim Samusenka](https://github.com/msamusenka) - Solution Architect (Integrations)
- [Eugene Karankow](https://github.com/ekarankow) - Backend Developer (Integrations)
- [Polina Gurinovich](https://github.com/PolinaGurinovich) - Front-end Developer (Integrations)
- [Aliaksei Strakh](https://github.com/astrakh) - QA Automation
- [Sergei Remsha](https://github.com/sr-remsha) - Technical Writer

## License

Copyright © 2021 EPAM Systems, Inc.

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

SPDX-License-Identifier: [Apache-2.0](https://spdx.org/licenses/Apache-2.0)
