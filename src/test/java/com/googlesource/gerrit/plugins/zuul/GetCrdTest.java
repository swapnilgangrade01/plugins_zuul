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
package com.googlesource.gerrit.plugins.zuul;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gerrit.entities.Account;
import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.PatchSet;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.server.change.RevisionResource;
import com.googlesource.gerrit.plugins.zuul.util.CommitMessageFetcher;
import com.googlesource.gerrit.plugins.zuul.util.NeededByFetcher;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Test;

public class GetCrdTest {
  private NeededByFetcher neededByFetcher;
  private CommitMessageFetcher commitMessageFetcher;
  private RevisionResource rsrc;

  @Test
  public void testNoDependencies() throws Exception {
    String commitMessage = "subject";
    configureMocks(commitMessage, new ArrayList<>());

    GetCrd getCrd = createGetCrd();
    Response<CrdInfo> response = getCrd.apply(rsrc);

    assertThat(response.statusCode()).isEqualTo(200);
    CrdInfo crdInfo = response.value();
    assertThat(crdInfo.dependsOn).isEmpty();
    assertThat(crdInfo.neededBy).isEmpty();
    assertThat(crdInfo.cycle).isFalse();
  }

  @Test
  public void testSingleDependsOn() throws Exception {
    String commitMessage = "subject\nDepends-On: I00000000";
    configureMocks(commitMessage, new ArrayList<>());

    GetCrd getCrd = createGetCrd();
    Response<CrdInfo> response = getCrd.apply(rsrc);

    assertThat(response.statusCode()).isEqualTo(200);
    CrdInfo crdInfo = response.value();
    assertThat(crdInfo.dependsOn).containsExactly("I00000000");
    assertThat(crdInfo.neededBy).isEmpty();
    assertThat(crdInfo.cycle).isFalse();
  }

  @Test
  public void testMultipleDependsOn() throws Exception {
    String commitMessage =
        "subject\nDepends-On: I00000000\nDepends-On: I00000002\nDepends-On: I00000004";
    configureMocks(commitMessage, new ArrayList<>());

    GetCrd getCrd = createGetCrd();
    Response<CrdInfo> response = getCrd.apply(rsrc);

    assertThat(response.statusCode()).isEqualTo(200);
    CrdInfo crdInfo = response.value();
    assertThat(crdInfo.dependsOn).containsExactly("I00000000", "I00000002", "I00000004");
    assertThat(crdInfo.neededBy).isEmpty();
    assertThat(crdInfo.cycle).isFalse();
  }

  @Test
  public void testSingleNeededBy() throws Exception {
    String commitMessage = "subject";
    List<String> neededBy = new ArrayList<>();
    neededBy.add("I00000001");
    configureMocks(commitMessage, neededBy);

    GetCrd getCrd = createGetCrd();
    Response<CrdInfo> response = getCrd.apply(rsrc);

    assertThat(response.statusCode()).isEqualTo(200);
    CrdInfo crdInfo = response.value();
    assertThat(crdInfo.dependsOn).isEmpty();
    assertThat(crdInfo.neededBy).containsExactly("I00000001");
    assertThat(crdInfo.cycle).isFalse();
  }

  @Test
  public void testMultipleNeededBy() throws Exception {
    String commitMessage = "subject";
    List<String> neededBy = new ArrayList<>();
    neededBy.add("I00000001");
    neededBy.add("I00000003");
    neededBy.add("I00000005");
    configureMocks(commitMessage, neededBy);

    GetCrd getCrd = createGetCrd();
    Response<CrdInfo> response = getCrd.apply(rsrc);

    assertThat(response.statusCode()).isEqualTo(200);
    CrdInfo crdInfo = response.value();
    assertThat(crdInfo.dependsOn).isEmpty();
    assertThat(crdInfo.neededBy).containsExactly("I00000001", "I00000003", "I00000005");
    assertThat(crdInfo.cycle).isFalse();
  }

  @Test
  public void testMixed() throws Exception {
    String commitMessage = "subject\nDepends-On: I00000002\nDepends-On: I00000004";
    List<String> neededBy = new ArrayList<>();
    neededBy.add("I00000001");
    neededBy.add("I00000003");
    configureMocks(commitMessage, neededBy);

    GetCrd getCrd = createGetCrd();
    Response<CrdInfo> response = getCrd.apply(rsrc);

    assertThat(response.statusCode()).isEqualTo(200);
    CrdInfo crdInfo = response.value();
    assertThat(crdInfo.dependsOn).containsExactly("I00000002", "I00000004");
    assertThat(crdInfo.neededBy).containsExactly("I00000001", "I00000003");
    assertThat(crdInfo.cycle).isFalse();
  }

  @Test
  public void testSimpleCycle() throws Exception {
    String commitMessage = "subject\nDepends-On: I00000001";
    List<String> neededBy = new ArrayList<>();
    neededBy.add("I00000001");
    configureMocks(commitMessage, neededBy);

    GetCrd getCrd = createGetCrd();
    Response<CrdInfo> response = getCrd.apply(rsrc);

    assertThat(response.statusCode()).isEqualTo(200);
    CrdInfo crdInfo = response.value();
    assertThat(crdInfo.dependsOn).containsExactly("I00000001");
    assertThat(crdInfo.neededBy).containsExactly("I00000001");
    assertThat(crdInfo.cycle).isTrue();
  }

  public void configureMocks(String commitMessage, final List<String> neededBy) throws Exception {
    String commitId = "0123456789012345678901234567890123456789";

    Project.NameKey projectNameKey = Project.nameKey("projectFoo");

    PatchSet patchSet = mock(PatchSet.class);
    when(patchSet.commitId()).thenReturn(ObjectId.fromString(commitId));

    Change.Key changeKey = Change.key("I0123456789");

    Change change =
        new Change(
            changeKey,
            Change.id(4711),
            Account.id(23),
            BranchNameKey.create(projectNameKey, "branchBar"),
            new Timestamp(0));

    rsrc = mock(RevisionResource.class);
    when(rsrc.getChange()).thenReturn(change);
    when(rsrc.getPatchSet()).thenReturn(patchSet);

    commitMessageFetcher = mock(CommitMessageFetcher.class);
    when(commitMessageFetcher.fetch(projectNameKey, commitId)).thenReturn(commitMessage);

    neededByFetcher = mock(NeededByFetcher.class);
    when(neededByFetcher.fetchForChangeKey(changeKey)).thenReturn(neededBy);
  }

  private GetCrd createGetCrd() {
    return new GetCrd(commitMessageFetcher, neededByFetcher);
  }
}
