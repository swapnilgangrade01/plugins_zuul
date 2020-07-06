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

import com.google.common.collect.Lists;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.TopLevelResource;
import com.google.gerrit.server.change.RevisionResource;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.restapi.change.ChangesCollection;
import com.google.gerrit.server.restapi.change.QueryChanges;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.errors.RepositoryNotFoundException;

/** Fetches the Depends-On part of cross repository dependencies. */
public class DependsOnFetcher {
  private final ChangesCollection changes;
  private final CommitMessageFetcher commitMessageFetcher;
  private final DependsOnExtractor dependsOnExtractor;

  @Inject
  public DependsOnFetcher(
      ChangesCollection changes,
      CommitMessageFetcher commitMessageFetcher,
      DependsOnExtractor dependsOnExtractor) {
    this.changes = changes;
    this.commitMessageFetcher = commitMessageFetcher;
    this.dependsOnExtractor = dependsOnExtractor;
  }

  @SuppressWarnings("unchecked")
  private List<ChangeInfo> fetchChangeInfosForChangeKeys(List<String> keys)
      throws BadRequestException, AuthException, PermissionBackendException {
    List<ChangeInfo> ret;
    if (keys.isEmpty()) {
      ret = new ArrayList<>();
    } else {
      QueryChanges query = changes.list();
      String queryString = "change:" + String.join(" OR change:", keys);
      query.addQuery(queryString);
      Response<List<?>> response = query.apply(TopLevelResource.INSTANCE);
      ret = (List<ChangeInfo>) response.value();
    }
    return ret;
  }

  public Pair<List<ChangeInfo>, List<String>> fetchForRevision(RevisionResource rsrc)
      throws RepositoryNotFoundException, IOException, BadRequestException, AuthException,
          PermissionBackendException {
    Project.NameKey p = rsrc.getChange().getProject();
    String rev = rsrc.getPatchSet().commitId().getName();
    String commitMsg = commitMessageFetcher.fetch(p, rev);

    List<String> extractedChangeKeys = dependsOnExtractor.extract(commitMsg);
    List<ChangeInfo> foundChangeInfos = fetchChangeInfosForChangeKeys(extractedChangeKeys);

    // `extracted` and `found` need not agree in size. It might be that a Change-Id from
    // `extracted` matches more than one Change (E.g.: cherry-picked to different branch). And it
    // might be that a Change-Id from `extracted` does not yield a result.
    // So we need to check that `found` holds at least one ChangeInfo for each Change-Id in
    // `extractedDependsOn`.

    List<String> resultMissing = Lists.newArrayList(extractedChangeKeys);
    List<ChangeInfo> resultFound = new ArrayList<>(extractedChangeKeys.size());

    for (ChangeInfo changeInfo : foundChangeInfos) {
      String changeId = changeInfo.changeId.toString();
      if (extractedChangeKeys.contains(changeId)) {
        resultMissing.remove(changeId);
        resultFound.add(changeInfo);
      }
    }
    return new ImmutablePair<>(resultFound, resultMissing);
  }
}
