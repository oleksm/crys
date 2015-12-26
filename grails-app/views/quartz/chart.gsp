<head>
    <title>Quartz Chart for ${hr} hours</title>
    <script type="text/javascript" src="https://www.google.com/jsapi?autoload={'modules':[{'name':'visualization',
       'version':'1','packages':['timeline']}]}"></script>
    <script type="text/javascript">

        google.setOnLoadCallback(drawChart);
        function drawChart() {
            var container = document.getElementById('chart_div');
            var chart = new google.visualization.Timeline(container);
            var dataTable = new google.visualization.DataTable();

            dataTable.addColumn({ type: 'string', id: 'Job' });
            dataTable.addColumn({ type: 'string', id: 'Event' });
            dataTable.addColumn({ type: 'date', id: 'Start' });
            dataTable.addColumn({ type: 'date', id: 'End' });

            dataTable.addRows([
                <g:each var="e" in="${events}">
                ["${e.job}", "${e.event}", new Date(${e.start}), new Date(${e.end})],
                </g:each>
            ]);
            var options = {
                height: 510

//                timeline: {singleColor: "#8d8"}
            };
            chart.draw(dataTable, options);
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
    <div id="chart_div"></div>
    <div>
        %{--Total: ${reportItems.sum {it.profit}}--}%
    </div>

</div>
</body>
