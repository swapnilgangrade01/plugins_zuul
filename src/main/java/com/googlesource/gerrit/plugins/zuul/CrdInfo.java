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

import com.google.gerrit.extensions.common.ChangeInfo;
import java.util.List;

/** Cross-repository dependencies of a Change */
public class CrdInfo {
  /** Shallow ChangeInfos of changes that depend on this Change and are available on this server */
  public List<ChangeInfo> dependsOnFound;

  /** Change-Ids of changes that depend on this Change and are not available on this server */
  public List<String> dependsOnMissing;

  /** Shallow ChangeInfos of changes that depend on this Change */
  public List<ChangeInfo> neededBy;

  /** true, if this change is contained in a dependency cycle */
  public boolean cycle;
}
