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

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.extensions.restapi.TopLevelResource;
import com.google.gerrit.server.change.RevisionResource;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.restapi.change.ChangesCollection;
import com.google.gerrit.server.restapi.change.QueryChanges;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jgit.errors.RepositoryNotFoundException;

@Singleton
public class GetCrd implements RestReadView<RevisionResource> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ChangesCollection changes;
  private final CommitMessageFetcher commitMessageFetcher;

  @Inject
  GetCrd(ChangesCollection changes, CommitMessageFetcher commitMessageFetcher) {
    this.changes = changes;
    this.commitMessageFetcher = commitMessageFetcher;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Response<CrdInfo> apply(RevisionResource rsrc)
      throws RepositoryNotFoundException, IOException, BadRequestException, AuthException,
          PermissionBackendException {
    CrdInfo out = new CrdInfo();
    out.dependsOn = new ArrayList<>();
    out.neededBy = new ArrayList<>();

    Change.Key thisId = rsrc.getChange().getKey();

    // get depends on info
    Project.NameKey p = rsrc.getChange().getProject();
    String rev = rsrc.getPatchSet().commitId().getName();
    String commitMsg = commitMessageFetcher.fetch(p, rev);
    Pattern pattern = Pattern.compile("[Dd]epends-[Oo]n:? (I[0-9a-f]{8,40})", Pattern.DOTALL);
    Matcher matcher = pattern.matcher(commitMsg);
    while (matcher.find()) {
      String otherId = matcher.group(1);
      logger.atFinest().log("Change %s depends on change %s", thisId, otherId);
      out.dependsOn.add(otherId);
    }

    // get needed by info
    QueryChanges query = changes.list();
    String neededByQuery = "message:" + thisId + " -change:" + thisId;
    query.addQuery(neededByQuery);
    Response<List<?>> response = query.apply(TopLevelResource.INSTANCE);
    List<ChangeInfo> changes = (List<ChangeInfo>) response.value();
    // check for dependency cycles
    for (ChangeInfo other : changes) {
      String otherId = other.changeId;
      logger.atFinest().log("Change %s needed by %s", thisId, otherId);
      if (out.dependsOn.contains(otherId)) {
        logger.atFiner().log(
            "Detected dependency cycle between changes %s and %s", thisId, otherId);
        out.cycle = true;
      }
      out.neededBy.add(otherId);
    }

    return Response.ok(out);
  }
}
