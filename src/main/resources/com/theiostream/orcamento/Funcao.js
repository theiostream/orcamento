fillInfo();

addHeader("Detalhes");
createGraph('sfn', "Subfunções");
createGraph('pr', "Programas");

addHeader("Participação no Orçamento");
createGraph('or', "Órgãos");

reloadData('or', "Orgao");
reloadData('sfn', "Subfuncao");
reloadData('pr', "Programa");
