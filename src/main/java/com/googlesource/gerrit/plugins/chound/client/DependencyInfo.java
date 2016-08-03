package com.googlesource.gerrit.plugins.chound.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class DependencyInfo extends JavaScriptObject {

  public final native JsArrayString dependsOn() /*-{ return this.depends_on; }-*/;
  public final native JsArrayString neededBy() /*-{ return this.needed_by; }-*/;
  public final native boolean cycle() /*-{ return this.cycle; }-*/;

  protected DependencyInfo() {
  }
}
