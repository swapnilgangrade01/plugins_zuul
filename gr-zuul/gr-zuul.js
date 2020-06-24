/**
 * @license
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {htmlTemplate} from './gr-zuul_html.js';

class GrZuul extends Polymer.Element {
  /** @returns {string} name of the component */
  static get is() { return 'gr-zuul'; }

  /** @returns {?} template for this component */
  static get template() { return htmlTemplate; }

  static get properties() {
    return {
      change: {
        type: Object,
        observer: '_onChangeChanged',
      },
      _crd: {
        type: Object,
        value: {},
      },
      _crd_loaded: {
        type: Boolean,
        value: false,
      },
    };
  }

  _onChangeChanged() {
    this._crd_loaded = false;
    const url = '/changes/' + this.change.id + '/revisions/current/crd';
    return this.plugin.restApi().send('GET', url).then(crd => {
      this._crd = crd;
      this._crd_loaded = true;
    });
  }

  _computeDependencyUrl(changeId) {
    return Gerrit.Nav.getUrlForSearchQuery(changeId);
  }
}

customElements.define(GrZuul.is, GrZuul);
