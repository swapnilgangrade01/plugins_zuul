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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.PatchSet;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class DependsOnFetcherTest {
  private ChangesCollection changes;
  private CommitMessageFetcher commitMessageFetcher;
  private DependsOnExtractor dependsOnExtractor;
  private RevisionResource rsrc;
  private Map<Integer, ChangeInfo> changeInfos = new HashMap<>();

  @Test
  public void testExtractNoDependencies() throws Exception {
    configureMocks(new ArrayList<>(), new ArrayList<>());

    DependsOnFetcher fetcher = createFetcher();
    Pair<List<ChangeInfo>, List<String>> dependsOn = fetcher.fetchForRevision(rsrc);

    assertThat(dependsOn.getLeft()).isEmpty();
    assertThat(dependsOn.getRight()).isEmpty();
  }

  @Test
  public void testExtractSingleFoundDependency() throws Exception {
    List<String> extracted = new ArrayList<>();
    extracted.add(getChangeKey(1));

    List<ChangeInfo> searchResult = new ArrayList<>();
    searchResult.add(getChangeInfo(1));
    configureMocks(extracted, searchResult);

    DependsOnFetcher fetcher = createFetcher();
    Pair<List<ChangeInfo>, List<String>> dependsOn = fetcher.fetchForRevision(rsrc);

    assertThat(dependsOn.getLeft()).containsExactly(getChangeInfo(1));
    assertThat(dependsOn.getRight()).isEmpty();
  }

  @Test
  public void testExtractMultipleFoundDependencies() throws Exception {
    List<String> extracted = new ArrayList<>();
    extracted.add(getChangeKey(1));
    extracted.add(getChangeKey(2));
    extracted.add(getChangeKey(3));
    List<ChangeInfo> searchResult = new ArrayList<>();
    searchResult.add(getChangeInfo(1));
    searchResult.add(getChangeInfo(2));
    searchResult.add(getChangeInfo(3));
    configureMocks(extracted, searchResult);

    DependsOnFetcher fetcher = createFetcher();
    Pair<List<ChangeInfo>, List<String>> dependsOn = fetcher.fetchForRevision(rsrc);

    assertThat(dependsOn.getLeft())
        .containsExactly(getChangeInfo(1), getChangeInfo(2), getChangeInfo(3));
    assertThat(dependsOn.getRight()).isEmpty();
  }

  @Test
  public void testExtractSingleMissingDependency() throws Exception {
    List<String> extracted = new ArrayList<>();
    extracted.add(getChangeKey(1));

    configureMocks(extracted, new ArrayList<>());

    DependsOnFetcher fetcher = createFetcher();
    Pair<List<ChangeInfo>, List<String>> dependsOn = fetcher.fetchForRevision(rsrc);

    assertThat(dependsOn.getLeft()).isEmpty();
    assertThat(dependsOn.getRight()).containsExactly(getChangeKey(1));
  }

  @Test
  public void testExtractMultipleMissingDependencies() throws Exception {
    List<String> extracted = new ArrayList<>();
    extracted.add(getChangeKey(1));
    extracted.add(getChangeKey(2));
    extracted.add(getChangeKey(3));
    configureMocks(extracted, new ArrayList<>());

    DependsOnFetcher fetcher = createFetcher();
    Pair<List<ChangeInfo>, List<String>> dependsOn = fetcher.fetchForRevision(rsrc);

    assertThat(dependsOn.getLeft()).isEmpty();
    assertThat(dependsOn.getRight())
        .containsExactly(getChangeKey(1), getChangeKey(2), getChangeKey(3));
  }

  @Test
  public void testExtractMultipleDependenciesMultipleResultsForChangeId() throws Exception {
    List<String> extracted = new ArrayList<>();
    extracted.add(getChangeKey(1));
    extracted.add(getChangeKey(2));
    extracted.add(getChangeKey(3));

    List<ChangeInfo> searchResult = new ArrayList<>();
    searchResult.add(getChangeInfo(1));
    searchResult.add(getChangeInfo(2));
    searchResult.add(getChangeInfo(3));

    ChangeInfo changeInfo = getChangeInfo(102);
    changeInfo.changeId = getChangeInfo(2).changeId;
    searchResult.add(changeInfo);

    configureMocks(extracted, searchResult);

    DependsOnFetcher fetcher = createFetcher();
    Pair<List<ChangeInfo>, List<String>> dependsOn = fetcher.fetchForRevision(rsrc);

    assertThat(dependsOn.getLeft())
        .containsExactly(getChangeInfo(1), getChangeInfo(2), getChangeInfo(3), getChangeInfo(102));
    assertThat(dependsOn.getRight()).isEmpty();
  }

  @Test
  public void testExtractMixed() throws Exception {
    List<String> extracted = new ArrayList<>();
    extracted.add(getChangeKey(1));
    extracted.add(getChangeKey(2));
    extracted.add(getChangeKey(3));
    extracted.add(getChangeKey(4));

    List<ChangeInfo> searchResult = new ArrayList<>();
    searchResult.add(getChangeInfo(2));
    searchResult.add(getChangeInfo(3));

    ChangeInfo changeInfo = getChangeInfo(102);
    changeInfo.changeId = getChangeInfo(2).changeId;
    searchResult.add(changeInfo);

    configureMocks(extracted, searchResult);

    DependsOnFetcher fetcher = createFetcher();
    Pair<List<ChangeInfo>, List<String>> dependsOn = fetcher.fetchForRevision(rsrc);

    assertThat(dependsOn.getLeft())
        .containsExactly(getChangeInfo(2), getChangeInfo(102), getChangeInfo(3));
    assertThat(dependsOn.getRight()).containsExactly(getChangeKey(1), getChangeKey(4));
  }

  private void configureMocks(
      List<String> extractedDependsOn, List<ChangeInfo> searchResultDependsOn)
      throws RepositoryNotFoundException, IOException, BadRequestException, AuthException,
          PermissionBackendException {
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
    when(dependsOnExtractor.extract("commitMsgFoo")).thenReturn(extractedDependsOn);

    QueryChanges queryChanges = mock(QueryChanges.class);
    final AtomicBoolean addedQuery = new AtomicBoolean(false);

    if (!extractedDependsOn.isEmpty()) {
      doAnswer(
              new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                  if (addedQuery.getAndSet(true)) {
                    fail("flag has already been set");
                  }
                  return null;
                }
              })
          .when(queryChanges)
          .addQuery("change:" + String.join(" OR change:", extractedDependsOn));

      when(queryChanges.apply(TopLevelResource.INSTANCE))
          .thenAnswer(
              new Answer<Response<List<ChangeInfo>>>() {

                @Override
                public Response<List<ChangeInfo>> answer(InvocationOnMock invocation)
                    throws Throwable {
                  if (!addedQuery.get()) {
                    fail("executed query before all options were set");
                  }
                  return Response.ok(searchResultDependsOn);
                }
              });
    }

    changes = mock(ChangesCollection.class);
    when(changes.list()).thenReturn(queryChanges);
  }

  private String getChangeKey(int keyEnding) {
    return "I0123456789abcdef0000000000000000000" + (10000 + keyEnding);
  }

  private ChangeInfo getChangeInfo(int keyEnding) {
    return changeInfos.computeIfAbsent(
        keyEnding,
        neededKeyEnding -> {
          ChangeInfo changeInfo = new ChangeInfo();
          changeInfo.changeId = getChangeKey(neededKeyEnding);
          changeInfo._number = neededKeyEnding;
          return changeInfo;
        });
  }

  private DependsOnFetcher createFetcher() {
    return new DependsOnFetcher(changes, commitMessageFetcher, dependsOnExtractor);
  }
}
