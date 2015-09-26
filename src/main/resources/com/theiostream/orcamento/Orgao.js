fillInfo();

addHeader("Detalhes");
createGraph('uo', "Unidades Orçamentárias", 'UnidadeOrcamentaria');
createGraph('fn', "Funções", 'Funcao');
createGraph('pr', "Programas", 'Programa');
createGraph('gnd', "Grupos de Natureza de Despesa", 'GND');

reloadData('uo', "UnidadeOrcamentaria");
reloadData('fn', "Funcao");
reloadData('pr', "Programa");
reloadData('gnd', 'GND');

