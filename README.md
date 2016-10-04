# Gerrit Depends On Plugin

Openstack uses [Zuul] for project gating and one of the features of Zuul is
[cross repository dependencies] (CRD).  Zuul will detect CRDs in git
commit messages to allow it to arrange patchsets (in order) before scheduling
them for build and test.

It can become very difficult, downright confusing really to determine the
dependency relationships with only the 'depends-on' reference.  Also the use
of CRD can produce [dependency cycles] which can cause Zuul to be ineffective.
To help alleviate these issues this plugin adds a 'needed-by' reference on the
Gerrit UI.  It also adds dependency cycle detection and will display the CRD
references in red if a cycle has been detected.  This plugin also adds a REST
endpoint to allow other clients to retrieve CRD info.

More information about this plugin can be found in the documentation.

[Zuul]: http://docs.openstack.org/infra/zuul/index.html
[cross repository dependencies]: http://docs.openstack.org/infra/zuul/gating.html#cross-repository-dependencies
[dependency cycles]: http://docs.openstack.org/infra/zuul/gating.html#cycles
