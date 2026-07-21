// Configuration semantic-release. Voir docs/publication.md pour la mécanique
// complète (règles de version, pipeline CI, distribution de l'APK).
module.exports = {
  branches: ['main'],
  tagFormat: 'v${version}',
  plugins: [
    [
      'semantic-release-gitmoji',
      {
        releaseRules: {
          major: ['💥'],
          minor: ['✨'],
          patch: ['🐛', '♻️', '💄', '🚀'],
        },
      },
    ],
    [
      '@semantic-release/exec',
      {
        prepareCmd: './scripts/bump-version.sh ${nextRelease.version} && ./gradlew assembleDebug',
      },
    ],
    [
      '@semantic-release/git',
      {
        assets: ['app/build.gradle.kts'],
        message: '🔖(release): v${nextRelease.version} [skip ci]',
      },
    ],
    [
      '@semantic-release/github',
      {
        assets: [
          {
            path: 'app/build/outputs/apk/debug/app-debug.apk',
            name: 'BadgeMoi-v${nextRelease.version}.apk',
            label: 'APK de test (debug) v${nextRelease.version}',
          },
        ],
      },
    ],
  ],
}
