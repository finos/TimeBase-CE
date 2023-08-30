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
        <h3>
            License Information
        </h3>
        <br/>
        <c:set var="license" value="${model.license}" />
        <c:choose>
            <c:when test="${license != null}">
                <div align="center">
                    <table align="left" class="table table-hover table-striped col">
                        <tbody>
                        <tr>
                            <td align="left">
                                License key:
                            </td>
                            <td align="left">
                                <span style="font-weight: bold;">${license.serial}</span>
                            </td>
                        </tr>
                        <tr>
                            <td align="left">
                                Licensed to:
                            </td>
                            <td align="left">
                            <span style="font-weight: bold;">
                                <c:choose>
                                    <c:when test="${license.clientName != null}">
                                        ${license.clientName}
                                    </c:when>
                                    <c:otherwise>
                                        Unknown client
                                    </c:otherwise>
                                </c:choose>
                            </span>
                            </td>
                        <tr>
                            <td align="left">
                                License expiration time:
                            </td>
                            <td align="left">
                                <span style="font-weight: bold;">${license.expirationTime}</span>
                            </td>
                        </tr>
                        <tr>
                            <td align="left">
                                License valid util:
                            </td>
                            <td align="left">
                                <span style="font-weight: bold;">${license.validUntil()}</span>
                            </td>
                        </tr>
                        <tr>
                            <td align="left">
                                License was last validated:
                            </td>
                            <td align="left">
                                <span style="font-weight: bold;">${model.lastValidateTime}</span>
                            </td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise>
                <div class="label label-danger">Saved license has expired or is invalid.</div>
                <br/>
                <br/>
                <c:url var="revalidateUrl" value="/license/revalidate"/>
                <a href="${revalidateUrl}" role="button" class="btn btn-primary">Revalidate License</a>
                <br/>
                <br/>
            </c:otherwise>
        </c:choose>

        <div class="pull-right">
            <img src="${returnImgUrl}"/>
            <a class="action" href="/.."> QuantServer Home</a>
        </div>
    </div>
    <div class="col-md-1"></div>
</t:layoutpage>