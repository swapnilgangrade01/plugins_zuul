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

import com.google.gerrit.entities.Change;
import com.google.gerrit.extensions.client.ListChangesOption;
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
  private final ChangesCollection changes;
  private final CommitMessageFetcher commitMessageFetcher;
  private final DependsOnExtractor dependsOnExtractor;

  @Inject
  public NeededByFetcher(
      ChangesCollection changes,
      CommitMessageFetcher commitMessageFetcher,
      DependsOnExtractor dependsOnExtractor) {
    this.changes = changes;
    this.commitMessageFetcher = commitMessageFetcher;
    this.dependsOnExtractor = dependsOnExtractor;
  }

  public List<ChangeInfo> fetchForChangeKey(Change.Key key)
      throws BadRequestException, AuthException, PermissionBackendException {
    // TODO Add support for URL based `Depends-On` references.
    String keyString = key.toString();
    List<ChangeInfo> neededBy = new ArrayList<>();

    QueryChanges query = changes.list();
    String neededByQuery = "message:" + keyString + " -change:" + keyString;
    query.addOption(ListChangesOption.CURRENT_REVISION);
    query.addOption(ListChangesOption.CURRENT_COMMIT);
    query.addQuery(neededByQuery);
    Response<List<?>> response = query.apply(TopLevelResource.INSTANCE);
    @SuppressWarnings("unchecked")
    List<ChangeInfo> changes = (List<ChangeInfo>) response.value();
    for (ChangeInfo changeInfo : changes) {
      // The search found the key somewhere in the commit message. But this need not be a
      // `Depends-On`. `key` might be mentioned for a completely different reason. So we need to
      // check if `key` occurs in a `Depends-On`.
      String commitMessage = commitMessageFetcher.fetch(changeInfo);
      List<String> dependencies = dependsOnExtractor.extract(commitMessage);
      if (dependencies.contains(keyString)) {
        neededBy.add(changeInfo);
      }
    }
    return neededBy;
  }
}
