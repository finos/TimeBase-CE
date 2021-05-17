/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.qsrv.comm.cat;


import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

public class TomcatConfig {

    public static HashMap<String, Object> getTomcatConfig(final StartConfiguration config){
        final HashMap<String, Object> tomcatConfig = new HashMap<>();
        if (config.tb != null)
            processProps("TimeBase", config.tb.getProps(), tomcatConfig);

        if (config.uhf != null)
            processProps("UHF", config.uhf.getProps(), tomcatConfig);

        if(config.agg != null)
            processProps("Aggregator", config.agg.getProps(), tomcatConfig);

        if(config.quantServer != null)
            processProps("QuantServer", config.quantServer.getProps(), tomcatConfig);

        if (config.es != null)
            processProps("ExecutionServer", config.es.getProps(), tomcatConfig);

        if(config.sts != null)
             processProps("StrategyServer", config.sts.getProps(), tomcatConfig);

        return tomcatConfig;
    }

    private static void processProps(final String prefix, final Properties properties, final HashMap<String, Object> tomcatConfig){
        final String tomcatPrefix = prefix + ".tomcat.";
        final Enumeration<Object> enumeration = properties.keys();
        while (enumeration.hasMoreElements()){
            String key = (String)enumeration.nextElement();
            if(key.contains(tomcatPrefix)){
                tomcatConfig.put( key.substring(key.lastIndexOf('.')+1, key.length()) , properties.get(key));
            }
        }
    }


}
