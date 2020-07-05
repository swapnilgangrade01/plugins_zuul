// Copyright (C) 2020 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.googlesource.gerrit.plugins.zuul.util;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Change;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.TopLevelResource;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.restapi.change.ChangesCollection;
import com.google.gerrit.server.restapi.change.QueryChanges;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/** Fetches the Needed-By part of cross repository dependencies. */
public class NeededByFetcher {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ChangesCollection changes;

  @Inject
  public NeededByFetcher(ChangesCollection changes) {
    this.changes = changes;
  }

  public List<String> fetchForChangeKey(Change.Key key)
      throws BadRequestException, AuthException, PermissionBackendException {
    List<String> neededBy = new ArrayList<>();

    QueryChanges query = changes.list();
    String neededByQuery = "message:" + key + " -change:" + key;
    query.addQuery(neededByQuery);
    Response<List<?>> response = query.apply(TopLevelResource.INSTANCE);
    @SuppressWarnings("unchecked")
    List<ChangeInfo> changes = (List<ChangeInfo>) response.value();
    for (ChangeInfo other : changes) {
      String otherKey = other.changeId;
      logger.atFinest().log("Change %s needed by %s", key, otherKey);
      neededBy.add(otherKey);
    }
    return neededBy;
  }
}
