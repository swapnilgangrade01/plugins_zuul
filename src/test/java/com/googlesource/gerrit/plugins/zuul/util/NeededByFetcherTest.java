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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gerrit.entities.Change;
import com.google.gerrit.extensions.client.ListChangesOption;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.TopLevelResource;
import com.google.gerrit.server.restapi.change.ChangesCollection;
import com.google.gerrit.server.restapi.change.QueryChanges;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class NeededByFetcherTest {
  private ChangesCollection changes;
  private CommitMessageFetcher commitMessageFetcher;
  private DependsOnExtractor dependsOnExtractor;
  private Change.Key changeKey = getChangeKey(1);
  private Map<Integer, ChangeInfo> changeInfos = new HashMap<>();

  @Test
  public void testFetchForChangeKeyNoResults() throws Exception {
    List<ChangeInfo> searchResult = new ArrayList<>();

    configureMocks(searchResult);

    NeededByFetcher fetcher = createFetcher();

    List<ChangeInfo> neededBy = fetcher.fetchForChangeKey(changeKey);

    assertThat(neededBy).isEmpty();
  }

  @Test
  public void testFetchForChangeKeySingleResult() throws Exception {
    List<ChangeInfo> searchResult = new ArrayList<>();
    searchResult.add(getChangeInfo(2));

    configureMocks(searchResult);

    NeededByFetcher fetcher = createFetcher();

    List<ChangeInfo> neededBy = fetcher.fetchForChangeKey(changeKey);

    assertThat(neededBy).containsExactly(getChangeInfo(2));
  }

  @Test
  public void testFetchForChangeKeySingleResultUnmatchedEmpty() throws Exception {
    List<ChangeInfo> searchResult = new ArrayList<>();
    searchResult.add(getChangeInfo(2));

    configureMocks(searchResult);
    when(dependsOnExtractor.extract("commitMessage2")).thenReturn(new ArrayList<>());

    NeededByFetcher fetcher = createFetcher();

    List<ChangeInfo> neededBy = fetcher.fetchForChangeKey(changeKey);

    assertThat(neededBy).isEmpty();
  }

  @Test
  public void testFetchForChangeKeySingleResultUnmatchedDifferent() throws Exception {
    List<ChangeInfo> searchResult = new ArrayList<>();
    searchResult.add(getChangeInfo(2));

    configureMocks(searchResult);
    List<String> extracted = new ArrayList<>();
    extracted.add(getChangeKey(3).toString());
    when(dependsOnExtractor.extract("commitMessage2")).thenReturn(extracted);

    NeededByFetcher fetcher = createFetcher();

    List<ChangeInfo> neededBy = fetcher.fetchForChangeKey(changeKey);

    assertThat(neededBy).isEmpty();
  }

  @Test
  public void testFetchForChangeKeyMultipleResults() throws Exception {
    List<ChangeInfo> searchResult = new ArrayList<>();
    searchResult.add(getChangeInfo(2));
    searchResult.add(getChangeInfo(3));

    configureMocks(searchResult);

    NeededByFetcher fetcher = createFetcher();

    List<ChangeInfo> neededBy = fetcher.fetchForChangeKey(changeKey);

    assertThat(neededBy).containsExactly(getChangeInfo(2), getChangeInfo(3));
  }

  @Test
  public void testFetchForChangeKeyMultipleResultsSomeUnmatched() throws Exception {
    List<ChangeInfo> searchResult = new ArrayList<>();
    searchResult.add(getChangeInfo(2));
    searchResult.add(getChangeInfo(3));
    searchResult.add(getChangeInfo(4));
    searchResult.add(getChangeInfo(5));

    configureMocks(searchResult);
    when(dependsOnExtractor.extract("commitMessage3")).thenReturn(new ArrayList<>());
    when(dependsOnExtractor.extract("commitMessage4")).thenReturn(new ArrayList<>());

    NeededByFetcher fetcher = createFetcher();

    List<ChangeInfo> neededBy = fetcher.fetchForChangeKey(changeKey);

    assertThat(neededBy).containsExactly(getChangeInfo(2), getChangeInfo(5));
  }

  /**
   * Sets up mocks for a given search result.
   *
   * <p>Each search result is configured to have a `Depends-On` to the changeKey per default. To
   * refine, override the extraction for ("commitMessage" + number) to the desired list of
   * Change-Ids.
   *
   * @param searchResult The search result to configure.
   * @throws Exception thrown upon issues.
   */
  public void configureMocks(final List<ChangeInfo> searchResult) throws Exception {
    QueryChanges queryChanges = mock(QueryChanges.class);
    final AtomicBoolean addedQuery = new AtomicBoolean(false);
    final AtomicBoolean addedOptionCurrentRevision = new AtomicBoolean(false);
    final AtomicBoolean addedOptionCurrentCommit = new AtomicBoolean(false);

    mockQueryChangesWithSwitch(queryChanges, addedOptionCurrentCommit)
        .addOption(ListChangesOption.CURRENT_COMMIT);
    mockQueryChangesWithSwitch(queryChanges, addedOptionCurrentRevision)
        .addOption(ListChangesOption.CURRENT_REVISION);
    mockQueryChangesWithSwitch(queryChanges, addedQuery)
        .addQuery("message:" + changeKey + " -change:" + changeKey);
    when(queryChanges.apply(TopLevelResource.INSTANCE))
        .thenAnswer(
            new Answer<Response<List<ChangeInfo>>>() {

              @Override
              public Response<List<ChangeInfo>> answer(InvocationOnMock invocation)
                  throws Throwable {
                boolean ready =
                    addedOptionCurrentRevision.get()
                        && addedOptionCurrentCommit.get()
                        && addedQuery.get();
                if (!ready) {
                  fail("executed query before all options were set");
                }
                return Response.ok(searchResult);
              }
            });

    changes = mock(ChangesCollection.class);
    when(changes.list()).thenReturn(queryChanges);

    commitMessageFetcher = mock(CommitMessageFetcher.class);
    when(commitMessageFetcher.fetch(any(ChangeInfo.class)))
        .thenAnswer(
            new Answer<String>() {
              @Override
              public String answer(InvocationOnMock invocation) throws Throwable {
                ChangeInfo changeInfo = invocation.getArgument(0);
                return "commitMessage" + changeInfo._number;
              }
            });

    dependsOnExtractor = mock(DependsOnExtractor.class);
    List<String> extractedMatchList = new ArrayList<>();
    extractedMatchList.add(changeKey.toString());
    when(dependsOnExtractor.extract(any(String.class))).thenReturn(extractedMatchList);
  }

  private QueryChanges mockQueryChangesWithSwitch(
      QueryChanges queryChanges, AtomicBoolean booleanSwitch) {
    return doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                if (booleanSwitch.getAndSet(true)) {
                  fail("flag has already been set");
                }
                return null;
              }
            })
        .when(queryChanges);
  }

  private Change.Key getChangeKey(int keyEnding) {
    return Change.key("I0123456789abcdef0000000000000000000" + (10000 + keyEnding));
  }

  private ChangeInfo getChangeInfo(int keyEnding) {
    return changeInfos.computeIfAbsent(
        keyEnding,
        neededKeyEnding -> {
          ChangeInfo changeInfo = new ChangeInfo();
          changeInfo.changeId = getChangeKey(neededKeyEnding).toString();
          changeInfo._number = neededKeyEnding;
          return changeInfo;
        });
  }

  private NeededByFetcher createFetcher() {
    return new NeededByFetcher(changes, commitMessageFetcher, dependsOnExtractor);
  }
}
