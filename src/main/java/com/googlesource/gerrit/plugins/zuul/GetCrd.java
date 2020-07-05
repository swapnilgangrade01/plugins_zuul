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
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.change.RevisionResource;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.zuul.util.CommitMessageFetcher;
import com.googlesource.gerrit.plugins.zuul.util.NeededByFetcher;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jgit.errors.RepositoryNotFoundException;

@Singleton
public class GetCrd implements RestReadView<RevisionResource> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final CommitMessageFetcher commitMessageFetcher;
  private final NeededByFetcher neededByFetcher;

  @Inject
  GetCrd(CommitMessageFetcher commitMessageFetcher, NeededByFetcher neededByFetcher) {
    this.commitMessageFetcher = commitMessageFetcher;
    this.neededByFetcher = neededByFetcher;
  }

  @Override
  public Response<CrdInfo> apply(RevisionResource rsrc)
      throws RepositoryNotFoundException, IOException, BadRequestException, AuthException,
          PermissionBackendException {
    CrdInfo out = new CrdInfo();
    Change.Key thisId = rsrc.getChange().getKey();

    // get depends on info
    out.dependsOn = new ArrayList<>();
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

    out.neededBy = neededByFetcher.fetchForChangeKey(thisId);

    out.cycle = false;
    for (String neededKey : out.neededBy) {
      if (out.dependsOn.contains(neededKey)) {
        out.cycle = true;
        break;
      }
    }
    return Response.ok(out);
  }
}
