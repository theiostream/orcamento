fillInfo();

addHeader("Detalhes");
createGraph('fn', "Funções", 'Funcao');
createGraph('pr', "Programas", 'Programa');
createGraph('ac', "Ações", 'Acao');

reloadData('fn', "Funcao");
reloadData('pr', "Programa");
reloadData('ac', "Acao");
