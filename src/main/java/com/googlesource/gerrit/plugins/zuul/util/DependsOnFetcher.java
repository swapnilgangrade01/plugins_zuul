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

import com.google.gerrit.entities.Project;
import com.google.gerrit.server.change.RevisionResource;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.List;
import org.eclipse.jgit.errors.RepositoryNotFoundException;

/** Fetches the Depends-On part of cross repository dependencies. */
public class DependsOnFetcher {
  private final CommitMessageFetcher commitMessageFetcher;
  private final DependsOnExtractor dependsOnExtractor;

  @Inject
  public DependsOnFetcher(
      CommitMessageFetcher commitMessageFetcher, DependsOnExtractor dependsOnExtractor) {
    this.commitMessageFetcher = commitMessageFetcher;
    this.dependsOnExtractor = dependsOnExtractor;
  }

  public List<String> fetchForRevision(RevisionResource rsrc)
      throws RepositoryNotFoundException, IOException {
    Project.NameKey p = rsrc.getChange().getProject();
    String rev = rsrc.getPatchSet().commitId().getName();
    String commitMsg = commitMessageFetcher.fetch(p, rev);
    return dependsOnExtractor.extract(commitMsg);
  }
}
