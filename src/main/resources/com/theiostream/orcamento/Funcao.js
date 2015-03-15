fillInfo();

addHeader("Detalhes");
createGraph('sfn', "Subfunções", 'Subfuncao');
createGraph('pr', "Programas", 'Programa');

addHeader("Participação no Orçamento");
createGraph('or', "Órgãos", 'Orgao');

reloadData('or', "Orgao");
reloadData('sfn', "Subfuncao");
reloadData('pr', "Programa");
