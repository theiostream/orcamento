fillInfo();

addHeader("Detalhes");
createGraph("ac", "Ações");

addHeader("Participação no Orçamento");
createGraph("or", "Órgãos", 'Orgao');
createGraph("uo", "Unidades Orçamentárias", 'UnidadeOrcamentaria');
createGraph("fn", "Funções", 'Funcao');
createGraph("sfn", "Subfunções", 'Subfuncao');

reloadData("ac", "Acao");
reloadData("or", "Orgao");
reloadData("uo", "UnidadeOrcamentaria");
reloadData("fn", "Funcao");
reloadData("sfn", "Subfuncao");
