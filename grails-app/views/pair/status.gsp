<head>
    <title>Statistics for ${hr} hours</title>
    <script type='text/javascript' src='https://www.google.com/jsapi'></script>
    <script type='text/javascript'>
        google.load('visualization', '1', {packages:['table']});
        google.setOnLoadCallback(drawTable);
        function drawTable() {
            var data = new google.visualization.DataTable();
            data.addColumn('string', 'Pair');
            data.addColumn('string', 'EXC');
            data.addColumn('string', 'Synced');
            data.addColumn('string', 'Slow');
            data.addColumn('string', 'Trade');
            data.addColumn('number', 'Price S');
            data.addColumn('number', 'Min Amt');
            data.addColumn('number', 'Fee');
            data.addColumn('string', 'RefID');

            data.addRows([
                <g:each var="p" in="${pairs}">
                ['${p.name}', '${p.exchange.name}', '${p.synced}', '${p.slow}', '${p.trade}', ${p.minPriceMovement}, ${p.minTradeAmount}, ${p.tradeFee}, '${p.refId}'],
                </g:each>
            ]);

            var table = new google.visualization.Table(document.getElementById('table_div'));

            // Profit Columt Format
//            var profitFormatter = new google.visualization.NumberFormat(
//                    {negativeColor: 'red', negativeParens: true, fractionDigits: 8});
//            profitFormatter.format(data, 1); // Apply formatter to second column
//
//            // Amount Column Format
//            var amountFormatter = new google.visualization.NumberFormat(
//                    {fractionDigits: 6});
//            amountFormatter.format(data, 2); // Apply formatter to second column
//            amountFormatter.format(data, 3); // Apply formatter to second column
//            amountFormatter.format(data, 4); // Apply formatter to second column
//            amountFormatter.format(data, 5); // Apply formatter to second column
//
//            // Volume Columns Format
//            var volumeFormatter = new google.visualization.NumberFormat(
//                    {fractionDigits: 2});
//            volumeFormatter.format(data, 11);
//            volumeFormatter.format(data, 12);
//            volumeFormatter.format(data, 13);

            table.draw(data, {allowHtml: true, showRowNumber: true, sortColumn: 0, sortAscending: true, height: '640px', width: '100%'});
        }
    </script>
</head>

<body>
<div class="nav" role="navigation">
    <ul>
        <li><a class="home" href="${createLink(uri: '/')}">Home</a></li>
        <li><a href="${createLink(uri: '/quartz/list')}" >Quartz</a></li>
    </ul>
</div>
<div class="body">
    <div id='table_div'></div>
</div>
</body>
