embulk-util-guess
==================

For Embulk plugin developers
-----------------------------

* [Javadoc](https://dev.embulk.org/embulk-util-guess/)

For Maintainers
----------------

### Release

Modify `version` in `build.gradle` at a detached commit to bump up the versions of Embulk standard plugins.

```
git checkout --detach master

(Edit: Remove "-SNAPSHOT" in "version" in build.gradle.)

git add build.gradle

git commit -m "Release vX.Y.Z"

git tag -a vX.Y.Z

(Edit: Write a tag annotation in the changelog format.)

git push -u origin vX.Y.Z  # Pushing a version tag would trigger a release operation on GitHub Actions after approval.
```
