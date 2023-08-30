<!--
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
 -->

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>

<%@include file="/WEB-INF/jsp/common/taglibs.jsp"%>

<t:layoutpage>

    <%@include file="/WEB-INF/jsp/common/common.jsp"%>

    <div class="col-md-1"></div>
    <div class="col-md-10">
        <c:url var="loadersUrl" value="/loaders"/>
        <h3>
            Loader ${model.loader.id} (<fmt:formatDate pattern="HH:mm:ss.SSS" value="${model.currentDate}"/>)
        </h3>
        <ul class="nav nav-tabs">
            <li>
                <a href="#Loader" data-toggle="tab">Loader Information</a>
            </li>
            <li>
                <a href="#Sorting" data-toggle="tab">Global Sorting</a>
            </li>
            <li>
                <a href="#Statistics" data-toggle="tab">Statistics</a>
            </li>
        </ul>

        <div id="tabContent" class="tab-content">
            <%-- tab1: Loader --%>
            <div class="tab-pane " id="Loader">
                <br/>

                <table class="table table-hover table-striped">
                    <tr>
                        <th align="left">Stream:</th>
                        <td><c:out value="${model.loader.targetStreamKey}"/></td>
                    </tr>
                    <tr>
                        <th align="left">User:</th>
                        <td><c:out value="${model.loader.user}"/></td>
                    </tr>
                    <tr>
                        <th align="left">Application:</th>
                        <td><c:out value="${model.loader.application}"/></td>
                    </tr>
                    <tr>
                        <th align="left">Options:</th>
                        <td>
                            <c:set var="options" value="${model.loader.options}"/>
                            <c:if test="${options.raw}">Raw;</c:if>
                            <c:if test="${options.globalSorting}">Sorted;</c:if>
                            <c:if test="${options.channelPerformance == ChannelPerformance.MIN_LATENCY}">Minimize Latency</c:if>
                        </td>
                    </tr>
                    <tr>
                        <th align="left">Opened:</th>
                        <td><fmt:formatDate pattern="yyyy-MM-dd HH:mm:ss.SSS" value="${model.loader.openDate}"/></td>
                    </tr>
                    <tr>
                        <th align="left">Closed:</th>
                        <td><fmt:formatDate pattern="yyyy-MM-dd HH:mm:ss.SSS" value="${model.loader.closeDate}"/></td>
                    </tr>
                    <tr>
                        <th align="left"># Messages:</th>
                        <td>${model.loader.totalNumMessages}</td>
                    </tr>
                    <tr>
                        <th align="left">Last Message Time:</th>
                        <td><fmt:formatDate pattern="${model.getFormat(model.loader.lastMessageDate)}" timeZone="UTC" value="${model.loader.lastMessageDate}"/></td>
                    </tr>
                    <tr>
                        <th align="left">Last send() Time:</th>
                        <td><fmt:formatDate pattern="${model.getFormat(model.loader.lastMessageSysDate)}" value="${model.loader.lastMessageSysDate}"/></td>
                    </tr>

                </table>
            </div>

            <%-- tab2: Sorting --%>
            <div class="tab-pane " id="Sorting">
                <br/>

                <c:choose>
                    <c:when test="${model.loader.options.globalSorting}">
                        <c:set var="sorter" value="${model.loader.globalSorter}"/>
                        <c:set var="loading" value="${sorter.buffer!=null}"/>
                        <table class="table table-hover table-striped table-bordered">
                            <tr>
                                <th align="left">Phase:</th>
                                <td>
                                    <c:choose>
                                        <c:when test="${loading}">
                                            loading into buffer (<c:out value="${sorter.bufferSize}"/> bytes)
                                        </c:when>
                                        <c:otherwise>
                                            flushing to tickdb
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                            <tr>
                                <th align="left">Swap file:</th>
                                <td><code><c:out value="${sorter.tmpFile}"/></code></td>
                            </tr>
                            <tr>
                                <th align="left">Messages in the buffer:</th>
                                <td><code><c:out value="${sorter.totalNumMessages}"/></code></td>
                            </tr>
                            <c:if test="${!loading}">
                                <tr>
                                    <th align="left">Flushing progress:</th>
                                    <td><code><c:out value="${loader.progress}"/></code></td>
                                </tr>
                            </c:if>
                        </table>
                    </c:when>
                    <c:otherwise>
                        <div class="label label-info">Global sorting is not specified.</div>
                        <br/>
                        <br/>
                    </c:otherwise>
                </c:choose>
            </div>
            <%-- tab3: Statistics --%>
            <div class="tab-pane " id="Statistics">
                <br/>

                <c:set var="stats" value="${model.loader}"/>
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
                                <td align="center">${loop.index}</td>
                                <td><c:out value="${row.symbol}"/></td>
                                <td align="right"><fmt:formatNumber groupingUsed="true" value="${row.totalNumMessages}"/></td>
                                <td><fmt:formatDate pattern="yyyy-MM-dd HH:mm:ss.SSS" timeZone="UTC" value="${row.lastMessageDate}"/></td>
                                <td><fmt:formatDate pattern="yyyy-MM-dd HH:mm:ss.SSS" value="${row.lastMessageSysDate}"/></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>

                <script type="application/javascript">
                    function activateTab(tab){
                        $('.nav-tabs a[href="' + tab + '"]').tab('show');
                    };

                    var itemName = "loaderTab";
                    $(document).ready(function() {
                        if (window.location.hash != null && window.location.hash != '') {
                            activateTab(window.location.hash)
                        } else {
                            var hash = sessionStorage.getItem(itemName);
                            if (hash !== null && hash.length > 0) {
                                activateTab(hash);
                            } else {
                                activateTab('#Loader')
                            }
                        }
                    });

                    $('.nav-tabs li a').click(function (e) {
                        sessionStorage.setItem(itemName, e.target.hash);
                    });
                </script>
            </div>
        </div>

        <div class="pull-right">
            <img src="${returnImgUrl}">
            <a class="action" href="${loadersUrl}"> Loaders</a>
            /
            <a class="action" href="/.."> QuantServer Home</a>
        </div>
    </div>
    <div class="col-md-1"></div>

</t:layoutpage>