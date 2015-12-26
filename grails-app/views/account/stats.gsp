<head>
    <title>Statistics for ${hr} hours</title>
    <script type='text/javascript' src='https://www.google.com/jsapi'></script>
    <script type='text/javascript'>
        google.load('visualization', '1', {packages:['table']});
        google.setOnLoadCallback(drawTable);
        function drawTable() {
            var data = new google.visualization.DataTable();
            data.addColumn('string', 'Pair');
            data.addColumn('number', 'Profit');
            data.addColumn('number', 'Spent');
            data.addColumn('number', 'Gain');
            data.addColumn('number', 'In Orders');
            data.addColumn('number', 'Fees');
            data.addColumn('number', 'Orders');
            data.addColumn('number', 'Buys');
            data.addColumn('number', 'Sells');
            data.addColumn('number', 'Open');
            data.addColumn('number', 'Cancels');
            data.addColumn('number', 'Vol Purchased');
            data.addColumn('number', 'Vol Sold');
            data.addColumn('number', 'Vol Locked');

            data.addRows([
            <g:each var="r" in="${reportItems}">
                  ['${r.pair}', ${r.profit}, ${r.spent}, ${r.gain}, ${r.pending}, ${r.fees}, ${r.places}, ${r.buys}, ${r.sells}, ${r.open}, ${r.cancels}, ${r.purchased}, ${r.sold}, ${r.locked}],
//                ['Mike',  {v: 10000, f: '$10,000'}, true],
            </g:each>
            ]);

            var table = new google.visualization.Table(document.getElementById('table_div'));

            // Profit Columt Format
            var profitFormatter = new google.visualization.NumberFormat(
                    {negativeColor: 'red', negativeParens: true, fractionDigits: 8});
            profitFormatter.format(data, 1); // Apply formatter to second column

            // Amount Column Format
            var amountFormatter = new google.visualization.NumberFormat(
                    {fractionDigits: 6});
            amountFormatter.format(data, 2); // Apply formatter to second column
            amountFormatter.format(data, 3); // Apply formatter to second column
            amountFormatter.format(data, 4); // Apply formatter to second column
            amountFormatter.format(data, 5); // Apply formatter to second column

            // Volume Columns Format
            var volumeFormatter = new google.visualization.NumberFormat(
                    {fractionDigits: 2});
            volumeFormatter.format(data, 11);
            volumeFormatter.format(data, 12);
            volumeFormatter.format(data, 13);

            table.draw(data, {allowHtml: true, showRowNumber: true, sortColumn: 1, sortAscending: false, height: '480px'});
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
        <div>
            %{--Total: ${reportItems.sum {it.profit}}--}%
        </div>

    </div>
</body>
