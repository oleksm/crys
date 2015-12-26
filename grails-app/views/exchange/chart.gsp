<head>
    <script type="text/javascript" src="https://www.google.com/jsapi"></script>
    <script type="text/javascript">
        google.load("visualization", "1", {packages:["corechart"]});
        google.setOnLoadCallback(drawSeriesChart);

        function drawSeriesChart() {

            var data = google.visualization.arrayToDataTable([
                ['Pair', 'Market Velocity', 'Margin Trend', 'Exchange', 'Amount'],
                <g:each var="s" in="${stats}">
                    ['${s.pair.name}', ${s.amtPurchased && s.amtSold ? s.amtPurchased / s.amtSold : 0.0d}, ${s.avgBuyPrice && s.avgSellPrice ? s.avgBuyPrice / s.avgSellPrice : 0.0d}, '${s.pair.exchange.name}', ${s.amtTotal}],
                </g:each>
            ]);

            var options = {
                title: 'Correlation between Market Trend and Velocity within last ${hr} hr (${stats[0].timeStamp})',
                hAxis: {title: 'Market Velocity', logScale: true},
                vAxis: {title: 'Margin Trend', logScale: true},
                chartArea: {width: '85%', height: '85%'},

                bubble: {textStyle: {fontSize: 10}}
            };

            var chart = new google.visualization.BubbleChart(document.getElementById('series_chart_div'));

            // The select handler. Call the chart's getSelection() method
            function selectHandler(e) {
                var selectedItem = chart.getSelection()[0];
                if (selectedItem) {
                    var value = data.getValue(selectedItem.row, 0);
                    window.location.href = '/crys/pair/trade?pair=' + value + '&hr=${hr}'
                }
            }

            // Listen for the 'select' event, and call my function selectHandler() when
            // the user selects something on the chart.
            google.visualization.events.addListener(chart, 'select', selectHandler);

            chart.draw(data, options);
        }

        function drawGradientColorChart() {
            var data = google.visualization.arrayToDataTable([
                ['ID', 'X', 'Y', 'Temperature'],
                ['',   80,  167,      120],
                ['',   79,  136,      130],
                ['',   78,  184,      50],
                ['',   72,  278,      230],
                ['',   81,  200,      210],
                ['',   72,  170,      100],
                ['',   68,  477,      80]
            ]);

            var options = {
                colorAxis: {colors: ['yellow', 'red']}
            };

            var chart = new google.visualization.BubbleChart(document.getElementById('gradient_chart_div'));
            chart.draw(data, options);
        }
    </script>
</head>
<body>
<div class="nav" role="navigation">
    <ul>
        <li><a class="home" href="${createLink(uri: '/')}">Home</a></li>
        <li><a href="${createLink(uri: '/quartz/list')}" >Quartz</a></li>
        <li><g:link action="csv">Export CSV</g:link></li>
    </ul>
</div>
<div class="body">
    <div id="series_chart_div" style="width: 900px; height: 640px;"></div>

    <div class="content scaffold-show" role="main">
        <h1>Pairs Trade Statistics</h1>
        <g:each in="${pair_names}" var="p">
            <g:link controller="pair" action="trade" params="[pair: p, hr: hr]"> [${p}] </g:link>
        </g:each>
    </div>
</div>
