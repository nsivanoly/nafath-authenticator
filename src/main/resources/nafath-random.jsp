<%@ page import="org.owasp.encoder.Encode" %>
<%@ page import="java.io.File" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="layout" uri="org.wso2.identity.apps.taglibs.layout.controller" %>
<%@ page import="com.client.custom.nafath.authenticator.util.CheckNafathRequestStatus" %>
<%@ include file="includes/localize.jsp" %>
<%@ include file="includes/init-url.jsp" %>

<%
    // Check if the request is an AJAX call
    String ajax = request.getParameter("ajax");

    if ("true".equals(ajax)) {
        String sessionDataKey = request.getParameter("sessionDataKey");
        CheckNafathRequestStatus checker = new CheckNafathRequestStatus();
        String status = checker.getRequestStatus(sessionDataKey);

        response.setContentType("text/plain");
        response.getWriter().write(status);
        return; // Stop further execution to avoid generating the rest of the page
    }
%>

<%-- Branding Preferences --%>
<jsp:directive.include file="includes/branding-preferences.jsp"/>

<html>
    <head>
        <%-- header --%>
        <%
            File headerFile = new File(getServletContext().getRealPath("extensions/header.jsp"));
            if (headerFile.exists()) {
        %>
        <jsp:include page="extensions/header.jsp"/>
        <% } else { %>
        <jsp:include page="includes/header.jsp"/>
        <% } %>

        <%-- analytics --%>
        <%
            File analyticsFile = new File(getServletContext().getRealPath("extensions/analytics.jsp"));
            if (analyticsFile.exists()) {
        %>
            <jsp:include page="extensions/analytics.jsp"/>
        <% } else { %>
            <jsp:include page="includes/analytics.jsp"/>
        <% } %>
    </head>

    <body class="login-portal layout sms-otp-portal-layout">

        <% if (new File(getServletContext().getRealPath("extensions/timeout.jsp")).exists()) { %>
            <jsp:include page="extensions/timeout.jsp"/>
        <% } else { %>
            <jsp:include page="util/timeout.jsp"/>
        <% } %>

        <layout:main layoutName="<%= layout %>" layoutFileRelativePath="<%= layoutFileRelativePath %>" data="<%= layoutData %>" >
            <layout:component componentName="ProductHeader">
                <%-- product-title --%>
                <%
                    File productTitleFile = new File(getServletContext().getRealPath("extensions/product-title.jsp"));
                    if (productTitleFile.exists()) {
                %>
                <jsp:include page="extensions/product-title.jsp"/>
                <% } else { %>
                <jsp:include page="includes/product-title.jsp"/>
                <% } %>
            </layout:component>
            <layout:component componentName="MainSection">
              <div class="ui segment">
                      <%-- page content --%>
                      <h2>
                        <%= i18n(resourceBundle, customText, "NAFATH Verification") %>
                      </h2>
                       
                      <form id="nafathForm" action="../commonauth" method="POST">
                        <div class="segment-form">
                            <div class="random-container">
                                <h4>Enter this text in the Nafath application on your phone</h4>
                                <div class="random">
                                    <h5>Random Text: <%= Encode.forHtml(request.getParameter("random")) %></h5>
                                    <h5>Status: <span id="status">Loading...</span></h5> 
                                    
                                    <input class="form-control" type="hidden" name="status" id="statusField"/>
                                    <input class="form-control" type="hidden" id="sessionDataKey" name="sessionDataKey"
                                    value='<%= Encode.forHtmlAttribute(request.getParameter("sessionDataKey")) %>'/>
                                </div> 
                            </div>
                        </div>
                    </form>
                    
                  </div>
            </layout:component>
            <layout:component componentName="ProductFooter">
                <%-- product-footer --%>
                <%
                    File productFooterFile = new File(getServletContext().getRealPath("extensions/product-footer.jsp"));
                    if (productFooterFile.exists()) {
                %>
                <jsp:include page="extensions/product-footer.jsp"/>
                <% } else { %>
                <jsp:include page="includes/product-footer.jsp"/>
                <% } %>
            </layout:component>
            <layout:dynamicComponent filePathStoringVariableName="pathOfDynamicComponent">
                <jsp:include page="${pathOfDynamicComponent}" />
            </layout:dynamicComponent>
        </layout:main>

        <%-- footer --%>
        <%
            File footerFile = new File(getServletContext().getRealPath("extensions/footer.jsp"));
            if (footerFile.exists()) {
        %>
        <jsp:include page="extensions/footer.jsp"/>
        <% } else { %>
        <jsp:include page="includes/footer.jsp"/>
        <% } %>

        <script type="text/javascript">
            function fetchStatus() {
                const sessionDataKey = document.getElementById("sessionDataKey").value;
        
                const xhr = new XMLHttpRequest();
                xhr.open("GET", "./nafath-random.jsp?ajax=true&sessionDataKey=" + encodeURIComponent(sessionDataKey), true);
        
                xhr.onreadystatechange = function () {
                    if (xhr.readyState === 4) {
                        if (xhr.status === 200) {
                            try {
                                const status = xhr.responseText.trim();

                                document.getElementById('status').innerText = status;
                                document.getElementById('statusField').value = status;
        
                                
                                if (status === "COMPLETED" || status === "EXPIRED" || status === "REJECTED") {
                                    document.getElementById('nafathForm').submit();
                                    if (statusInterval) {
                                        clearInterval(statusInterval);  
                                    }
                                }
                            } catch (error) {
                                console.error("Error parsing response: ", error);
                            }
                        } else {
                            console.error("AJAX request failed. Status: " + xhr.status);
                        }
                    }
                };
        
                xhr.send();
            }
            
            window.onload = function() {
                fetchStatus();
                setInterval(fetchStatus, 5000);
            };
        </script>
        
    </body>
</html>



