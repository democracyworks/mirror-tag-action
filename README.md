# mirror-tag-action

Warning: This readme is incomplete and untested.

A Github Action to mirror commits from recently pushed tags to a branch within the same repository

For example, if you want your `production` branch to always mirror the latest tag
this action is for you.

To use this Action, create a file called `.github/workflows/mirror.yml` with the
following contents:

```yaml
on:
  push:
    tags:
      - '*'

jobs:
  mirror_job:
    runs-on: ubuntu-latest
    name: Mirror all new tags to DESTINATION_BRANCH_NAME branch
    steps:
    - name: Mirror action step
      id: mirror
      uses: democracyworks/mirror-tag-action
      with:
        github-token: ${{ secrets.GITHUB_TOKEN }}
        dest: 'DESTINATION_BRANCH_NAME'

```

With `DESTINATION_BRANCH_NAME` replaced as appropriate.
