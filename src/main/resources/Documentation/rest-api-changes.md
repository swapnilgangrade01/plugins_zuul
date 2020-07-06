@PLUGIN@ - /changes/ REST API
==============================

This page describes the '/changes/' REST endpoints that are added by
the @PLUGIN@ plugin.

Please also take note of the general information on the
[REST API](../../../Documentation/rest-api.html).

<a id="plugin-endpoints"> @PLUGIN@ Endpoints
--------------------------------------------

### <a id="get-crd"> Get CRD

'GET /changes/[\{change-id\}](../../../Documentation/rest-api-changes.html#change-id)/revisions/[\{revision-id\}](../../../Documentation/rest-api-changes.html#revision-id)/crd'

Gets the zuul [CRD](#crd-info) for a change.  Please refer to the
general [changes rest api](../../../Documentation/rest-api-changes.html#get-review)
for additional info on this request.

#### Request

```
  GET /changes/myProject~master~I8473b95934b5732ac55d26311a706c9c2bde9940/revisions/674ac754f91e64a0efb8087e59a176484bd534d1/crd HTTP/1.0
```

As response a [CrdInfo](#crd-info) entity is returned that describes the cross-repository dependencies.

#### Response

```
  HTTP/1.1 200 OK
  Content-Disposition: attachment
  Content-Type: application/json; charset=UTF-8

  )]}'
  {
    "depends_on_found": [
      {
        "id": "repo1~master~Ic0f5bcc8f998dfc0f1b7164de7a824f7832d4abe",
        "project": "zuul/repo1",
        "branch": "master",
        [...]
      }
    ],
    "depends_on_missing": [
      "Ib01834990d3791330d65c469e9a3f93db6eb41f0",
      "Ic0f5bcc8f998dfc0f1b7164de7a824f7832d4abe",
    ],
    "needed_by": [
      {
        "id": "another%2Frepo~master~I8944323ed34d55af7a17a48c8d8509f3cf62b6bf",
        "project": "zuul/repo1",
        "branch": "master",
        [...]
      }
    ],
    "cycle": false
  }
```

<a id="json-entities">JSON Entities
-----------------------------------

### <a id="crd-info"></a>CrdInfo

The `CrdInfo` entity shows zuul dependencies on a patch set.

|Field Name         |Description|
|:------------------|:----------|
|depends_on_found   |List of shallow [ChangeInfo](../../../Documentation/rest-api-changes.html#change-info) entities. One for each Change that is available on this server and this change depends on|
|depends_on_missing |List of Change-Ids. One for each change that is not available on this server although this change depends on|
|needed_by          |List of shallow [ChangeInfo](../../../Documentation/rest-api-changes.html#change-info) entities. One for each change that is dependent on this change|
|cycle              |Whether this change is in a circular dependency chain|


SEE ALSO
--------

* [Change related REST endpoints](../../../Documentation/rest-api-changes.html)
* [Plugin Development](../../../Documentation/dev-plugins.html)
* [REST API Development](../../../Documentation/dev-rest-api.html)

GERRIT
------
Part of [Gerrit Code Review](../../../Documentation/index.html)
