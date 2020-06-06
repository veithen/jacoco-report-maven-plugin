/*-
 * #%L
 * jacoco-report-maven-plugin
 * %%
 * Copyright (C) 2018 - 2020 Andreas Veithen
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
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

final class ServiceMap {
    private ServiceMap() {}

    static Map<String,String> loadServiceMap(String name) {
        Enumeration<URL> urls;
        try {
            urls = ServiceMap.class.getClassLoader().getResources(name);
        } catch (IOException ex) {
            return Collections.emptyMap();
        }
        Map<String,String> map = new HashMap<>();
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            try (InputStream in = url.openStream()) {
                Properties props = new Properties();
                props.load(in);
                for (Map.Entry<Object,Object> entry : props.entrySet()) {
                    map.put((String)entry.getKey(), (String)entry.getValue());
                }
            } catch (IOException ex) {
                // Ignore and continue
            }
        }
        return map;
    }
}
