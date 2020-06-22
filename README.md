# Gerrit Zuul Plugin

Openstack uses [Zuul] for project gating and one of the features of Zuul is
[cross repository dependencies] (CRD).  Zuul will detect CRDs in git
commit messages to allow it to arrange patchsets (in order) before scheduling
them for build and test.

The problems with having only a 'depends-on' reference:
* It can become very difficult, downright confusing really, to determine the
dependency relationships.
* The use of CRD can produce [dependency cycles] which can be difficult to
detect.

To help alleviate these issues this plugin adds the following:
* A reverse lookup for the 'depends-on' reference.
* A REST endpoint to allow other clients to retrieve CRD info.

Detailed information about this plugin can be found in the documentation.

[Zuul]: http://docs.openstack.org/infra/zuul/index.html
[cross repository dependencies]: http://docs.openstack.org/infra/zuul/gating.html#cross-repository-dependencies
[dependency cycles]: http://docs.openstack.org/infra/zuul/gating.html#cycles
