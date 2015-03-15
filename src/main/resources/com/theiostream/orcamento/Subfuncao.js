fillInfo();

addHeader("Detalhes");
createGraph("pr", "Programas", 'Programa');
createGraph("ac", "Ações", 'Acao');

addHeader("Participação no Orçamento");
createGraph("or", "Órgãos", 'Orgao');
createGraph("uo", "Unidades Orçamentárias", 'UnidadeOrcamentaria');
createGraph("fn", "Funções", 'Funcao');

reloadData("pr", "Programa");
reloadData("ac", "Acao");
reloadData("or", "Orgao");
reloadData("uo", "UnidadeOrcamentaria");
reloadData("fn", "Funcao");
