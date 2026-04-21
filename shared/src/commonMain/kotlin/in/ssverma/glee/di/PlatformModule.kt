package `in`.ssverma.glee.di

import okio.FileSystem
import org.koin.core.module.Module

expect val platformModule: Module
expect val platformFileSystem: FileSystem
