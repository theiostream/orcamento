fillInfo();

addHeader("Detalhes");
createGraph('uo', "Unidades Orçamentárias", 'UnidadeOrcamentaria');
createGraph('fn', "Funções", 'Funcao');
createGraph('pr', "Programas", 'Programa');

reloadData('uo', "UnidadeOrcamentaria");
reloadData('fn', "Funcao");
reloadData('pr', "Programa");

