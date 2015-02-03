fillInfo();

addHeader("Detalhes");
createGraph("pr", "Programas");
createGraph("ac", "Ações");

addHeader("Participação no Orçamento");
createGraph("or", "Órgãos");
createGraph("uo", "Unidades Orçamentárias");
createGraph("fn", "Funções");

reloadData("pr", "Programa");
reloadData("ac", "Acao");
reloadData("or", "Orgao");
reloadData("uo", "UnidadeOrcamentaria");
reloadData("fn", "Funcao");
