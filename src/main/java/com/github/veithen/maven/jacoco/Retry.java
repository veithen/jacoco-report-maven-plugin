/*-
 * #%L
 * jacoco-report-maven-plugin
 * %%
 * Copyright (C) 2018 - 2021 Andreas Veithen
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.veithen.maven.jacoco;

import java.io.IOException;
import java.util.function.Supplier;

import javax.ws.rs.ProcessingException;

public final class Retry {
    private Retry() {}

    public static <T> T withRetry(Supplier<T> retryable) {
        int numAttempts = 0;
        long delay = 500;
        while (true) {
            numAttempts++;
            try {
                return retryable.get();
            } catch (ProcessingException ex) {
                if (ex.getCause() instanceof IOException && numAttempts < 4) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ex2) {
                        throw new ProcessingException(ex2);
                    }
                    delay *= 2;
                    continue;
                }
                throw ex;
            }
        }
    }
}
