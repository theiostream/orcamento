fillInfo();

addHeader('Detalhes');
addProgramaSelector();
createGraph('st', 'Despesas');

addHeader('Participação no Orçamento');
createGraph('or', 'Órgãos');

reloadData('st', 'Subtitulo');
reloadData('or', 'Orgao');
