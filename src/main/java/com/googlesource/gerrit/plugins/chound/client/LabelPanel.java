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
import com.google.gerrit.client.rpc.NativeMap;
import com.google.gerrit.plugin.client.extension.Panel;
import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.InlineHyperlink;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class LabelPanel extends VerticalPanel {
  static class Factory implements Panel.EntryPoint {
    @Override
    public void onLoad(Panel panel) {
      panel.setWidget(new LabelPanel(panel));
    }
  }

  LabelPanel(final Panel panel) {
    ChangeInfo change =
        panel.getObject(GerritUiExtensionPoint.Key.CHANGE_INFO).cast();
    RestApi r = new RestApi("changes")
      .view("?q=message:" + change.changeId())
      .view("+NOT+change:" + change.changeId());
    r.get(new AsyncCallback<NativeMap<ChangeInfo>>() {
      @Override
      public void onSuccess(NativeMap<ChangeInfo> result) {
        if (!result.isEmpty()) {
          display(result);
        }
      }

      @Override
      public void onFailure(Throwable caught) {
        // never invoked
      }
    });
  }

  private void display(NativeMap<ChangeInfo> result) {
    if (!result.isEmpty()) {
      HorizontalPanel p = new HorizontalPanel();
      InlineLabel il = new InlineLabel("Dependency");
      p.add(il);
      for (String key : result.keySet()) {
        InlineHyperlink ih = new InlineHyperlink(
            result.get(key).changeId(), "/c/" + result.get(key)._number());
        p.add(ih);
      }
      add(p);
    }
  }
}