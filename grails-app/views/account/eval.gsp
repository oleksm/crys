<head>
</head>
<body>
<div class="nav" role="navigation">
    <ul>
        <li><a class="home" href="${createLink(uri: '/')}">Home</a></li>
        <li><a href="${createLink(uri: '/quartz/list')}" >Quartz</a></li>
        <li><g:link controller="exchange" action="chart">Pairs</g:link></li>
    </ul>
</div>
<div class="body">
    <g:if test="${accounts}">
        <div class="content scaffold-show" role="main">
            <h1>Choose Account</h1>
            <g:each in="${accounts}" var="acc">
                <g:link action="eval" params="[aid: acc.id]">[${acc.name}] </g:link>
            </g:each>
        </div>
    </g:if>
    <g:if test="${pairs}">
            <div class="content scaffold-show" role="main">
            <h1>Choose Pair</h1>
            <g:each in="${pairs}" var="p">
                <g:link action="eval" params="[aid: acc.id, pid: p.id]">[${p.name}] </g:link>
            </g:each>
        </div>
    </g:if>
    <g:if test="${bid}">
        <div class="content scaffold-show" role="main">
            <h1>Bid Evaluation Results for ${acc.name} - ${pair.name} </h1>
            Result: ${bid.result} <br/>
            <g:each in="${bid.report}" var="l">
                <br/>${l}
            </g:each>
        </div>
        <br/>
    </g:if>
    <g:if test="${ask}">
        <div class="content scaffold-show" role="main">
            <h1>Ask Evaluation Results for ${acc.name} - ${pair.name} </h1>
            Result: ${ask.result} <br/>
            <g:each in="${ask.report}" var="l">
                <br/>${l}
            </g:each>
        </div>
        <br/>
    </g:if>
    <g:if test="${sell}">
        <div class="content scaffold-show" role="main">
            <h1>Sell Evaluation Results for ${acc.name} - ${pair.name} </h1>
            Result: ${sell.result} <br/>
            <g:each in="${sell.report}" var="l">
                <br/>${l}
            </g:each>
        </div>
        <br/>
    </g:if>
    <g:if test="${watch}">
        <div class="content scaffold-show" role="main">
            <h1>Watch Expressions for ${acc.name} - ${pair.name} </h1>
            Result: ${watch.result} <br/>
            <g:each in="${watch.report}" var="l">
                <br/>${l}
            </g:each>
        </div>
        <br/>
        <g:link action="eval" params="[aid: acc.id]">[ Choose Other Pair ]</g:link>
        <g:link action="eval">[ Choose Other Account ]</g:link>
    </g:if>
</div>
