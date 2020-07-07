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

export const htmlTemplate = Polymer.html`
    <style include="shared-styles">
      section.related-changes-section {
        margin-bottom: 1.4em; /* Same as line height for collapse purposes */
        display: block;
      }
      div.foo {
        margin-bottom: 1.4em; /* Same as line height for collapse purposes */
      }
      a {
        display: block;
      }
      .changeContainer,
      a {
        max-width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      .changeContainer {
        display: flex;
      }
      .changeContainer.thisChange:before {
        content: '➔';
        width: 1.2em;
      }
      h4,
      section div {
        display: flex;
      }
      h4:before,
      section div:before {
        content: ' ';
        flex-shrink: 0;
        width: 1.2em;
      }
      .status {
        color: var(--deemphasized-text-color);
        font-weight: var(--font-weight-bold);
        margin-left: var(--spacing-xs);
      }
      /* The above styles are copy/paste from gr-related-changes-list_html.js */
      .dependencyCycleDetected {
        color: #d17171;
      }
      .missingFromThisServer {
        color: #d17171;
      }
      .hidden {
        display: none;
      }
    </style>
    <template is="dom-if" if="[[_crd_loaded]]">
      <template is="dom-if" if="[[_isDependsOnSectionVisible()]]">
        <section class="related-changes-section">
          <h4>Depends on</h4>
          <template is="dom-repeat" items="[[_crd.depends_on_found]]">
            <div class="changeContainer zuulDependencyContainer">
              <a
                href$="[[_computeDependencyUrl(item)]]"
                title$="[[item.project]]: [[item.branch]]: [[item.subject]]"
              >
                [[item.project]]: [[item.branch]]: [[item.subject]]
              </a>
              <span class$="[[_computeChangeStatusClass(item)]]">
                ([[_computeChangeStatus(item)]])
              </span>
              <template is="dom-if" if="[[_crd.cycle]]">
                <span class="status dependencyCycleDetected">
                  (Dependency cycle detected)
                </span>
              </template>
            </div>
          </template>
          <template is="dom-repeat" items="[[_crd.depends_on_missing]]">
            <div class="changeContainer zuulDependencyContainer">
              <span>
                [[item]]
              </span>
              <span class="status missingFromThisServer">
                (Missing from this server)
              </span>
            </div>
          </template>
        </section>
      </template>
      <template is="dom-if" if="[[_crd.needed_by.length]]">
        <section class="related-changes-section">
          <h4>Needed by</h4>
          <template is="dom-repeat" items="[[_crd.needed_by]]">
            <div class="changeContainer zuulDependencyContainer">
              <a
                href$="[[_computeDependencyUrl(item)]]"
                title$="[[item.project]]: [[item.branch]]: [[item.subject]]"
              >
                [[item.project]]: [[item.branch]]: [[item.subject]]
              </a>
              <span class$="[[_computeChangeStatusClass(item)]]">
                ([[_computeChangeStatus(item)]])
              </span>
              <template is="dom-if" if="[[_crd.cycle]]">
                <span class="status dependencyCycleDetected">
                  (Dependency cycle detected)
                </span>
              </template>
            </div>
          </template>
        </section>
      </template>
    </template>
`;

