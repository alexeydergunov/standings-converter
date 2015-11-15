# Standings Converter

[![Build Status](https://travis-ci.org/alexeydergunov/standings-converter.svg?branch=master)]
(https://travis-ci.org/alexeydergunov/standings-converter)

Standings Converter is an utility for converting submission logs of programming contests. Different testing systems use
their own format of these logs, and that causes troubles when you try to copy a contest from one testing system to
another. Usually people just don't import monitor and submission logs. This project is to help you to import them.

## Supported testing systems

For now, it can only parse Ejudge run logs and convert them to Testsys logs. Testsys logs are used in Codeforces Gyms,
and therefore it's quite necessary to have such utility to import submissions and standings to gym contests.

## Usage

1. Download and install Maven, enter the root directory of this project and execute `mvn clean install`.
2. Obtain a submission log you want to convert. If you use ejudge, enter to the contest as an admin (to the master page)
and press "Export runs in XML external format" link. Save the opened text in the target directory of the project.
3. Execute `java -jar java -jar [jarFile] [ejudgeInputFile] [testsysOutputFile]`. Parameter `jarFile` is the name of the
jar in the target directory, including the extension, `ejudgeInputFile` is the file imported from ejudge, and
`testsysOutputFile` is the file you want the converted log to be saved to.
