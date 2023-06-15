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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@include file="/WEB-INF/jsp/common/taglibs.jsp"%>

<nav class="navbar navbar-inverse navbar-fixed-top">
	<div class="container">
		<div class="navbar-header">
			<button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1" aria-expanded="false">
				<span class="sr-only">Navigation</span>
				<span class="icon-bar"></span>
				<span class="icon-bar"></span>
				<span class="icon-bar"></span>
			</button>

			<c:url value="/index" var="rootUrl" />
			<a class="navbar-brand" href="${rootUrl}">TimeBase</a>
		</div>

		<div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
			<ul class="nav navbar-nav">
				<c:forEach var="section" items="${model.menuModel.menuSections}">
					<li${section.text eq model.menuModel.currentMenuSection.text ? ' class="active"' : ''}>
						<c:url var="linkUrl" value="/${section.url}"/>
						<a href="${linkUrl}">${section.text}</a>
					</li>
				</c:forEach>
			</ul>

			<ul class="nav navbar-nav navbar-right">
				<li class="nav-item">
					<c:if test="${model.monitor != null}">
						<c:choose>
							<c:when test="${model.monitor.trackMessages == false}">
								<c:url var="trackUrl" value="/track/true"/>
								<c:url var="redButtonUrl" value="/resources/img/redb.png" />
								<a href="${trackUrl}" class="btn-sm"><img src="${redButtonUrl}"> Start Tracking</a>
							</c:when>
							<c:otherwise>
								<c:url var="trackUrl" value="/track/false"/>
								<c:url var="greenButtonUrl" value="/resources/img/greenb.png" />
								<a href="${trackUrl}" class="btn-sm"><img src="${greenButtonUrl}"> Stop Tracking</a>
							</c:otherwise>
						</c:choose>
						<c:url var="trackUrl" value="/track/true"/>
					</c:if>
				</li>
			</ul>
		</div>
	</div>
</nav>
