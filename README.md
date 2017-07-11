# Status

In development. Not usable yet.

# What

Programs to copy (read then write) content from a source and to a target [Rational Team Concert](https://jazz.net/products/rational-team-concert) (RTC) project area (PA). The following repository objects are copied:

- categories
- development lines and iterations
- work item types with their history (all the work item "versions").

## Limitations

- Work item types and workflows must be "compatible".
- History in the target PA will show the user the tool uses to log in and the timestamps will correspond to when the objects are written.
(But see workaround below.)
- The timelines will be re-created in the target project area, the program doesn't try to reuse existing development lines or iterations if some exist (and then, they should probably be archived).
- Some work item properties are not copied:
  - Approvals
  - Subscribers.
- Links and attachments are added to the last (most recent) version of the work item (not updated for each version in the history).
- If a user is not part of the source project area anymore, and can't be found in the input matching file, it will be replaced by the user running the program.

# Build

These programs uses the RTC API. The reading program and the writing programs can log in to different versions of RTC.

For each version of RTC you will need to connect to:

- go to <https://jazz.net/downloads/rational-team-concert>
- click the version of Rational Team Concert
- click the "All Downloads" tab
- download the "Plain Java Client Libraries"
- unzip.

For each Eclipse project needing the API:

- use the correct version of the API jar files for building each Eclipse project where this is needed, for example:
  - `rtc.pa.read.plain` connects to an RTC 5.0.2, hence needs the 5.0.2 version of the API
  - `rtc.pa.write.plain` connects to an RTC 6.0.4, hence needs the 6.0.4 version of the API.
  
Don't try to use a version of the API different from the version of RTC you want to connect to, this won't work.  

# Usage

The needed arguments are:

- CCM server URL
- project name
- user ID
- password
- ... (see each program usage in `Main.java`)

For example:

`https://hub.jazz.net/ccm01 "UU | PPP" jazz_admin iloveyou`

## Preconditions

The target project areas should already exist, with its users.

## Workaround for work items history

In the target project areas, work items versions will be shown as created by the user the migration tool used to log in, and the timestamps will correspond to when the migration took place.
There's currently no way to override this.

As a workaround, the target PA process can be customized to add the following two custom attributes to all the workitems:

- ID: `rtc.pa.modified`, Type: `Timestamp`
- ID: `rtc.pa.modifier`, Type: `Contributor`

If these attributes exist, they will be added to the history to reflect in the target PA what took place when and by whom in the source PA.

# Special

## Connection to Bluemix Track&Plan (aka JazzHub)

Note: Bluemix Track&Plan is [Rational Team Concert version 5.0.2](https://jazz.net/downloads/rational-team-concert/releases/5.0.2?p=allDownloads).

The URL used to access Bluemix Track&Plan looks like `https://hub.jazz.net/project/UUU/PPP` where `UUU` is a user name and `PPP` a project name.

You need the real project area URL. For that:

- click "Track and Plan > Project Dashboard"
- the page you reach looks like `https://hub.jazz.net/ccmXX/web/projects/UUU | PPP`
  - `https://hub.jazz.net/ccmXX` is the CCM server URL
  - `UUU | PPP` is the project name.
  
# Design

A read program reads the source PA and creates a model instance in memory, and then serializes it in a local file.

A write program reads the above local file, a file matching the user IDs before to the one after, and then writes to the target PA.
