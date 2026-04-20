package com.nikka.di

import com.nikka.core.data.JsonTaskRepository
import com.nikka.core.data.NotificationScheduler
import com.nikka.core.data.TaskRepository
import com.nikka.core.data.createDiscordWebhookClient
import com.nikka.feature.home.HomeViewModel
import com.nikka.feature.settings.SettingsViewModel
import kotlinx.datetime.Clock
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<TaskRepository> { JsonTaskRepository() }
    single<Clock> { Clock.System }
    single { createDiscordWebhookClient() }
    single { NotificationScheduler(get(), get(), get()) }
    viewModel { HomeViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
}
