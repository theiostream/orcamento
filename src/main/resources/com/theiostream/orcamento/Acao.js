fillInfo();

addHeader('Detalhes');
addProgramaSelector();
createGraph('st', 'Despesas', 'Subtitulo');

addHeader('Participação no Orçamento');
createGraph('or', 'Órgãos', 'Orgao');
createGraph('gnd', "Grupos de Natureza de Despesa", 'GND');

reloadData('st', 'Subtitulo');
reloadData('or', 'Orgao');
reloadData('gnd', 'GND');
