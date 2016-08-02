// Copyright (C) 2016 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.chound.client;

import com.google.gerrit.client.GerritUiExtensionPoint;
import com.google.gerrit.client.info.ChangeInfo;
import com.google.gerrit.client.info.ChangeInfo.CommitInfo;
import com.google.gerrit.client.info.ChangeInfo.RevisionInfo;
import com.google.gerrit.client.rpc.NativeMap;
import com.google.gerrit.plugin.client.extension.Panel;
import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwtexpui.clippy.client.CopyableLabel;

public class LabelPanel extends VerticalPanel {
  static class Factory implements Panel.EntryPoint {
    @Override
    public void onLoad(Panel panel) {
      panel.setWidget(new LabelPanel(panel));
    }
  }

  LabelPanel(final Panel panel) {
    final ChangeInfo change =
        panel.getObject(GerritUiExtensionPoint.Key.CHANGE_INFO).cast();
    RevisionInfo rev =
        panel.getObject(GerritUiExtensionPoint.Key.REVISION_INFO).cast();

    new RestApi("changes").id(change.id()).view("revisions").id(rev.id())
        .view("commit").get(new AsyncCallback<CommitInfo>() {
          @Override
          public void onSuccess(CommitInfo result) {
            if (result != null) {
              displayDependsOn(result);
            }
          }

          @Override
          public void onFailure(Throwable caught) {
            // never invoked
          }
        });

    new RestApi("changes").view("?q=message:" + change.changeId())
        .view("+NOT+change:" + change.changeId())
        .get(new AsyncCallback<NativeMap<ChangeInfo>>() {
          @Override
          public void onSuccess(NativeMap<ChangeInfo> result) {
            if (!result.isEmpty()) {
              displayNeededBy(result);
            }
          }

          @Override
          public void onFailure(Throwable caught) {
            // never invoked
          }
        });
  }

  private void displayDependsOn(CommitInfo result) {
    int row = 0;
    int column = 1;
    Grid grid = new Grid(row, column);
    String message = result.message();
    if (message.toLowerCase().contains("depends-on:")) {
      MatchResult matcher;
      RegExp pattern =
          RegExp.compile("[Dd]epends-[Oo]n:? (I[0-9a-f]{8,40})", "g");
      while ((matcher = pattern.exec(message)) != null) {
        grid.insertRow(row);
        HorizontalPanel p = new HorizontalPanel();
        p.addStyleName("infoBlock");
        p.add(new Label("Depends-on"));
        p.add(new CopyableLabel(matcher.getGroup(1)));
        grid.setWidget(row, 0, p);
        row++;
      }
      add(grid);
    }
  }

  private void displayNeededBy(NativeMap<ChangeInfo> result) {
    HorizontalPanel p = new HorizontalPanel();
    p.addStyleName("infoBlock");
    p.add(new Label("Needed-by"));
    for (String key : result.keySet()) {
      p.add(new CopyableLabel(result.get(key).changeId()));
    }
    add(p);
  }
}
