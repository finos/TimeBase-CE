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

    <h3>
        Open Cursors (<fmt:formatDate pattern="HH:mm:ss.SSS" value="${model.currentDate}"/>)
    </h3>
    <table id="cursorsTable" class="table table-hover table-striped table-bordered" >
        <thead>
        <tr>
            <th>Index</th>
            <th>Details</th>
            <th>Id</th>
            <th>Stream(s)</th>
            <th>Application</th>
            <th>User</th>
            <th>Options</th>
            <th title="Instruments, Types">Filter</th>
            <th>Opened</th>
            <th>Last Reset</th>
            <th># Msgs</th>
            <th>Last Msg Time</th>
            <th>Last next() Time</th>
        </tr>
        </thead>

        <tbody>
            <c:forEach items="${model.monitor.openCursors}" var="cursor">
                <tr>
                    <td>${cursor.id}</td>
                    <td></td>
                    <c:url var="cursorUrl" value="cursors/${cursor.id}" />
                    <td>
                        <a href="${cursorUrl}">${cursor.id}</a>
                    </td>
                    <td>
                        <c:forEach items="${cursor.sourceStreamKeys}" var="sk">
                            <c:out value="${sk}"/>
                        </c:forEach>
                    </td>

                    <td>${cursor.application}</td>
                    <td>${cursor.user}</td>

                    <td>
                        <c:set var="options" value="${cursor.options}"/>
                        <c:if test="${options.raw}">Raw;</c:if>
                        <c:if test="${options.live}">Live;</c:if>
                        <c:if test="${options.reversed}">Reversed;</c:if>
                        <c:if test="${options.allowLateOutOfOrder}">Unordered;</c:if>
                        <c:if test="${options.channelPerformance == ChannelPerformance.MIN_LATENCY}">Minimize Latency;</c:if>
                        <c:if test="${options.realTimeNotification}">Real-time;</c:if>
                    </td>

                    <td align="center">
                        Entities:
                        <code>
                        <c:choose>
                            <c:when test="${cursor.isAllEntitiesSubscribed()}">ALL</c:when>
                            <c:otherwise>
                                <c:set var="subscribedEntities" value="${cursor.getSubscribedEntities()}"/>
                                <c:forEach items="${subscribedEntities}" var="iid">
                                    [<c:out value="${iid}"/>];
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                        </code>
                        <br/>
                        Types:
                        <code>
                        <c:choose>
                            <c:when test="${cursor.isAllTypesSubscribed()}">ALL</c:when>
                            <c:otherwise>
                                <c:set var="subscribedTypes" value="${cursor.getSubscribedTypes()}"/>
                                <c:forEach items="${subscribedTypes}" var="type">
                                    [<c:out value="${type}"/>];
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                        </code>
                    </td>

                    <td>
                        <fmt:formatDate pattern="${model.getFormat(cursor.openDate)}" value="${cursor.openDate}"/>
                    </td>

                    <td>
                        <fmt:formatDate pattern="${model.getFormat(cursor.lastResetDate)}" timeZone="UTC" value="${cursor.lastResetDate}"/>
                    </td>

                    <td align="right">
                        ${cursor.totalNumMessages}
                    </td>

                    <td>
                        <fmt:formatDate pattern="${model.getFormat(cursor.lastMessageDate)}" timeZone="UTC" value="${cursor.lastMessageDate}"/>
                    </td>
                    <td>
                        <fmt:formatDate pattern="${model.getFormat(cursor.lastMessageSysDate)}" value="${cursor.lastMessageSysDate}"/>
                    </td>
                </tr>
            </c:forEach>
        </tbody>
    </table>

    <div class="pull-right">
        <img src="${returnImgUrl}">
        <a class="action" href="/.."> QuantServer Home</a>
    </div>

    <script type="application/javascript">
        function format ( d ) {
            return '<div class="col-md-8">'+
                '<table class="table">'+
                '<tr>'+
                '<th>Cursor ID:</th>'+
                '<td>' + d[2] + '</td>'+
                '</tr>'+
                '<tr>'+
                '<th>Subscription:</th>'+
                '<td>' + d[7] + '</td>'+
                '</tr>'+
                '<tr>'+
                '<tr>'+
                '<th>Opened:</th>'+
                '<td>' + d[8] + '</td>'+
                '</tr>'+
                '<tr>'+
                '<th>Last Reset:</th>'+
                '<td>' + d[9] + '</td>'+
                '</tr>'+
                '<tr>'+
                '<th>Last Msg Time:</th>'+
                '<td>' + d[11] + '</td>'+
                '</tr>'+
                '<tr>'+
                '<th>Last next() Time:</th>'+
                '<td>' + d[12] + '</td>'+
                '</tr>'+
                '</table>'+
                '</div>';
        }

        $(document).ready(function() {
            var table = $('#cursorsTable').DataTable( {
                "paginate": false,
                'columnDefs' : [
                    { 'className': 'details-control', "orderable": false, 'targets': 1},
                    { 'visible': false, 'targets': [0, 7, 8, 9, 11, 12] }
                ],
                "order": [[2, 'asc']],
                "stateSave": true
            });

            var itemName = "cursorDetails";
            var openedDetailsSet = getExpandedDetailsSet(itemName);
            expandDetails(table, openedDetailsSet, 0);
            saveExpandedDetailsSet(itemName, openedDetailsSet);
            $('#cursorsTable tbody').on('click', 'td.details-control', function () {
                var tr = $(this).closest('tr');
                var row = table.row( tr );
                var index = row.data()[0];

                if ( row.child.isShown() ) {
                    row.child.hide();
                    tr.removeClass('shown');

                    openedDetailsSet.delete(index);
                    saveExpandedDetailsSet(itemName, openedDetailsSet);
                } else {
                    row.child( format(row.data()) ).show();
                    tr.addClass('shown');

                    openedDetailsSet.add(index);
                    saveExpandedDetailsSet(itemName, openedDetailsSet);
                }
            } );

        } );
    </script>

</t:layoutpage>