fillInfo();

addHeader('Detalhes');
addProgramaSelector();
createGraph('st', 'Despesas', 'Subtitulo');

addHeader('Participação no Orçamento');
createGraph('or', 'Órgãos', 'Orgao');

reloadData('st', 'Subtitulo');
reloadData('or', 'Orgao');
