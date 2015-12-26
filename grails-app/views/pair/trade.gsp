<head>
    <title>${pair} Trades ${hr} hr</title>
    <script type="text/javascript">
        google.load("visualization", "1", {packages:["corechart"]});
        google.setOnLoadCallback(drawChart);
        function drawChart() {
            var data = google.visualization.arrayToDataTable([
                [<g:each status="ii" var="t" in="${header}">'${t}'${ii < result.size() - 1 ? ',': ''}</g:each>],
                <g:each var="row" in="${result}">
                  [new Date(${row.key}),
                  <g:each status="i" var="cell" in="${row.value}">${cell ?: 'null'}${i < row.value.size() - 1 ? ',': ''}</g:each>],
                </g:each>
            ]);

            var options = {
                title: '${pair} Trades ${hr} hr',
                interpolateNulls: true,
                strictFirstColumnType: false,
                hAxis: {title: 'Time',  titleTextStyle: {color: '#333'}},
                series: {
                    0: {color: '#FF0000', pointShape: 'circle', pointSize: 2},
                    1: {color: '#99CCFF', pointShape: 'circle', pointSize: 2},
                    2: {color: '#FF9900', pointShape: 'circle', pointSize: 2},
                    3: {color: '#00CC00', pointShape: 'circle', pointSize: 2}
                }
            };

            var chart = new google.visualization.AreaChart(document.getElementById('chart_div'));
            chart.draw(data, options);
        }
    </script>
</head>

<body>
    <div class="nav" role="navigation">
        <ul>
            <li><a class="home" href="${createLink(uri: '/')}">Home</a></li>
            <li><a href="${createLink(uri: '/quartz/list')}" >Quartz</a></li>
            <li><g:link controller="exchange" action="chart">Pairs</g:link></li>
            <li><g:link class="${hr == 1 ? 'selected' : ''}" action="trade" params="[pair: pair, hr: 1]">1H</g:link></li>
            <li><g:link class="${hr == 2 ? 'selected' : ''}" action="trade" params="[pair: pair, hr: 2]">2H</g:link></li>
            <li><g:link class="${hr == 4 ? 'selected' : ''}" action="trade" params="[pair: pair, hr: 4]">4H</g:link></li>
            <li><g:link class="${hr == 8 ? 'selected' : ''}" action="trade" params="[pair: pair, hr: 8]">8H</g:link></li>
            <li><g:link class="${hr == 12 ? 'selected' : ''}" action="trade" params="[pair: pair, hr: 12]">12H</g:link></li>
            <li><g:link class="${hr == 24 ? 'selected' : ''}" action="trade" params="[pair: pair, hr: 24]">24H</g:link></li>
            <li><g:link class="${hr == 48 ? 'selected' : ''}" action="trade" params="[pair: pair, hr: 48]">2D</g:link></li>
            <li><g:link class="${hr == 168 ? 'selected' : ''}" action="trade" params="[pair: pair, hr: 168]">1W</g:link></li>
        </ul>
    </div>
    <div class="body">
        <div id="chart_div" style="width: 960px; height: 600px;"></div>

        <div class="content scaffold-show" role="main">
            <g:each var="p" in="${pairs}" status="i">
                <h1>${p.exchange.name} ${p.name}</h1>
                <ol class="property-list">
                    <g:each in="${stats[i]}" var="stat">
                        <li class="fieldcontain">
                            <span class="property-label">${stat.key}</span>

                            <span class="property-value" aria-labelledby="created-label">${stat.value}</span>
                        </li>
                    </g:each>
                    <li class="fieldcontain">
                        <span class="property-label">Evaluate</span>

                        <span class="property-value" aria-labelledby="created-label">
                            <g:each in="${p.exchange.accounts.findAll {it.profile}}" var="acc">
                                <g:link controller="account" action="eval" params="[aid: acc.id, pid: p.id]">[ ${acc.name} ] </g:link>
                            </g:each>
                        </span>
                    </li>
                </ol>
            </g:each>
            <h1>More Pairs</h1>
            <g:each in="${allPairs}" var="p">
                <g:link controller="pair" action="trade" params="[pair: p, hr: hr]"> [${p}] </g:link>
            </g:each>
        </div>

    </div>
</body>
