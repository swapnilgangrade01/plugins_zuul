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
      hidden: {
        type: Boolean,
        value: true,
        reflectToAttribute: true,
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
    this.setHidden(true);
    const url = '/changes/' + this.change.id + '/revisions/current/crd';
    return this.plugin.restApi().send('GET', url).then(crd => {
      this._crd = crd;
      this._crd_loaded = true;
      this.setHidden(!(this._isDependsOnSectionVisible()
                       || crd.needed_by.length));
    });
  }

  // copied from gr-related-changes-list.js, which is inaccessible from here.
  // Resolved uses of `this.ChangeStatus.[...]`, as that's inaccessible from here too.
  // Removed _isIndirectAncestor check, as the needed data is inaccessible from here.
  // Not all code paths are reachable, as we only have shallow ChangeInfo objects. We leave the
  // code here nonetheless, to allow for easier updating from gr-related-changes-list.js.
  _computeChangeStatusClass(change) {
    const classes = ['status'];
    if (change._revision_number != change._current_revision_number) {
      classes.push('notCurrent');
    } else if (change.submittable) {
      classes.push('submittable');
    } else if (change.status == 'NEW') {
      classes.push('hidden');
    }
    return classes.join(' ');
  }

  // copied from gr-related-changes-list.js, which is inaccessible from here.
  // Resolved uses of `this.ChangeStatus.[...]`, as that's inaccessible from here too.
  // Removed _isIndirectAncestor check, as the needed data is inaccessible from here.
  // Not all code paths are reachable, as we only have shallow ChangeInfo objects. We leave the
  // code here nonetheless, to allow for easier updating from gr-related-changes-list.js.
  _computeChangeStatus(change) {
    switch (change.status) {
      case 'MERGED':
        return 'Merged';
      case 'ABANDONED':
        return 'Abandoned';
    }
    if (change._revision_number != change._current_revision_number) {
      return 'Not current';
    } else if (change.submittable) {
      return 'Submittable';
    }
    return '';
  }

  setHidden(hidden) {
    if (this.hidden != hidden) {
      this.hidden = hidden;

      // Flag to parents that something changed
      this.dispatchEvent(new CustomEvent('new-section-loaded', {
        composed: true, bubbles: true,
      }));
    }
  }

  _computeDependencyUrl(changeInfo) {
    return Gerrit.Nav.getUrlForSearchQuery(changeInfo.change_id);
  }

  _isDependsOnSectionVisible() {
    return !!(this._crd.depends_on_found.length
              + this._crd.depends_on_missing.length);
  }
}

customElements.define(GrZuul.is, GrZuul);
