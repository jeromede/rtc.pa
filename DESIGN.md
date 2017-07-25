# DESIGN

## Second approach ([1.0.0-rc.1]())

First create a (first version of) each work item, its links; then, for each work item again, continue with the other versions:

1) Create categories
2) Create development lines and iterations
3) For each work item, create a minimal work item version, with:
    - links
    - attachments
    - approvals
4) For each work item again, create each version from the history:
    - builtin attributes
    - custom attributes
    - comments
    - tags
    - subscribers
    - change state if needed
    - change WI type if needed

The change was more or less a refactoring of the previous algorithm.

Note: some other changes make the new model incompatible with the previous one, hence the older serialized model instances won’t be readable.

## First approach ([1.0.0-pre.1]())

1) Create categories
2) Create development lines and iterations
3) For each work item:
   1) Create work item
   2) Create each version from the history:
      - builtin attributes
      - custom attributes
      - comments
      - tags
      - subscribers
      - change state if needed
      - change WI type if needed
   3) Create links
   4) Upload attachments
   5) Create approvals.

There’s a problem there: it’s impossible to know in advance the number of a not already created WI, but this knewledge  required to change the special WI links in comments, etc.
