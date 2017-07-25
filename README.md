# Status

Reading from a project area to a file (and to a directory for attachments) works.

Writing to another project area from the previous file and directory works.

## Still to do

- change automatic links to work items in summaries and comments so that they point to the new item number, not the old one.


# What

Programs to copy (read then write) content from a source and to a target [Rational Team Concert](https://jazz.net/products/rational-team-concert) (RTC) project area (PA). The following repository objects are copied:

- categories
- development lines and iterations
- work item links, attachments, approvals, comments
- work item history (all the work item "versions").

No existing elements in the target PA are deleted, the writing part only adds new elements.

## Limitations

- Work item types and workflows must be "compatible".
- Only work items from the considered PA are taken into account (no link to items in other project areas for example).
- History in the target PA will show the user the tool uses to log in and the timestamps will correspond to when the objects are written.
(But see workaround below.)
- Timelines (development lines, iterations) will be re-created in the target project area. The write program doesn’t try to reuse existing development lines or iterations if some exist (and then, they should probably be archived). Note: they should probably be renamed as the writing program puts a index in the ID/name.
- Links between work items inside the read PA are the only one taken into account.
- If a user is not part of the source project area anymore, and can’t be found in the input matching file, s·he will be replaced by the user running the program.
- Who resolves a work item will be the user running the migration tool (who is the one triggering the state change action). See below for the a workaround to see who did it originally.


# Build

These programs (`rtc.pa.connection_test.plain`, `rtc.pa.read.plain` and `rtc.pa.write.plain`) use the RTC API.

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
  
Don’t try to use a version of the API different from the version of RTC you want to connect to, this won’t work.

The version of the API declared in the class paths of the regular Eclipse projects in this repository corresponds to the latest version of RTC, unziped in `/opt/IBM/RTC-6.0.4/api/`

Different projects `version*` are already set for different version of the API. This will work if the local installation has the `/opt/IBM/RTC-x.y.z/api/` directories already prepared.

You will have to change your Eclipse project properties if this doesn’t match your local installation (or if you are working on a non-UNIX plaform).


# Usage

The typical needed arguments are:

- CCM server URL
- project name
- user ID
- password
- ... (see each program usage in its `Main.java`)

For example:

`https://hub.jazz.net/ccm13 "UU | PPP" jazz_admin iloveyou`

## Read program usage

`rtc.pa.read.Main source_repository_url source_project_area_name login password serialization_output_file ouput_directory_for_attachments`

For example:

`rtc.pa.read.Main https://old.example.com/ccm 'HR UX' admin 'Xy0H!T,K7m' 'HR UX.ser' attachments`

## Trace .ser file

`rtc.pa.model.trace.Main serialization_file`

For example:

`rtc.pa.model.trace.Main 'HR UX.ser'`

## Write program usage

`rtc.pa.write.Main target_repository_url target_project_area_name login password serialization_input_file input_directory_for_attachments matching_members_input_file matching_workitem_ids_output_file`

For example:

`rtc.pa.write.Main https://ccm.example.com/ccm 'HR UX' migrator 'jK12l;:-)8:-)' 'HR UX.ser' attachments members.txt workitem_ids.txt`

Note: `matching_members_input_file` has to be a UTF-8 text file with a line for each member; this line should read like:

> `ID_in_source ID_in_target`

(two IDs separated by spaces).

Don’t forget the special user `unassigned`, who should still be the same in the target project area, unless you made the administrative operation to change its name. So, there also should be a line like:

> `unassigned unassigned`


## Preconditions

The target project area should already exist, with its users.

It would be configured so that each item type has the following 4 custom attributes (see next §).


# Behavior

## Workaround for work items history

In the target project areas, work items versions will be shown as created by the user the migration tool used to log in, and the timestamps will correspond to when the migration took place.
There is currently no way to override this.

As a workaround, the target PA process can be customized to add the following two custom attributes to all the work items:

- Suggested name: `Original modification date`, ID: `rtc.pa.modified`, Type: `Timestamp`
- Suggested name: `Original modifier`, ID: `rtc.pa.modifier`, Type: `Contributor`

Another custom attribute will be used (if it exists) to help remember the previous ID (the one from the source PA) for each work item:

- Suggested name: `Original ID`, ID: `rtc.pa.id`, Type: `Integer`

Yet another custom attribute will be used (if it exists) to show in the history who the resolver was, in case the change is a work item resolution:

- Suggested name: `Original resolver`, ID: `rtc.pa.resolver`, Type: `Contributor`

## Migration specific custom attributes

If any of these custom attributes (`rtc.pa.modified`, `rtc.pa.modifier`, `rtc.pa.id`, `rtc.pa.resolver`) exist in the target PA, they will be used and their value set in the work item histories to reflect what took place when and by whom in the source PA. The value of `rtc.pa.id` could also be displayed in the work item forms.

## More on old work item IDs

The following behaviors can be changed in the code of `rtc.pa.write.text.Transposition`.

### Work item summary

Each work item summary will be prefixed by the original work item ID between braces:

> ga bu zo meu

becomes:

> {12345} ga bu zo meu

, where 12345 is the ID of the same work item in the source repository.

### Descriptions and comments

Furthermore, "automatic hyperlinks" are transposed, using the following algorithm. In each description and comment, text like:

> ga bu 12345 zo meu

, where "12345" is the ID of the work item in the source PA, will be modified to become:

> ga bu 678 {12345} zo meu

, where "678" is the corresponding work item ID in the target PA.


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

> [Discussion on design here](DESIGN.md).


# Thanks

_Vielen Dank_ [Ralph](https://github.com/rsjazz) for your [direct](https://jazz.net/forum/users/rschoon) or [indirect](https://rsjazz.wordpress.com) valuable help.