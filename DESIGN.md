# DESIGN

## Second approach (in development)

First create a (first version of) each WI, its links, then continue with the other versions.

The change is more or less a refacttoring of the previous algorithm.

## First approach ([1.0.0-pre.1]())

1) Create categories
2) Create development lines and iterations
3) For each work item:
   1) Create work item
   2) Create each version from the history:
      - builtin attributes,
      - custom attributes,
      - comments
      - tags
      - subscribers
      - change state if needed
      - change WI type if needed
   3) Create links
   4) Upload attachments
   5) Create approvals.

There are to problems

1) Impossible to know in advance the number of a not already created WI, but this is required to change the special WI links in comments, etc.
2) When resolving a WI to a duplicate state, a duplicate link is required; hence the links have to exist first. This is bug in 1.0.0-pre.1.
