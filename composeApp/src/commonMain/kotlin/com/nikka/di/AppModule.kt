package com.nikka.di

import com.nikka.core.data.JsonTaskRepository
import com.nikka.core.data.TaskRepository
import com.nikka.feature.home.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<TaskRepository> { JsonTaskRepository() }
    viewModel { HomeViewModel(get()) }
}
