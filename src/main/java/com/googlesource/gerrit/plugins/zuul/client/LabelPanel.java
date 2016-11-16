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

package com.googlesource.gerrit.plugins.zuul.client;

import com.google.gerrit.client.GerritUiExtensionPoint;
import com.google.gerrit.client.info.ChangeInfo;
import com.google.gerrit.client.info.ChangeInfo.RevisionInfo;
import com.google.gerrit.plugin.client.Plugin;
import com.google.gerrit.plugin.client.extension.Panel;
import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gwt.http.client.URL;
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

  private final static String COLOR_RED = "#F00";

  LabelPanel(final Panel panel) {
    final ChangeInfo change =
        panel.getObject(GerritUiExtensionPoint.Key.CHANGE_INFO).cast();
    final RevisionInfo rev =
        panel.getObject(GerritUiExtensionPoint.Key.REVISION_INFO).cast();

    if (!rev.isEdit()) {
      String decodedChangeId = URL.decodePathSegment(change.id());
      new RestApi("changes").id(decodedChangeId).view("revisions").id(rev.id())
          .view(Plugin.get().getPluginName(), "crd")
          .get(new AsyncCallback<DependencyInfo>() {
            @Override
            public void onSuccess(DependencyInfo result) {
              if (result != null) {
                display(result);
              }
            }

            @Override
            public void onFailure(Throwable caught) {
              // never invoked
            }
          });
    }
  }

  private void display(DependencyInfo result) {
    int row = 0;
    int column = 1;
    Grid grid = new Grid(row, column);
    // show depends-on ids
    for (int i=0; i < result.dependsOn().length(); i++) {
      HorizontalPanel p = new HorizontalPanel();
      p.addStyleName("infoBlock");
      Label label = new Label("Depends-on");
      label.setWidth("72px");
      p.add(label);
      CopyableLabel cl = new CopyableLabel(result.dependsOn().get(i));
      if (result.cycle()) {
        cl.getElement().getStyle().setColor(COLOR_RED);
      }
      p.add(cl);
      grid.insertRow(row);
      grid.setWidget(row, 0, p);
      row++;
    }
    // show needed-by ids
    for (int i=0; i < result.neededBy().length(); i++) {
      HorizontalPanel p = new HorizontalPanel();
      p.addStyleName("infoBlock");
      Label label = new Label("Needed-by");
      label.setWidth("72px");
      p.add(label);
      CopyableLabel cl = new CopyableLabel(result.neededBy().get(i));
      if (result.cycle()) {
        cl.getElement().getStyle().setColor(COLOR_RED);
      }
      p.add(cl);
      grid.insertRow(row);
      grid.setWidget(row, 0, p);
      row++;
    }
    add(grid);
  }
}
