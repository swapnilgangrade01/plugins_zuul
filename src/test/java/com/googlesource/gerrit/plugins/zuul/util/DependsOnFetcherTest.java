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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.PatchSet;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.change.RevisionResource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Test;

public class DependsOnFetcherTest {
  private CommitMessageFetcher commitMessageFetcher;
  private DependsOnExtractor dependsOnExtractor;
  private RevisionResource rsrc;

  @Test
  public void testExtractNoDependencies() throws Exception {
    configureMocks(new ArrayList<>());

    DependsOnFetcher fetcher = createFetcher();
    List<String> dependsOn = fetcher.fetchForRevision(rsrc);

    assertThat(dependsOn).isEmpty();
  }

  @Test
  public void testExtractSingleDependency() throws Exception {
    List<String> extracted = new ArrayList<>();
    extracted.add("I00000001");
    configureMocks(extracted);

    DependsOnFetcher fetcher = createFetcher();
    List<String> dependsOn = fetcher.fetchForRevision(rsrc);

    assertThat(dependsOn).containsExactly("I00000001");
  }

  @Test
  public void testExtractMultipleDependencies() throws Exception {
    List<String> extracted = new ArrayList<>();
    extracted.add("I00000001");
    extracted.add("I00000002");
    extracted.add("I00000003");
    configureMocks(extracted);

    DependsOnFetcher fetcher = createFetcher();
    List<String> dependsOn = fetcher.fetchForRevision(rsrc);

    assertThat(dependsOn).containsExactly("I00000001", "I00000002", "I00000003");
  }

  private void configureMocks(List<String> dependsOn)
      throws RepositoryNotFoundException, IOException {
    String commitId = "0123456789012345678901234567890123456789";

    Project.NameKey projectNameKey = Project.nameKey("projectFoo");

    PatchSet patchSet = mock(PatchSet.class);
    when(patchSet.commitId()).thenReturn(ObjectId.fromString(commitId));

    BranchNameKey branchNameKey = BranchNameKey.create(projectNameKey, "branchBar");

    Change.Key changeKey = Change.key("I0123456789");
    Change change = new Change(changeKey, null, null, branchNameKey, null);

    rsrc = mock(RevisionResource.class);
    when(rsrc.getChange()).thenReturn(change);
    when(rsrc.getPatchSet()).thenReturn(patchSet);

    commitMessageFetcher = mock(CommitMessageFetcher.class);
    when(commitMessageFetcher.fetch(projectNameKey, commitId)).thenReturn("commitMsgFoo");

    dependsOnExtractor = mock(DependsOnExtractor.class);
    when(dependsOnExtractor.extract("commitMsgFoo")).thenReturn(dependsOn);
  }

  private DependsOnFetcher createFetcher() {
    return new DependsOnFetcher(commitMessageFetcher, dependsOnExtractor);
  }
}
