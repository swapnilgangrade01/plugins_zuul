// Copyright (C) 2015 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.zuul;

import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.change.RevisionResource;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.zuul.util.DependsOnFetcher;
import com.googlesource.gerrit.plugins.zuul.util.NeededByFetcher;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.errors.RepositoryNotFoundException;

@Singleton
public class GetCrd implements RestReadView<RevisionResource> {
  private final DependsOnFetcher dependsOnFetcher;
  private final NeededByFetcher neededByFetcher;

  @Inject
  GetCrd(DependsOnFetcher dependsOnFetcher, NeededByFetcher neededByFetcher) {
    this.dependsOnFetcher = dependsOnFetcher;
    this.neededByFetcher = neededByFetcher;
  }

  @Override
  public Response<CrdInfo> apply(RevisionResource rsrc)
      throws RepositoryNotFoundException, IOException, BadRequestException, AuthException,
          PermissionBackendException {
    CrdInfo out = new CrdInfo();
    Pair<List<ChangeInfo>, List<String>> dependsOn = dependsOnFetcher.fetchForRevision(rsrc);
    out.dependsOnFound = dependsOn.getLeft();
    out.dependsOnMissing = dependsOn.getRight();

    out.neededBy = neededByFetcher.fetchForChangeKey(rsrc.getChange().getKey());

    List<String> dependsOnAllKeys = new ArrayList<>(out.dependsOnMissing);
    dependsOnAllKeys.addAll(
        out.dependsOnFound.stream()
            .map(changeInfo -> changeInfo.changeId)
            .collect(Collectors.toList()));

    out.cycle = false;
    for (ChangeInfo changeInfo : out.neededBy) {
      if (dependsOnAllKeys.contains(changeInfo.changeId)) {
        out.cycle = true;
        break;
      }
    }
    return Response.ok(out);
  }
}
