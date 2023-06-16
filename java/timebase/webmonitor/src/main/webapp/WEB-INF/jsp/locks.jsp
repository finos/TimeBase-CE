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
        Acquired Locks (<fmt:formatDate pattern="HH:mm:ss.SSS" value="${model.currentDate}"/>)
    </h3>

    <table id="locksTable" class="table table-hover table-striped table-bordered" >
        <thead>
            <tr>
                <th>Details</th>
                <th>Id</th>
                <th>GUID</th>
                <th>Type</th>
                <th>Client Id</th>
                <th>Stream</th>
                <th>Application</th>
                <th>User</th>
                <th>Host</th>
            </tr>
        </thead>

        <tbody>
            <c:forEach items="${model.monitor.locks}" var="lock">
                <c:url var="connectionUrl" value="/connection">
                    <c:param name="clientId" value="${lock.clientId}" />
                </c:url>
                <tr>
                    <td></td>
                    <td>${lock.id}</td>
                    <td>${lock.guid}</td>
                    <td>${lock.type}</td>
                    <td><a href="${connectionUrl}">${lock.clientId}</a></td>
                    <td>${lock.streamKey}</td>
                    <td><a href="${connectionUrl}">${lock.application}</a></td>
                    <td>${lock.user}</td>
                    <td>${lock.host}</td>
                </tr>
            </c:forEach>
        </tbody>
    </table>

    <div class="pull-right">
        <img src="${returnImgUrl}"/>
        <a class="action" href="/.."> QuantServer Home</a>
    </div>

    <script type="application/javascript">
        function format ( d ) {
            return '<div class="col-md-6">'+
                '<table class="table">'+
                '<tr>'+
                '<th>Lock ID:</th>'+
                '<td>' + d[1] + '</td>'+
                '</tr>'+
                '<tr>'+
                '<th>GUID:</th>'+
                '<td>' + d[2] + '</td>'+
                '</tr>'+
                '<tr>'+
                '<th>Client ID:</th>'+
                '<td>' + d[4] + '</td>'+
                '</tr>'+
                '</table>'+
                '</div>';
        }

        $(document).ready(function() {
            var table = $('#locksTable').DataTable( {
                "paginate": false,
                'columnDefs' : [
                    { 'className': 'details-control', "orderable": false, 'targets': 0},
                    { 'visible': false, 'targets': [4] }
                ],
                "order": [[1, 'asc']],
                "stateSave": true
            });

            var itemName = "lockDetails";
            var openedDetailsSet = getExpandedDetailsSet(itemName);
            expandDetails(table, openedDetailsSet, 1);
            saveExpandedDetailsSet(itemName, openedDetailsSet);
            $('#locksTable tbody').on('click', 'td.details-control', function () {
                var tr = $(this).closest('tr');
                var row = table.row( tr );
                var index = row.data()[1];

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