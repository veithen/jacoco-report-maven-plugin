/*-
 * #%L
 * jacoco-report-maven-plugin
 * %%
 * Copyright (C) 2018 - 2023 Andreas Veithen
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
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.ServerErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Retry {
    private static final Logger log = LoggerFactory.getLogger(Retry.class);

    private Retry() {}

    public static <T> T withRetry(Supplier<T> action, Predicate<RuntimeException> retriable) {
        int numAttempts = 0;
        long delay = 500;
        while (true) {
            numAttempts++;
            try {
                return action.get();
            } catch (RuntimeException ex) {
                if (retriable.test(ex) && numAttempts < 4) {
                    log.info(
                            "Request failed with {}; retrying in {} ms",
                            ex.getClass().getName(),
                            delay);
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

    public static <T> T withRetry(Supplier<T> action) {
        return withRetry(action, Retry::isServerError);
    }

    public static boolean isServerError(RuntimeException ex) {
        return (ex instanceof ProcessingException && ex.getCause() instanceof IOException)
                || ex instanceof ServerErrorException;
    }
}
