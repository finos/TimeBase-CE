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

    <h3>
        Open Loaders (<fmt:formatDate pattern="HH:mm:ss.SSS" value="${model.currentDate}"/>)
    </h3>
    <table id="loadersTable" class="table table-hover table-striped table-bordered">
        <thead>
        <tr>
            <th>Index</th>
            <th>Details</th>
            <th>Id</th>
            <th>Stream</th>
            <th>Application</th>
            <th>User</th>
            <th>Options</th>
            <th>Opened</th>
            <th># Msgs</th>
            <th>Last Msg Time</th>
            <th>Last send() Time</th>
        </tr>
        </thead>

        <tbody>
            <c:forEach var="loader" items="${model.monitor.openLoaders}">
                <tr>
                    <td>${loader.id}</td>
                    <td></td>
                    <c:url var="loaderUrl" value="loaders/${loader.id}" />
                    <td><a href="${loaderUrl}">${loader.id}</a></td>
                    <td>${loader.targetStreamKey}</td>
                    <td>${loader.application}</td>
                    <td>${loader.user}</td>
                    <td>
                        <c:set var="options" value="${loader.options}"/>
                        <c:if test="${options.raw}">Raw;</c:if>
                        <c:if test="${options.globalSorting}">Sorted;</c:if>
                        <c:if test="${options.channelPerformance == ChannelPerformance.MIN_LATENCY}">Minimize Latency</c:if>
                    </td>
                    <td>
                        <fmt:formatDate pattern="${model.getFormat(loader.openDate)}" value="${loader.openDate}"/>
                    </td>
                    <td align="right">
                        <fmt:formatNumber groupingUsed="true" value="${loader.totalNumMessages}"/>
                    </td>
                    <td>
                        <fmt:formatDate pattern="${model.getFormat(loader.lastMessageDate)}" timeZone="UTC" value="${loader.lastMessageDate}"/>
                    </td>
                    <td>
                        <fmt:formatDate pattern="${model.getFormat(loader.lastMessageSysDate)}" value="${loader.lastMessageSysDate}"/>
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
            return '<div class="col-md-6">'+
                '<table class="table">'+
                '<tr>'+
                '<th>Loader ID:</th>'+
                '<td>' + d[2] + '</td>'+
                '</tr>'+
                '<tr>'+
                '<th>Opened:</th>'+
                '<td>' + d[7] + '</td>'+
                '</tr>'+
                '<tr>'+
                '<th>Last Msg Time:</th>'+
                '<td>' + d[9] + '</td>'+
                '</tr>'+
                '<tr>'+
                '<th>Last send() Time:</th>'+
                '<td>' + d[10] + '</td>'+
                '</tr>'+
                '</table>'+
                '</div>';
        }

        $(document).ready(function() {
            var table = $('#loadersTable').DataTable( {
                "paginate": false,
                'columnDefs' : [
                    { 'className': 'details-control', "orderable": false, 'targets': 1},
                    { 'visible': false, 'targets': [0, 7, 9, 10] }
                ],
                "order": [[2, 'asc']],
                "stateSave": true
            });

            var itemName = "loaderDetails";
            var openedDetailsSet = getExpandedDetailsSet(itemName);
            expandDetails(table, openedDetailsSet, 0);
            saveExpandedDetailsSet(itemName, openedDetailsSet);
            $('#loadersTable tbody').on('click', 'td.details-control', function () {
                var tr = $(this).closest('tr');
                var row = table.row(tr);
                var index = row.data()[0];

                if (row.child.isShown()) {
                    row.child.hide();
                    tr.removeClass('shown');

                    openedDetailsSet.delete(index);
                    saveExpandedDetailsSet(itemName, openedDetailsSet);
                } else {
                    row.child(format(row.data())).show();
                    tr.addClass('shown');

                    openedDetailsSet.add(index);
                    saveExpandedDetailsSet(itemName, openedDetailsSet);
                }
            } );

        } );
    </script>

</t:layoutpage>