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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gerrit.entities.Change;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.TopLevelResource;
import com.google.gerrit.server.restapi.change.ChangesCollection;
import com.google.gerrit.server.restapi.change.QueryChanges;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class NeededByFetcherTest {
  private ChangesCollection changes;
  private Change.Key changeKey = getChangeKey(1);

  @Test
  public void testFetchForChangeKeyNoResults() throws Exception {
    List<ChangeInfo> searchResult = new ArrayList<>();

    configureMocks(searchResult);

    NeededByFetcher fetcher = createFetcher();

    List<String> neededBy = fetcher.fetchForChangeKey(changeKey);

    assertThat(neededBy).isEmpty();
  }

  @Test
  public void testFetchForChangeKeySingleResult() throws Exception {
    List<ChangeInfo> searchResult = new ArrayList<>();
    searchResult.add(getChangeInfo(2));

    configureMocks(searchResult);

    NeededByFetcher fetcher = createFetcher();

    List<String> neededBy = fetcher.fetchForChangeKey(changeKey);

    assertThat(neededBy).containsExactly(getChangeKey(2).toString());
  }

  @Test
  public void testFetchForChangeKeyMultipleResults() throws Exception {
    List<ChangeInfo> searchResult = new ArrayList<>();
    searchResult.add(getChangeInfo(2));
    searchResult.add(getChangeInfo(3));

    configureMocks(searchResult);

    NeededByFetcher fetcher = createFetcher();

    List<String> neededBy = fetcher.fetchForChangeKey(changeKey);

    assertThat(neededBy).containsExactly(getChangeKey(2).toString(), getChangeKey(3).toString());
  }

  public void configureMocks(final List<ChangeInfo> searchResult) throws Exception {
    QueryChanges queryChanges = mock(QueryChanges.class);
    final AtomicBoolean addedQuery = new AtomicBoolean(false);
    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                addedQuery.getAndSet(true);
                return null;
              }
            })
        .when(queryChanges)
        .addQuery("message:" + changeKey + " -change:" + changeKey);
    when(queryChanges.apply(TopLevelResource.INSTANCE))
        .thenAnswer(
            new Answer<Response<List<ChangeInfo>>>() {

              @Override
              public Response<List<ChangeInfo>> answer(InvocationOnMock invocation)
                  throws Throwable {
                return Response.ok(addedQuery.get() ? searchResult : null);
              }
            });

    changes = mock(ChangesCollection.class);
    when(changes.list()).thenReturn(queryChanges);
  }

  private Change.Key getChangeKey(int keyEnding) {
    return Change.key("I0123456789abcdef0000000000000000000" + (10000 + keyEnding));
  }

  private ChangeInfo getChangeInfo(int keyEnding) {
    ChangeInfo changeInfo = new ChangeInfo();
    changeInfo.changeId = getChangeKey(keyEnding).toString();
    return changeInfo;
  }

  private NeededByFetcher createFetcher() {
    return new NeededByFetcher(changes);
  }
}
