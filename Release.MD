Retrofit-kx configured for sonatype hosting, and so some minimal local setup should be done for security purpose  

### Local properties setup:

- username: Sonatype user id
- password: Sonatype user password
- publishKeyId: gpg key id, retrieved from [Publish key id generation](#publish-key-id-generation) section
- publishSecret: gpg secret, retrieved from [Publish secret generation](#publish-secret-generation) section 
- publishPassword: gpg password, used to sign artifacts

#### Publish key id generation

Last 8 symbols from `pub` section, when executing `gpg --fingerprint` command

#### Publish secret generation

- gpgUserId: gpg user id
- pubRingPath: path to pubring.kbx file

```
gpg --export-secret-keys --armor $gpgUserId $pubRingPath | grep -v '\-\-' | grep -v '^=.' | tr -d '\n'
```

## Release

In case api changed (`retrofit-kx` module or `retrofit-kx-ksp` module) we need to:
- change isReleaseReady to false - `buildSrc/../retrofitx.publish.PublishPlugin -> publishEnvironment.isReleaseReady`
- add test cases for new changes
- bump api version - `buildSrc/../retrofitx.publish.PublishPlugin -> publishEnvironment.baseVersion`
- test [local deployment](#local-deployment) with bumped version
- test `kotlin-sample` and `android-sample` with new local artifacts
- upload artifacts to remote by following [Remote deployment](#remote-deployment) section
- retest `kotlin-sample` and `android-sample` with remote artifacts

If release considered stable, then proceed with next steps:
- change isReleaseReady to true - `buildSrc/../retrofitx.publish.PublishPlugin -> publishEnvironment.isReleaseReady`
- upload artifacts to remote by following [Remote deployment](#remote-deployment) section

### Local deployment

To test local deployment:
- uncomment `mavenLocal()` line from settings.gradle.kts
- use `publishToMavenToLocal()` gradle tasks for `retrofit-kx` / `retrofit-kx-ksp` to update local artifacts with new changes
- verify `kotlin-sample` and `android-sample` projects with locally deployed changes
- comment `mavenLocal()` from step 1 back

### Remote deployment

To deploy artifacts to remote repository use `publishAllPublicationsToMavenCentralRepository()` tasks for `retrofit-kx` / `retrofit-kx-ksp`  
In case new changes from remote repository not picking up, use `Refresh gradle dependencies` option (right click on root project in gradle section)