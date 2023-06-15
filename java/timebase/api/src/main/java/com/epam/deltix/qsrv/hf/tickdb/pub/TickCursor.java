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
package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;
import com.epam.deltix.qsrv.hf.pub.*;

/**
 *  A cursor (also known as iterator, or result set) for reading data from a
 *  stream. This class provides implements {@link com.epam.deltix.qsrv.hf.tickdb.pub.query.SubscriptionController}
 *  for dynamically reconfiguring the feed, as well as method {@link #reset} for
 *  essentially re-opening the cursor on a completely different timestamp.
 *
 *  This class extends [sequential] FilteredMessageSource with random access
 *  to underlying source using {@link #reset(long)} method.
 */
public interface TickCursor extends InstrumentMessageSource {

}