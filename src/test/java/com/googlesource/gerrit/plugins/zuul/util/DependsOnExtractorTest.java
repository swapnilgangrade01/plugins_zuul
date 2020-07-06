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

import com.google.gerrit.entities.Change;
import java.util.List;
import org.junit.Test;

public class DependsOnExtractorTest {
  @Test
  public void testExtractNoDependies() throws Exception {
    DependsOnExtractor extractor = new DependsOnExtractor();

    List<String> extracted = extractor.extract("foo");

    assertThat(extracted).isEmpty();
  }

  @Test
  public void testExtractNoDependencyTooShortChangeId() throws Exception {
    DependsOnExtractor extractor = new DependsOnExtractor();
    String commitMessage = "subject\n\nDepends-On: I123456789112345678921234567893123456789";

    List<String> extracted = extractor.extract(commitMessage);

    assertThat(extracted).isEmpty();
  }

  @Test
  public void testExtractNoDependencyTooLongChangeId() throws Exception {
    DependsOnExtractor extractor = new DependsOnExtractor();
    String commitMessage = "subject\n\nDepends-On: I12345678911234567892123456789312345678941";

    List<String> extracted = extractor.extract(commitMessage);

    assertThat(extracted).isEmpty();
  }

  @Test
  public void testExtractSingleDependencyLineEnd() throws Exception {
    DependsOnExtractor extractor = new DependsOnExtractor();
    String commitMessage = "subject\n\nDepends-On: " + getChangeKey(1);

    List<String> extracted = extractor.extract(commitMessage);

    assertThat(extracted).containsExactly(getChangeKey(1));
  }

  @Test
  public void testExtractSingleDependencyContinuedLine() throws Exception {
    DependsOnExtractor extractor = new DependsOnExtractor();
    String commitMessage = "subject\n\nDepends-On: " + getChangeKey(1) + " ";

    List<String> extracted = extractor.extract(commitMessage);

    assertThat(extracted).containsExactly(getChangeKey(1));
  }

  @Test
  public void testExtractMultipleDependencies() throws Exception {
    DependsOnExtractor extractor = new DependsOnExtractor();
    String commitMessage =
        "subject\n\nDepends-On: "
            + getChangeKey(1)
            + "\nDepends-On: "
            + getChangeKey(2)
            + "\nDepends-On: "
            + getChangeKey(3);

    List<String> extracted = extractor.extract(commitMessage);

    assertThat(extracted).containsExactly(getChangeKey(1), getChangeKey(2), getChangeKey(3));
  }

  @Test
  public void testExtractLowerCase() throws Exception {
    DependsOnExtractor extractor = new DependsOnExtractor();
    String commitMessage = "subject\n\ndepends-on: " + getChangeKey(1);

    List<String> extracted = extractor.extract(commitMessage);

    assertThat(extracted).containsExactly(getChangeKey(1));
  }

  @Test
  public void testExtractMixedCases() throws Exception {
    DependsOnExtractor extractor = new DependsOnExtractor();
    String commitMessage =
        "subject\n\ndepends-On: " + getChangeKey(1) + "\nDePeNds-on: " + getChangeKey(2);

    List<String> extracted = extractor.extract(commitMessage);

    assertThat(extracted).containsExactly(getChangeKey(1), getChangeKey(2));
  }

  private String getChangeKey(int keyEnding) {
    return Change.key("I0123456789abcdef0000000000000000000" + (10000 + keyEnding)).toString();
  }
}
