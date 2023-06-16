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
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%@include file="/WEB-INF/jsp/common/taglibs.jsp"%>

<t:layoutpage>

    <%@include file="/WEB-INF/jsp/common/common.jsp"%>

    <div class="col-md-1"></div>
    <div class=" col-md-10">
        <c:url var="loadersUrl" value="/cursors"/>
        <h3>
            Cursor ${model.cursor.id} (<fmt:formatDate pattern="HH:mm:ss.SSS" value="${model.currentDate}"/>)
        </h3>
        <ul class="nav nav-tabs">
            <li>
                <a href="#Cursor" data-toggle="tab">Cursor Information</a>
            </li>
            <li>
                <a href="#Subscription" data-toggle="tab">Subscription</a>
            </li>
            <li>
                <a href="#Statistics" data-toggle="tab">Statistics</a>
            </li>
        </ul>

        <div id="tabContent" class="tab-content">
            <%-- tab1: Cursor --%>
            <div class="tab-pane" id="Cursor">
                <br/>

                <table class="table table-hover table-striped">
                    <tr>
                        <th align="left">Streams:</th>
                        <td>
                            <c:forEach items="${model.cursor.sourceStreamKeys}" var="sk">
                                <c:out value="${sk}"/>
                            </c:forEach>
                        </td>
                    </tr>
                    <tr>
                        <th align="left">User:</th>
                        <td>${model.cursor.user}</td>
                    </tr>
                    <tr>
                        <th align="left">Application:</th>
                        <td>${model.cursor.application}</td>
                    </tr>
                    <tr>
                        <th align="left">Options:</th>
                        <td>
                            <c:set var="options" value="${model.cursor.options}"/>
                            <c:if test="${options.raw}">Raw;</c:if>
                            <c:if test="${options.live}">Live;</c:if>
                            <c:if test="${options.reversed}">Reversed;</c:if>
                            <c:if test="${options.allowLateOutOfOrder}">Unordered;</c:if>
                            <c:if test="${options.channelPerformance == ChannelPerformance.MIN_LATENCY}">Minimize Latency;</c:if>
                            <c:if test="${options.realTimeNotification}">Real-time;</c:if>
                        </td>
                    </tr>
                    <tr>
                        <th align="left">Opened:</th>
                        <td><fmt:formatDate pattern="yyyy-MM-dd HH:mm:ss.SSS" value="${model.cursor.openDate}"/></td>
                    </tr>
                    <tr>
                        <th align="left">Last Reset:</th>
                        <td><fmt:formatDate pattern="yyyy-MM-dd HH:mm:ss.SSS" value="${model.cursor.lastResetDate}"/></td>
                    </tr>
                    <tr>
                        <th align="left">Closed:</th>
                        <td><fmt:formatDate pattern="yyyy-MM-dd HH:mm:ss.SSS" value="${model.cursor.closeDate}"/></td>
                    </tr>
                    <tr>
                        <th align="left"># Messages:</th>
                        <td>${model.cursor.totalNumMessages}</td>
                    </tr>
                    <tr>
                        <th align="left">Last Message Time:</th>
                        <td><fmt:formatDate pattern="${model.getFormat(model.cursor.lastMessageDate)}" timeZone="UTC" value="${model.cursor.lastMessageDate}"/></td>
                    </tr>
                    <tr>
                        <th align="left">Last next() Time:</th>
                        <td><fmt:formatDate pattern="${model.getFormat(model.cursor.lastMessageSysDate)}" value="${model.cursor.lastMessageSysDate}"/></td>
                    </tr>
                </table>
            </div>
            <%-- tab2: Subscription --%>
            <div class="tab-pane " id="Subscription">
                <br/>

                <table class="table table-hover table-striped table-bordered">
                    <thead>
                        <tr>
                            <th>Instrument Type</th>
                            <th>Symbol</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${model.cursor.isAllEntitiesSubscribed() == false}">
                                <c:forEach items="${model.cursor.subscribedEntities}" var="iid">
                                    <tr>
                                        <td><c:out value="${iid.symbol}"/></td>
                                    </tr>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td>ALL</td>
                                    <td>ALL</td>
                                </tr>
                            </c:otherwise>
                        </c:choose>
                    </tbody>

                </table>

                <br/>
                <table class="table table-hover table-striped table-bordered">
                    <thead>
                        <tr>
                            <th>Message Type</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${model.cursor.isAllTypesSubscribed() == false}">
                                <c:forEach items="${model.cursor.subscribedTypes}" var="type">
                                    <tr>
                                        <td><c:out value="${type}"/></td>
                                    </tr>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <tr><td>ALL</td></tr>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>

            <%-- tab3: Statistics --%>
            <div class="tab-pane " id="Statistics">
                <br/>

                <c:set var="stats" value="${model.cursor}"/>
                <c:set var="method" value="send"/>
                <table class="table table-hover table-striped table-bordered">
                    <thead>
                    <tr>
                        <th>#</th>
                        <th>Symbol</th>
                        <th>Type</th>
                        <th># Msgs</th>
                        <th>Last Msg Time</th>
                        <th>Last <c:out value="${method}"/>() Time</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td align="center">${fn:length(stats.getInstrumentStats())}</td>
                        <th align="left" colspan="2">TOTAL</th>
                        <td align="right"><fmt:formatNumber groupingUsed="true" value="${stats.totalNumMessages}"/></td>
                        <td><fmt:formatDate pattern="yyyy-MM-dd HH:mm:ss.SSS" timeZone="UTC" value="${stats.lastMessageDate}"/></td>
                        <td><fmt:formatDate pattern="yyyy-MM-dd HH:mm:ss.SSS" value="${stats.lastMessageSysDate}"/></td>
                    </tr>

                    <c:forEach items="${stats.instrumentStats}" var="row" varStatus="loop">
                        <tr>
                            <td align="center">${loop.index + 1}</td>
                            <td><c:out value="${row.symbol}"/></td>
                            <td align="right"><fmt:formatNumber groupingUsed="true" value="${row.totalNumMessages}"/></td>
                            <td><fmt:formatDate pattern="yyyy-MM-dd HH:mm:ss.SSS" timeZone="UTC" value="${row.lastMessageDate}"/></td>
                            <td><fmt:formatDate pattern="yyyy-MM-dd HH:mm:ss.SSS" value="${row.lastMessageSysDate}"/></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>

        <div class="pull-right" >
            <img src="${returnImgUrl}">
            <a class="action" href="${loadersUrl}"> Cursors</a>
            /
            <a class="action" href="/.."> QuantServer Home</a>
        </div>

    </div>
    <div class="col-md-1"></div>


    <script type="application/javascript">
        function activateTab(tab){
            $('.nav-tabs a[href="' + tab + '"]').tab('show');
        };

        var itemName = "cursorTab";
        $(document).ready(function() {
            if (window.location.hash != null && window.location.hash != '') {
                activaTab(window.location.hash)
            } else {
                var hash = sessionStorage.getItem(itemName);
                if (hash !== null && hash.length > 0) {
                    activateTab(hash);
                } else {
                    activateTab('#Cursor')
                }
            }
        });

        $('.nav-tabs li a').click(function (e) {
            sessionStorage.setItem(itemName, e.target.hash);
        });
    </script>

</t:layoutpage>