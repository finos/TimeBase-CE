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
package com.epam.deltix.qsrv.hf.tickdb.web.controller;

import com.epam.deltix.qsrv.hf.tickdb.http.AbstractHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author Daniil Yarmalkevich
 * Date: 11/11/2019
 */
@Controller
@RequestMapping(value = "/")
public class TimeBasePingController {

    @RequestMapping(value = "/ping", method = RequestMethod.GET)
    public ResponseEntity<String> startPage() {
        try {
            if (AbstractHandler.TDB.isOpen()) {
                return ResponseEntity.ok().body("TimeBase is ready.");
            }
        } catch (Exception ignored) {
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("TimeBase is not ready.");
    }

}