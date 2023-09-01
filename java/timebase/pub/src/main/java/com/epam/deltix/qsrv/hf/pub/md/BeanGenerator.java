/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.pub.md;

import com.epam.deltix.util.lang.StringUtils;


public class BeanGenerator {
    public static String    escapeIdentifierForJava (String id) {
        if (StringUtils.isJavaReservedWord (id))
            return (escapeJavaReservedWord (id));

        if (StringUtils.isValidJavaIdOrKeyword (id))
            return (id);

        return (doEscapeIdentifierForJava (id));
    }

    public static String    escapeIdentifierForCS (String id) {
        if (StringUtils.isCSReservedWord (id))
            return ("@" + id);

        if (StringUtils.isValidCSIdOrKeyword (id))
            return (id);

        return (doEscapeIdentifierForCS (id));
    }

    public static String    doEscapeIdentifierForJava (String id) {
        final StringBuilder     sb = new StringBuilder ();
        final int               fnameLength = id.length ();

        for (int ii = 0; ii < fnameLength; ii++) {
            char    c = id.charAt (ii);

            if (c == '$')
                sb.append ("$$");
            else if (ii == 0 ?
                        !Character.isJavaIdentifierStart (c) :
                        !Character.isJavaIdentifierPart (c))
            {
                sb.append ("$");
                sb.append ((int) c);
                sb.append ("$");
            }
            else
                sb.append (c);
        }

        return (sb.toString ());
    }

    public static String    doEscapeIdentifierForCS (String id) {
        final StringBuilder     sb = new StringBuilder ();
        final int               n = id.length ();

        for (int ii = 0; ii < n; ii++) {
            char    c = id.charAt (ii);

            if (c == '_')
                sb.append ("__");
            else if (ii == 0 ?
                        !StringUtils.isCSIdentifierStart (c) :
                        !StringUtils.isCSIdentifierPart (c))
            {
                sb.append ("_");
                sb.append ((int) c);
                sb.append ("_");
            }
            else
                sb.append (c);
        }

        return (sb.toString ());
    }

    public static String escapeJavaReservedWord (String keyword){
        return "$" + keyword;
    }
}