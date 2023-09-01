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

    <c:url var="loadersUrl" value="/connections"/>
    <h3>
        Connection '${model.dispatcher.clientId}' (<fmt:formatDate pattern="HH:mm:ss.SSS" value="${model.currentDate}"/>)
    </h3>

    <br/>
    <ul class="nav nav-tabs">
        <li>
            <a href="#Connection" data-toggle="tab">Connection Information</a>
        </li>
        <li>
            <a href="#VChannels" data-toggle="tab">Virtual Channels</a>
        </li>
    </ul>

    <div id="tabContent" class="tab-content">
            <%-- tab1: Connection --%>
        <div class="tab-pane " id="Connection">
            <br/>

            <table class="table table-hover table-striped table-bordered">
                <tr>
                    <th align="left">Client Id:</th>
                    <td>${model.dispatcher.clientId}</td>
                </tr>
                <tr>
                    <th align="left">Application:</th>
                    <td>${model.dispatcher.applicationID}</td>
                </tr>
                <tr>
                    <th align="left">Connection Time:</th>
                    <td>${model.dispatcher.creationDate}</td>
                </tr>
                <tr>
                    <th align="left">Number of Transports:</th>
                    <td>${model.dispatcher.numTransportChannels}</td>
                </tr>
                <tr>
                    <th align="left">Throughput (MB/sec):</th>
                    <td><fmt:formatNumber pattern="###,###,###.#" value="${(model.dispatcher.throughput / 1024 / 1024)}"/></td>
                </tr>
                <tr>
                    <th align="left">Avg Throughput (MB/sec):</th>
                    <td><fmt:formatNumber pattern="###,###,###.#" value="${(model.dispatcher.averageThroughput / 1024 / 1024)}"/></td>
                </tr>
                <tr>
                    <th align="left">Remote Address:</th>
                    <td>${model.dispatcher.remoteAddress}</td>
                </tr>
            </table>
        </div>
            <%-- tab2: VChannels --%>
        <div class="tab-pane " id="VChannels">
            <br/>

            <table id="vsChannelsTable" class="table table-hover table-striped table-bordered">
                <thead>
                    <tr>
                        <th>Local Id</th>
                        <th>Remote Id</th>
                        <th>State</th>
                        <th>Flush</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach items="${model.dispatcher.virtualChannels}" var="vs">
                        <c:if test="${vs != null}">
                            <tr>
                                <td>${vs.localId}</td>
                                <td>${vs.remoteId}</td>
                                <td>${vs.state}</td>
                                <td><c:if test="${vs.autoflush}">AUTO</c:if></td>
                            </tr>
                        </c:if>
                    </c:forEach>
                </tbody>
            </table>

            <script type="application/javascript">
                $(document).ready(function() {
                    $('#vsChannelsTable').DataTable( {
                        "paginate": false,
                        "searching": false
                    });
                } );
            </script>
        </div>
    </div>

    <div class="pull-right" >
        <img src="${returnImgUrl}">
        <a class="action" href="${loadersUrl}"> Connections</a>
        /
        <a class="action" href="/.."> QuantServer Home</a>
    </div>

    <script type="application/javascript">
        function activateTab(tab){
            $('.nav-tabs a[href="' + tab + '"]').tab('show');
        };

        var itemName = "connectionTab";
        $(document).ready(function() {
            if (window.location.hash != null && window.location.hash != '') {
                activaTab(window.location.hash)
            } else {
                var hash = sessionStorage.getItem(itemName);
                if (hash !== null && hash.length > 0) {
                    activateTab(hash);
                } else {
                    activateTab('#Connection')
                }
            }
        });

        $('.nav-tabs li a').click(function (e) {
            sessionStorage.setItem(itemName, e.target.hash);
        });
    </script>

</t:layoutpage>