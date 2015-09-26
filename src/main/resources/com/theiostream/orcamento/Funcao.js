fillInfo();

addHeader("Detalhes");
createGraph('sfn', "Subfunções", 'Subfuncao');
createGraph('pr', "Programas", 'Programa');

addHeader("Participação no Orçamento");
createGraph('or', "Órgãos", 'Orgao');
createGraph('gnd', "Grupos de Natureza de Despesa", 'GND');

reloadData('or', "Orgao");
reloadData('sfn', "Subfuncao");
reloadData('pr', "Programa");
reloadData('gnd', 'GND');
