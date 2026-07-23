package com.kis.mindfocus.di

val appModules = listOf(
    coroutinesModule,
    networkModule,
    // Declared per variant: see src/debug and src/release.
    apiModule,
    databaseModule,
    dataModule,
    detectionModule,
    featureModule,
)
