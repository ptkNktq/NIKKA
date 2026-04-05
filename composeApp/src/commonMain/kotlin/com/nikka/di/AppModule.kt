package com.nikka.di

import com.nikka.core.data.JsonTaskRepository
import com.nikka.core.data.TaskRepository
import com.nikka.feature.home.HomeViewModel
import kotlinx.datetime.Clock
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<TaskRepository> { JsonTaskRepository() }
    single<Clock> { Clock.System }
    viewModel { HomeViewModel(get(), get()) }
}
