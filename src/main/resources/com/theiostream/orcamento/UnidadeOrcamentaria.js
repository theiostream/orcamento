fillInfo();

addHeader("Detalhes");
createGraph('fn', "Funções", 'Funcao');
createGraph('pr', "Programas", 'Programa');
createGraph('ac', "Ações", 'Acao');

addHeader("Participação no Orçamento");
createGraph('gnd', "Grupos de Natureza de Despesa", 'GND');

reloadData('fn', "Funcao");
reloadData('pr', "Programa");
reloadData('ac', "Acao");
reloadData('gnd', 'GND');

