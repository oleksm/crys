<%@ page import="tech.oleks.crys.util.GoogleChartUtils" %>
<head>
    <title>${pair.name} Chart ${hr} hr</title>
    <script type="text/javascript" src="https://www.google.com/jsapi"></script>
    <script type="text/javascript" src="http://dygraphs.com/1.0.1/dygraph-combined.js"></script>
    <script type="text/javascript">
        google.load("visualization", "1", {packages:["corechart"]});
        google.setOnLoadCallback(drawChart);
        function drawChart() {

            /* Velocity Chart */

            var data1 = google.visualization.arrayToDataTable([
                ['Time', 'Amount Purchased', 'Amount Sold', { role: 'annotation' }],
                <g:each var="stat" in="${stats}">
                  ['${stat.timeStamp.format("h")}', ${stat.amtPurchased}, ${stat.amtSold}, ''],
                </g:each>
            ]);

            var options1 = {
                title: '${pair.exchange.name} ${pair.name} Market Velocity ${hr} hrs',
                colors: ['#66CC33', '#CC0000'],
                isStacked: true
            };
            new google.visualization.ColumnChart(document.getElementById('amt_chart')).draw(data1, options1);

            /* Prices Chart */

            var data2 = google.visualization.arrayToDataTable([
                ['Time', 'Min Price', 'Max Price', 'Avg Price', 'Avg Sell Price', 'Avg Buy Price'],
                <g:each var="stat" in="${stats}">
                [new Date(${tech.oleks.crys.util.GoogleChartUtils.date(stat.timeStamp)}), ${stat.minPrice ?: 'null'}, ${stat.maxPrice?: 'null'}, ${stat.avgPrice?: 'null'}, ${stat.avgSellPrice?: 'null'}, ${stat.avgBuyPrice?: 'null'}],
                </g:each>
            ]);

            %{--var options2 = {--}%
                %{--title: '${pair.exchange.name} ${pair.name} Prices ${hr} hr',--}%
                %{--interpolateNulls: true,--}%
                %{--strictFirstColumnType: false,--}%
                %{--hAxis: {title: 'Time',  titleTextStyle: {color: '#333'}},--}%
                %{--curveType: 'function',--}%
                %{--series: {--}%
                    %{--0: {color: '#FF0000', pointShape: 'circle', pointSize: 3},--}%
                    %{--1: {color: '#33CC00', pointShape: 'circle', pointSize: 3},--}%
                    %{--2: {color: '#0099FF', pointShape: 'circle', pointSize: 3},--}%
                    %{--3: {color: '#FFCC99', pointShape: 'circle', pointSize: 3},--}%
                    %{--4: {color: '#CCFFCC', pointShape: 'circle', pointSize: 3}--}%
                %{--}--}%
            %{--};--}%
            %{--new google.visualization.AreaChart(document.getElementById('prices_chart')).draw(data2, options2);--}%
        }
    </script>
    <script>

        function data_temp() {
            return "" +
            "Time,${pair.exchange.name} ${pair.name}\n" +
            <g:each var="stat" in="${stats}">
            "${stat.timeStamp.format("HHmmss")},${stat.minPrice};${stat.avgPrice};${stat.maxPrice}\n" +
            </g:each>
            ""
        }

        $(document).ready(function () {
                    g2 = new Dygraph(
                            document.getElementById("prices_chart"),
                            data_temp,
                            {
                                rollPeriod: 14,
                                showRoller: true,
                                customBars: true,
                                title: '${pair.exchange.name} ${pair.name} Prices ${hr} hr',
                                ylabel: 'Price',
                                legend: 'always',
                                labelsDivStyles: { 'textAlign': 'right' },
                                showRangeSelector: true,
                                rangeSelectorHeight: 30,
                                rangeSelectorPlotStrokeColor: 'yellow',
                                rangeSelectorPlotFillColor: 'lightyellow'
                            });
                }
        );
    </script>

</head>
<body>
    <div class="nav" role="navigation">
        <ul>
            <li><a class="home" href="${createLink(uri: '/')}">Home</a></li>
            <li><a href="${createLink(uri: '/quartz/list')}" >Quartz</a></li>
            <li><g:link controller="pair" action="trade" params="[pair: pair.name, hr: hr]">Trades</g:link></li>

        </ul>
    </div>
    <div class="body">
        <div id="amt_chart" style="width: 960px; height: 600px;"></div>
        <div id="prices_chart" style="width: 960px; height: 600px;"></div>
    </div>
</body>