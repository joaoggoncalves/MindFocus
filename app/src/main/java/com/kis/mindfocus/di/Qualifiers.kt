package com.kis.mindfocus.di

import org.koin.core.qualifier.named

val IoDispatcher = named("IoDispatcher")
val DefaultDispatcher = named("DefaultDispatcher")
val ApplicationScope = named("ApplicationScope")
