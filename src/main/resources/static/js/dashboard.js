(function () {
    'use strict';
    var root = document.getElementById('cashflow-sankey');
    var chart = document.getElementById('sankey-chart');
    if (root && chart) {
        var income = Number(root.dataset.income) || 0;
        var expense = Number(root.dataset.expense) || 0;
        var saved = Math.max(0, income - expense);
        var buckets = [
            { name: 'Taxes', test: /tax|gst|income tax|property tax/i, color: '#C18D52', rows: [], total: 0 },
            { name: 'Fixed & essentials', test: /rent|housing|home|bill|utility|utilities|emi|loan|insurance|subscription|school|education|grocer|food|transport|medical|health/i, color: '#5A8F76', rows: [], total: 0 },
            { name: 'Flexible spending', test: /.*/, color: '#96CDB0', rows: [], total: 0 }
        ];
        root.querySelectorAll('.sankey-source-data span').forEach(function (row) {
            var name = row.dataset.name || 'Other';
            var amount = Number(row.dataset.amount) || 0;
            var bucket = buckets.find(function (item) { return item.test.test(name); }) || buckets[2];
            bucket.rows.push({ name: name, amount: amount }); bucket.total += amount;
        });
        var flows = buckets.filter(function (b) { return b.total > 0; });
        if (saved > 0) flows.push({ name: 'Net savings', total: saved, rows: [], color: '#203B37' });
        var max = Math.max(income, expense, 1), scale = 205 / max;
        var svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute('viewBox', '0 0 820 310'); svg.setAttribute('role', 'presentation');
        function add(tag, attrs, parent) { var el = document.createElementNS(svg.namespaceURI, tag); Object.keys(attrs).forEach(function (key) { el.setAttribute(key, attrs[key]); }); (parent || svg).appendChild(el); return el; }
        var sourceY = 54, sourceHeight = Math.max(12, income * scale);
        add('rect', { x: 20, y: sourceY, width: 18, height: sourceHeight, rx: 8, fill: '#081B1B' });
        add('text', { x: 50, y: sourceY + 8, class: 'sankey-source-label' }).textContent = 'Income';
        add('text', { x: 50, y: sourceY + 26, class: 'sankey-value-label' }).textContent = '₹' + Math.round(income).toLocaleString('en-IN');
        var top = 42, gap = 12, drawable = 230 - gap * Math.max(0, flows.length - 1);
        var sum = flows.reduce(function (n, b) { return n + b.total; }, 0) || 1;
        var cursor = sourceY;
        flows.forEach(function (bucket) {
            var height = Math.max(10, drawable * bucket.total / sum);
            var center = top + height / 2;
            var path = add('path', { d: 'M38 ' + cursor + ' C150 ' + cursor + ', 185 ' + center + ', 264 ' + center, fill: 'none', stroke: bucket.color, 'stroke-width': Math.max(2, Math.min(54, bucket.total * scale)), 'stroke-opacity': '.38', 'stroke-linecap': 'round', tabindex: '0' });
            var tip = add('title', {}, path); tip.textContent = bucket.name + ': ₹' + Math.round(bucket.total).toLocaleString('en-IN');
            cursor += Math.max(2, bucket.total * scale);
            add('rect', { x: 258, y: center - height / 2, width: 16, height: height, rx: 7, fill: bucket.color });
            add('text', { x: 294, y: center - 3, class: 'sankey-group-label' }).textContent = bucket.name;
            add('text', { x: 294, y: center + 15, class: 'sankey-value-label' }).textContent = '₹' + Math.round(bucket.total).toLocaleString('en-IN');
            if (bucket.rows.length) add('text', { x: 294, y: center + 30, class: 'sankey-category-label' }).textContent = bucket.rows.slice(0, 4).map(function (item) { return item.name; }).join(' · ') + (bucket.rows.length > 4 ? ' +' + (bucket.rows.length - 4) : '');
        });
        var net = income - expense;
        add('text', { x: 20, y: 282, class: 'sankey-footnote' }).textContent = net >= 0 ? 'Unallocated after expenses: ₹' + Math.round(net).toLocaleString('en-IN') : 'Expenses exceed recorded income by ₹' + Math.round(-net).toLocaleString('en-IN');
        chart.appendChild(svg);
        if (!income && !expense) chart.innerHTML = '<p class="chart-empty">Add income and expenses to see your monthly flow.</p>';
    }
    var grid = document.getElementById('spending-heatmap');
    var monthLabels = document.getElementById('heatmap-months');
    if (grid) {
        var cells = Array.from(grid.querySelectorAll('.heat-cell'));
        var first = cells.length ? new Date(cells[0].dataset.date + 'T00:00:00').getDay() : 0;
        var mondayOffset = (first + 6) % 7;
        for (var i = 0; i < mondayOffset; i++) { var blank = document.createElement('span'); blank.className = 'heat-cell heat-cell-blank'; grid.insertBefore(blank, grid.firstChild); }
        var previousMonth = '';
        var weeks = Math.ceil((mondayOffset + cells.length) / 7);
        var monthByWeek = Array(weeks).fill('');
        cells.forEach(function (cell, index) {
            var date = new Date(cell.dataset.date + 'T00:00:00');
            var month = date.toLocaleString(undefined, { month: 'short' });
            var week = Math.floor((mondayOffset + index) / 7);
            if (month !== previousMonth && !monthByWeek[week]) monthByWeek[week] = month;
            previousMonth = month;
            var amount = Number(cell.dataset.amount) || 0;
            cell.title = date.toLocaleDateString() + ' · ₹' + amount.toLocaleString('en-IN', { maximumFractionDigits: 2 }) + ' spent';
        });
        if (monthLabels) { monthLabels.style.setProperty('--heatmap-weeks', weeks); monthLabels.innerHTML = monthByWeek.map(function (month) { return '<span>' + month + '</span>'; }).join(''); }
    }
})();
