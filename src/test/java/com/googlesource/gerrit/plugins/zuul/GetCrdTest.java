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

import com.google.gerrit.entities.Change;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.server.change.RevisionResource;
import com.googlesource.gerrit.plugins.zuul.util.DependsOnFetcher;
import com.googlesource.gerrit.plugins.zuul.util.NeededByFetcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;

public class GetCrdTest {
  private RevisionResource rsrc;
  private DependsOnFetcher dependsOnFetcher;
  private NeededByFetcher neededByFetcher;
  private Map<Integer, ChangeInfo> changeInfos = new HashMap<>();

  @Test
  public void testNoDependencies() throws Exception {
    configureMocks(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

    GetCrd getCrd = createGetCrd();
    Response<CrdInfo> response = getCrd.apply(rsrc);

    assertThat(response.statusCode()).isEqualTo(200);
    CrdInfo crdInfo = response.value();
    assertThat(crdInfo.dependsOnFound).isEmpty();
    assertThat(crdInfo.dependsOnMissing).isEmpty();
    assertThat(crdInfo.neededBy).isEmpty();
    assertThat(crdInfo.cycle).isFalse();
  }

  @Test
  public void testSingleFoundDependsOn() throws Exception {
    ArrayList<ChangeInfo> dependsOnFound = new ArrayList<>();
    dependsOnFound.add(getChangeInfo(0));

    configureMocks(dependsOnFound, new ArrayList<>(), new ArrayList<>());

    GetCrd getCrd = createGetCrd();
    Response<CrdInfo> response = getCrd.apply(rsrc);

    assertThat(response.statusCode()).isEqualTo(200);
    CrdInfo crdInfo = response.value();
    assertThat(crdInfo.dependsOnFound).containsExactly(getChangeInfo(0));
    assertThat(crdInfo.dependsOnMissing).isEmpty();
    assertThat(crdInfo.neededBy).isEmpty();
    assertThat(crdInfo.cycle).isFalse();
  }

  @Test
  public void testSingleMissingDependsOn() throws Exception {
    ArrayList<String> dependsOnMissing = new ArrayList<>();
    dependsOnMissing.add(getChangeKey(0));

    configureMocks(new ArrayList<>(), dependsOnMissing, new ArrayList<>());

    GetCrd getCrd = createGetCrd();
    Response<CrdInfo> response = getCrd.apply(rsrc);

    assertThat(response.statusCode()).isEqualTo(200);
    CrdInfo crdInfo = response.value();
    assertThat(crdInfo.dependsOnFound).isEmpty();
    assertThat(crdInfo.dependsOnMissing).containsExactly(getChangeKey(0));
    assertThat(crdInfo.neededBy).isEmpty();
    assertThat(crdInfo.cycle).isFalse();
  }

  @Test
  public void testMultipleFoundDependsOn() throws Exception {
    ArrayList<ChangeInfo> dependsOnFound = new ArrayList<>();
    dependsOnFound.add(getChangeInfo(0));
    dependsOnFound.add(getChangeInfo(2));
    dependsOnFound.add(getChangeInfo(4));

    configureMocks(dependsOnFound, new ArrayList<>(), new ArrayList<>());

    GetCrd getCrd = createGetCrd();
    Response<CrdInfo> response = getCrd.apply(rsrc);

    assertThat(response.statusCode()).isEqualTo(200);
    CrdInfo crdInfo = response.value();
    assertThat(crdInfo.dependsOnFound)
        .containsExactly(getChangeInfo(0), getChangeInfo(2), getChangeInfo(4));
    assertThat(crdInfo.dependsOnMissing).isEmpty();
    assertThat(crdInfo.neededBy).isEmpty();
    assertThat(crdInfo.cycle).isFalse();
  }

  @Test
  public void testMultipleMissingDependsOn() throws Exception {
    ArrayList<String> dependsOnMissing = new ArrayList<>();
    dependsOnMissing.add(getChangeKey(0));
    dependsOnMissing.add(getChangeKey(2));
    dependsOnMissing.add(getChangeKey(4));

    configureMocks(new ArrayList<>(), dependsOnMissing, new ArrayList<>());

    GetCrd getCrd = createGetCrd();
    Response<CrdInfo> response = getCrd.apply(rsrc);

    assertThat(response.statusCode()).isEqualTo(200);
    CrdInfo crdInfo = response.value();
    assertThat(crdInfo.dependsOnFound).isEmpty();
    assertThat(crdInfo.dependsOnMissing)
        .containsExactly(getChangeKey(0), getChangeKey(2), getChangeKey(4));
    assertThat(crdInfo.neededBy).isEmpty();
    assertThat(crdInfo.cycle).isFalse();
  }

  @Test
  public void testSingleNeededBy() throws Exception {
    List<ChangeInfo> neededBy = new ArrayList<>();
    neededBy.add(getChangeInfo(1));

    configureMocks(new ArrayList<>(), new ArrayList<>(), neededBy);

    GetCrd getCrd = createGetCrd();
    Response<CrdInfo> response = getCrd.apply(rsrc);

    assertThat(response.statusCode()).isEqualTo(200);
    CrdInfo crdInfo = response.value();
    assertThat(crdInfo.dependsOnFound).isEmpty();
    assertThat(crdInfo.dependsOnMissing).isEmpty();
    assertThat(crdInfo.neededBy).containsExactly(getChangeInfo(1));
    assertThat(crdInfo.cycle).isFalse();
  }

  @Test
  public void testMultipleNeededBy() throws Exception {
    List<ChangeInfo> neededBy = new ArrayList<>();
    neededBy.add(getChangeInfo(1));
    neededBy.add(getChangeInfo(3));
    neededBy.add(getChangeInfo(5));

    configureMocks(new ArrayList<>(), new ArrayList<>(), neededBy);

    GetCrd getCrd = createGetCrd();
    Response<CrdInfo> response = getCrd.apply(rsrc);

    assertThat(response.statusCode()).isEqualTo(200);
    CrdInfo crdInfo = response.value();
    assertThat(crdInfo.dependsOnFound).isEmpty();
    assertThat(crdInfo.dependsOnMissing).isEmpty();
    assertThat(crdInfo.neededBy)
        .containsExactly(getChangeInfo(1), getChangeInfo(3), getChangeInfo(5));
    assertThat(crdInfo.cycle).isFalse();
  }

  @Test
  public void testMixed() throws Exception {
    List<ChangeInfo> dependsOnFound = new ArrayList<>();
    dependsOnFound.add(getChangeInfo(2));
    dependsOnFound.add(getChangeInfo(4));

    List<String> dependsOnMissing = new ArrayList<>();
    dependsOnMissing.add(getChangeKey(5));
    dependsOnMissing.add(getChangeKey(6));

    List<ChangeInfo> neededBy = new ArrayList<>();
    neededBy.add(getChangeInfo(1));
    neededBy.add(getChangeInfo(3));

    configureMocks(dependsOnFound, dependsOnMissing, neededBy);

    GetCrd getCrd = createGetCrd();
    Response<CrdInfo> response = getCrd.apply(rsrc);

    assertThat(response.statusCode()).isEqualTo(200);
    CrdInfo crdInfo = response.value();
    assertThat(crdInfo.dependsOnFound).containsExactly(getChangeInfo(2), getChangeInfo(4));
    assertThat(crdInfo.dependsOnMissing).containsExactly(getChangeKey(5), getChangeKey(6));
    assertThat(crdInfo.neededBy).containsExactly(getChangeInfo(1), getChangeInfo(3));
    assertThat(crdInfo.cycle).isFalse();
  }

  @Test
  public void testSimpleCycle() throws Exception {
    List<ChangeInfo> dependsOn = new ArrayList<>();
    dependsOn.add(getChangeInfo(1));

    List<ChangeInfo> neededBy = new ArrayList<>();
    neededBy.add(getChangeInfo(1));

    configureMocks(dependsOn, new ArrayList<>(), neededBy);

    GetCrd getCrd = createGetCrd();
    Response<CrdInfo> response = getCrd.apply(rsrc);

    assertThat(response.statusCode()).isEqualTo(200);
    CrdInfo crdInfo = response.value();
    assertThat(crdInfo.dependsOnFound).containsExactly(getChangeInfo(1));
    assertThat(crdInfo.dependsOnMissing).isEmpty();
    assertThat(crdInfo.neededBy).containsExactly(getChangeInfo(1));
    assertThat(crdInfo.cycle).isTrue();
  }

  public void configureMocks(
      final List<ChangeInfo> dependsOnFound,
      final List<String> dependsOnMissing,
      final List<ChangeInfo> neededBy)
      throws Exception {
    Change.Key changeKey = Change.key("I0123456789");
    Change change = new Change(changeKey, null, null, null, null);
    rsrc = mock(RevisionResource.class);
    when(rsrc.getChange()).thenReturn(change);

    dependsOnFetcher = mock(DependsOnFetcher.class);
    when(dependsOnFetcher.fetchForRevision(rsrc))
        .thenReturn(new ImmutablePair<>(dependsOnFound, dependsOnMissing));

    neededByFetcher = mock(NeededByFetcher.class);
    when(neededByFetcher.fetchForChangeKey(changeKey)).thenReturn(neededBy);
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

  private GetCrd createGetCrd() {
    return new GetCrd(dependsOnFetcher, neededByFetcher);
  }
}
