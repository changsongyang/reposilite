/*
 * Copyright (c) 2021 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.panda_lang.reposilite.storage

import net.dzikoysk.dynamiclogger.Journalist
import org.panda_lang.reposilite.maven.api.Repository
import org.panda_lang.reposilite.storage.infrastructure.FileSystemStorageProviderFactory
import org.panda_lang.reposilite.storage.infrastructure.S3StorageProvider
import org.panda_lang.reposilite.storage.infrastructure.S3StorageProviderSettings
import picocli.CommandLine
import java.nio.file.Files
import java.nio.file.Path

internal object StorageProviderFactory {

    fun createStorageProvider(journalist: Journalist, workingDirectory: Path, repositoryName: String, storageDescription: String, quota: String): StorageProvider {
        if (storageDescription.startsWith("fs")) {
            val repositoryDirectory = workingDirectory.resolve(Repository.REPOSITORIES_PATH).resolve(repositoryName)
            Files.createDirectories(repositoryDirectory)

            return FileSystemStorageProviderFactory.of(journalist, repositoryDirectory, quota)
        }

        if (storageDescription.startsWith("s3")) {
            // Implement quota
            val settings = loadConfiguration(S3StorageProviderSettings(), storageDescription)
            return S3StorageProvider(journalist, settings.bucketName, settings.region)
        }

        if (storageDescription.equals("rest", ignoreCase = true)) {
            // TOFIX REST API storage endpoint
        }

        throw UnsupportedOperationException("Unknown storage provider: $storageDescription")
    }

    private fun <CONFIGURATION : Runnable> loadConfiguration(configuration: CONFIGURATION, description: String): CONFIGURATION =
        CommandLine.populateCommand(configuration, *description.split(" ").toTypedArray())

}