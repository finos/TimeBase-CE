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

<%@include file="/WEB-INF/jsp/common/taglibs.jsp"%>

<head profile="http://www.w3.org/2005/10/profile">
    <c:url value="/resources/img/deltix.ico" var="deltixIco" />
    <c:url value="/resources/css/bootstrap.min.css" var="bootstrapCeruleanCss" />
    <c:url value="/resources/css/jquery.dataTables.min.css" var="dataTablesCss" />
    <c:url value="/resources/css/style.css" var="styleCss" />
    <c:url value="/resources/js/jquery.min.js" var="jqueryJs" />
    <c:url value="/resources/js/bootstrap.min.js" var="bootstrapJs" />
    <c:url value="/resources/js/jquery.dataTables.min.js" var="dataTablesJs" />


    <link rel="icon" type="image/ico" href="${deltixIco}"/>
    <link rel="shortcut icon" href="${deltixIco}">
    <%-- duplicate for IE7 --%>
    <title>${model.title}</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <link rel="stylesheet" href="${bootstrapCeruleanCss}" type="text/css">
    <link rel="stylesheet" href="${dataTablesCss}" type="text/css">
    <link rel="stylesheet" href="${styleCss}" type="text/css">

    <script src="${jqueryJs}"></script>
    <script src="${bootstrapJs}"></script>
    <script src="${dataTablesJs}"></script>
</head>

<%-- js --%>
<script type="application/javascript">
    function getExpandedDetailsSet(itemName) {
        var expandedDetails = localStorage.getItem(itemName);
        if (expandedDetails !== null && expandedDetails.length > 0) {
            return new Set(expandedDetails.split('_;_'));
        }

        return new Set();
    }

    function saveExpandedDetailsSet(itemName, expandedDetailsSet) {
        localStorage.setItem(itemName, Array.from(expandedDetailsSet).join('_;_'));
    }

    function expandDetails(table, expandedDetailsSet, numColumn) {
        var allDataSet = new Set();
        $(table.rows().nodes()).each(function(index, tr) {
            var row = table.row(tr);
            var rowData = row.data()[numColumn];
            allDataSet.add(rowData);
            if (expandedDetailsSet.has(rowData)) {
                row.child(format(row.data())).show();
                $(tr).addClass('shown');
            }
        });

        // remove not existent values
        expandedDetailsSet.forEach(function(item, index, object) {
            if (!allDataSet.has(item)) {
                object.delete(item);
            }
        });
    }
</script>