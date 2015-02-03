fillInfo();

addHeader("Detalhes");
createGraph('uo', "Unidades Orçamentárias");
createGraph('fn', "Funções");
createGraph('pr', "Programas");

reloadData('uo', "UnidadeOrcamentaria");
reloadData('fn', "Funcao");
reloadData('pr', "Programa");

