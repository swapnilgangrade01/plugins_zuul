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

package com.googlesource.gerrit.plugins.chound;

import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.extensions.restapi.TopLevelResource;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.change.ChangesCollection;
import com.google.gerrit.server.change.RevisionResource;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.query.change.QueryChanges;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class GetDependency implements RestReadView<RevisionResource> {
  private final ChangesCollection changes;
  private final GitRepositoryManager repoManager;

  @Inject
  GetDependency(ChangesCollection changes, GitRepositoryManager repoManager) {
    this.changes = changes;
    this.repoManager = repoManager;
  }

  @Override
  @SuppressWarnings("unchecked")
  public DependencyInfo apply(RevisionResource rsrc)
      throws RepositoryNotFoundException, IOException, BadRequestException,
      AuthException, OrmException {

    DependencyInfo out = new DependencyInfo();
    out.dependsOn = new ArrayList<>();
    out.neededBy = new ArrayList<>();

    // get depends on info
    Project.NameKey p = rsrc.getChange().getProject();
    try (Repository repo = repoManager.openRepository(p);
        RevWalk rw = new RevWalk(repo)) {
      String rev = rsrc.getPatchSet().getRevision().get();
      RevCommit commit = rw.parseCommit(ObjectId.fromString(rev));
      String commitMsg = commit.getFullMessage();
      Pattern pattern = Pattern.compile("[Dd]epends-[Oo]n:? (I[0-9a-f]{8,40})",
          Pattern.DOTALL);
      Matcher matcher = pattern.matcher(commitMsg);
      while (matcher.find()) {
        out.dependsOn.add(matcher.group(1));
      }
    }

    // get needed by info
    Change.Key chgKey = rsrc.getChange().getKey();
    QueryChanges query = changes.list();
    String neededByQuery = "message:" + chgKey + " -change:" + chgKey;
    query.addQuery(neededByQuery);
    List<ChangeInfo> changes =
        (List<ChangeInfo>) query.apply(TopLevelResource.INSTANCE);
    // check for dependency cycles
    for (ChangeInfo change : changes) {
      if (out.dependsOn.contains(change.changeId)) {
        out.cycle = true;
      }
      out.neededBy.add(change.changeId);
    }

    return out;
  }
}
