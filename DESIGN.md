# DESIGN

## Second approach ([1.0.0-rc.4](https://github.com/jeromede/rtc.pa/releases/tag/1.0.0-rc.4))

Inverted steps 4 and 5 from the below process.

## Second approach ([1.0.0-rc.3](https://github.com/jeromede/rtc.pa/releases/tag/1.0.0-rc.3))

First create a (first version of) each work item, its links; then, for each work item again, continue with the other versions:

1) Create categories
2) Create development lines and iterations
3) For each work item, create a minimal work item version
    - and collect the matching between source IDs and target IDs (needed for step 4)
4) For each work item again, update by adding each version from the history (+ change automatic links in descriptions and comments):
    - builtin attributes
    - custom attributes
    - comments
    - tags
    - subscribers
    - change state if needed
    - change WI type if needed
5) For each work item again, update with:
    - links
    - attachments
    - approvals

This has been more or less a reordering of the previous algorithm.

Note: some other changes make the new model incompatible with the previous one, hence older serialized model instances won’t be readable.

## First approach ([1.0.0-pre.1](https://github.com/jeromede/rtc.pa/releases/tag/1.0.0-pre.1))

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
