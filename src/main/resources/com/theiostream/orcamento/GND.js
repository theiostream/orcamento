fillInfo();

addHeader("Detalhes");

createGraph('or', 'Órgãos', 'Orgao');
createGraph('fn', 'Funções', 'Funcao');

reloadData('or', 'Orgao');
reloadData('fn', 'Funcao');
