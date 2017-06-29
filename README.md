# What

Programs to copy (read then write) content from a source and to a target Rational Team Concert project area. The following repository objects are copied:

- categories
- development lines and iterations
- work item types with their history.

## Limitations

History in the target project area will show the user the tool uses to log in and the timestamps will correspond to when the objects are written.

# Build

This programs uses the Rational Team Concert API. The reading program and the writing programs can log in to different version of Rational Team Concert. For each concerned Eclipse project:

- go to <https://jazz.net/downloads/rational-team-concert>
- click the version of Rational Team Concert you will connect to
- click the "All Downloads" tab
- download the "Plain Java Client Libraries"
- unzip
- use the jar files for building.

# Usage

The needed arguments are:

- CCM server URL
- project name
- user ID
- password
- ... (see each program usage in `Main.java`)

For example:

`https://hub.jazz.net/ccm01 "UU | PPP" jazz_admin iloveyou`

# Special

## Connection to Bluemix Track&Plan (aka JazzHub)

Note: Bluemix Track&Plan is Rational Team Concert version 5.0.2.

The URL used to access Bluemix Track&Plan looks like `https://hub.jazz.net/project/UUU/PPP` where `UUU` is a user name and `PPP` a project name.

You need the real project area URL. For that:

- click "Track and Plan > Project Dashboard"
- the page you reach looks like `https://hub.jazz.net/ccmXX/web/projects/UUU | PPP`
  - `https://hub.jazz.net/ccmXX` is the CCM server URL
  - `UUU | PPP` is the project name.
  
