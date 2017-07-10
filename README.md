# Status

In development. Not usable yet.

# What

Programs to copy (read then write) content from a source and to a target Rational Team Concert project area. The following repository objects are copied:

- categories
- development lines and iterations
- work item types with their history.

## Limitations

History in the target project area will show the user the tool uses to log in and the timestamps will correspond to when the objects are written.

# Build

These programs uses the Rational Team Concert API. The reading program and the writing programs can log in to different version of Rational Team Concert.

For each version of Rational Team Concert you will need to connect to:

- go to <https://jazz.net/downloads/rational-team-concert>
- click the version of Rational Team Concert
- click the "All Downloads" tab
- download the "Plain Java Client Libraries"
- unzip.

For each Eclipse project:

- use the correct version of the API jar files for building each Eclipse project where this is needed, for example:
  - `rtc.pa.read.plain` will connect to an RTC 5.0.2, hence will need the 5.0.2 version of the API
  - `rtc.pa.write.plain` will connect to an RTC 6.0.4, hence will need the 6.0.4 version of the API.
  

# Usage

The needed arguments are:

- CCM server URL
- project name
- user ID
- password
- ... (see each program usage in `Main.java`)

For example:

`https://hub.jazz.net/ccm01 "UU | PPP" jazz_admin iloveyou`

## Workaround for work items history

In the target project areas, work items versions will be shown as created by the user the migration tool used to log in, and the timestamps will correspond to when the migration took place.
There's currently no way to override this.

As a workaround, the project area process can be customized to add the following two custom attributes to all the workitems:

- ID: `rtc.pa.modified`, Type: `Timestamp`
- ID: `rtc.pa.modifier`, Type: `Contributor`

Then, this will be added to the history to reflect in the target project area what took place when and by whom in the source project area.

# Special

## Connection to Bluemix Track&Plan (aka JazzHub)

Note: Bluemix Track&Plan is Rational Team Concert version 5.0.2.

The URL used to access Bluemix Track&Plan looks like `https://hub.jazz.net/project/UUU/PPP` where `UUU` is a user name and `PPP` a project name.

You need the real project area URL. For that:

- click "Track and Plan > Project Dashboard"
- the page you reach looks like `https://hub.jazz.net/ccmXX/web/projects/UUU | PPP`
  - `https://hub.jazz.net/ccmXX` is the CCM server URL
  - `UUU | PPP` is the project name.
  
