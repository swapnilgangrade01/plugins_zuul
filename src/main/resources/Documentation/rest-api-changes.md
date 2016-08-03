@PLUGIN@ - /changes/ REST API
==============================

This page describes the '/changes/' REST endpoints that are added by
the @PLUGIN@ plugin.

Please also take note of the general information on the
[REST API](../../../Documentation/rest-api.html).

<a id="plugin-endpoints"> @PLUGIN@ Endpoints
--------------------------------------------

### <a id="get-dependency"> Get Dependency

__GET__ /changes/{change-id}/revisions/{revision-id}/@PLUGIN@~dependency

Gets the zuul [dependency](#dependency-info) for a change.  Please refer to the
general [changes rest api](../../../Documentation/rest-api-changes.html#get-review)
for additional info on this request.

#### Request

```
  GET /changes/myProject~master~I8473b95934b5732ac55d26311a706c9c2bde9940/revisions/674ac754f91e64a0efb8087e59a176484bd534d1/@PLUGIN@~dependency HTTP/1.0
```

#### Response

```
  HTTP/1.1 200 OK
  Content-Disposition: attachment
  Content-Type: application/json; charset=UTF-8

  )]}'
  {
    "depends_on": [
      "Ic79ed94daa9b58527139aadba1b0d59d1f54754b",
      "I66853bf0c18e60f8de14d44dfb7c2ca1c3793111"
    ],
    "needed_by": [
      "I66853bf0c18e60f8de14d44dfb7c2ca1c379311d"
    ],
    "cycle": false
  }
```

<a id="json-entities">JSON Entities
-----------------------------------

### <a id="dependency-info"></a>DependencyInfo

The `DependencyInfo` entity shows zuul dependencies on a patch set.

|Field Name |Description|
|:----------|:----------|
|depends_on |List of changes that this change depends on|
|needed-by  |List of changes that is dependent on this change|
|cycle      |Whether this change is in a circular dependency chain|


SEE ALSO
--------

* [Change related REST endpoints](../../../Documentation/rest-api-changes.html)
* [Plugin Development](../../../Documentation/dev-plugins.html)
* [REST API Development](../../../Documentation/dev-rest-api.html)

GERRIT
------
Part of [Gerrit Code Review](../../../Documentation/index.html)
