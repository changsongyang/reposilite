/*
 * Copyright (c) 2023 dzikoysk
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

package com.reposilite.storage.filesystem

import com.reposilite.journalist.Journalist
import com.reposilite.shared.ErrorResponse
import com.reposilite.shared.toErrorResponse
import io.javalin.http.HttpStatus.INSUFFICIENT_STORAGE
import java.nio.file.Files
import java.nio.file.Path
import panda.std.Result

/**
 * @param rootDirectory root directory of storage space
 * @param maxSize the largest amount of storage available for use, in bytes
 */
internal class FixedQuota(
    journalist: Journalist,
    rootDirectory: Path,
    private val maxSize: Long,
    maxResourceLockLifetimeInSeconds: Int
) : FileSystemStorageProvider(
    journalist = journalist,
    rootDirectory = rootDirectory,
    maxResourceLockLifetimeInSeconds = maxResourceLockLifetimeInSeconds
) {

    init {
        if (maxSize <= 0) {
            throw IllegalArgumentException("Max size parameter has to be a value greater than 0")
        }
    }

    override fun canHold(contentLength: Long): Result<Long, ErrorResponse> =
        usage()
            .map { usage -> maxSize - usage }
            .filter({ available -> contentLength <= available }, { INSUFFICIENT_STORAGE.toErrorResponse("Repository cannot hold the given file (${maxSize - it} + $contentLength > $maxSize)") })

}

/**
 * @param rootDirectory root directory of storage space
 * @param maxPercentage the maximum percentage of the disk available for use
 */
internal class PercentageQuota(
    journalist: Journalist,
    rootDirectory: Path,
    private val maxPercentage: Double,
    maxResourceLockLifetimeInSeconds: Int
) : FileSystemStorageProvider(
    journalist = journalist,
    rootDirectory = rootDirectory,
    maxResourceLockLifetimeInSeconds = maxResourceLockLifetimeInSeconds
) {

    init {
        if (maxPercentage > 1 || maxPercentage <= 0) {
            throw IllegalArgumentException("Percentage parameter has to be a value between 0.0 and 1.0")
        }
    }

    override fun canHold(contentLength: Long): Result<Long, ErrorResponse> =
        usage()
            .map { usage ->
                val capacity = Files.getFileStore(rootDirectory).usableSpace
                val max = capacity * maxPercentage
                max.toLong() - usage
            }
            .filter({ available -> contentLength <= available }, { INSUFFICIENT_STORAGE.toErrorResponse("Repository cannot hold the given file ($contentLength too much for $maxPercentage%)") })

}
