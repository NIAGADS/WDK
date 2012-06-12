<%@ taglib prefix="imp" tagdir="/WEB-INF/tags/imp" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="wdkModel" value="${applicationScope.wdkModel}" />
<c:set var="wdkUser" value="${sessionScope.wdkUser}" />

<script type="text/javascript">
$(document).ready(function() {
    $("#search_history table.datatables").dataTable( {
        "bAutoWidth": false,
        "bJQueryUI": true,
        "bScrollCollapse": true,
        "aoColumns": [ { "bSortable": false }, 
                       null, 
                       { "bSortable": false },
                       // { "bSortable": false },
                       // { "bSortable": false },
                       { "bSortable": false },
                       null, 
                       null, 
                       null, 
                       null, 
                       { "bSortable": false } ],
        "aaSorting": [[ 5, "desc" ]]
    } );
} );
</script>

<imp:strategyHistory model="${wdkModel}" user="${wdkUser}" />
