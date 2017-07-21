# Status

Reading a project area to a file (and directory for attachments) works.

Writing is still in development (barely usable yet).

# What

Programs to copy (read then write) content from a source and to a target [Rational Team Concert](https://jazz.net/products/rational-team-concert) (RTC) project area (PA). The following repository objects are copied:

- categories
- development lines and iterations
- work item links, attachments, approvals, comments
- work item history (all the work item "versions")

## Limitations

- Work item types and workflows must be "compatible".
- History in the target PA will show the user the tool uses to log in and the timestamps will correspond to when the objects are written.
(But see workaround below.)
- The timelines will be re-created in the target project area, the program doesn't try to reuse existing development lines or iterations if some exist (and then, they should probably be archived).
- Links between work items inside the read PA are the only one taken into account.
- If a user is not part of the source project area anymore, and can't be found in the input matching file, s·he will be replaced by the user running the program.

# Build

These program (`rtc.pa.connection_test.plain`, `rtc.pa.read.plain` and `rtc.pa.write.plain`) use the RTC API.

Each program may need to connect to a specific version of RTC. For each needed version of RTC:

- go to <https://jazz.net/downloads/rational-team-concert>
- click the version of Rational Team Concert
- click the "All Downloads" tab
- download the "Plain Java Client Libraries"
- unzip.

For each Eclipse project needing the API:

- use the correct version of the API jar files for building each Eclipse project where this is needed; for example:
  - `rtc.pa.read.plain` connects to an RTC 5.0.2, hence needs the 5.0.2 version of the API
  - `rtc.pa.write.plain` connects to an RTC 6.0.4, hence needs the 6.0.4 version of the API
  - `rtc.pa.connection_test.plain` connects to both an RTC 5.0.2 and an RTC 6.0.4, hence the Eclipse project needs to be duplicated, one for the 5.0.2 version of the API, and one for the 6.0.4 version of the API.
  
Don't try to use a version of the API different from the version of RTC you want to connect to, this won't work.

The version of the API declared in the class paths of the regular Eclipse projects in this repository corresponds to the latest version of RTC, unziped in `/opt/IBM/RTC-6.0.4/api/`

Different projects `version*` are already set for different version of the API. This will work if the local installation has the `/opt/IBM/RTC-x.y.z/api/` directories already prepared.

You will have to change your Eclipse project properties if this doesn't match your local installation (or if you are working on a non-UNIX plaform).


# Usage

The typical needed arguments are:

- CCM server URL
- project name
- user ID
- password
- ... (see each program usage in `Main.java`)

For example:

`https://hub.jazz.net/ccm13 "UU | PPP" jazz_admin iloveyou`

## Preconditions

The target project area should already exist, with its users.

## Workaround for work items history

In the target project areas, work items versions will be shown as created by the user the migration tool used to log in, and the timestamps will correspond to when the migration took place.
There's currently no way to override this.

As a workaround, the target PA process can be customized to add the following two custom attributes to all the workitems:

- ID: `rtc.pa.modified`, Type: `Timestamp`
- ID: `rtc.pa.modifier`, Type: `Contributor`

If these custom attributes exist in the target PA, they will be used and set in the work item histories to reflect what took place when and by whom in the source PA.

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

A read program reads the source PA and creates a model instance in memory, and then serializes it to a local file. The attachments to the projects are saved to a directory.

A write program reads the above local files, and, given a file matching each user ID before (source PA) and after (target PA), writes to the target PA.

# Thanks

A special _Danke_ to [Ralph Schoon](/rsjazz) for his [direct](https://jazz.net/forum/users/rschoon) or [indirect](https://rsjazz.wordpress.com) valuable help.