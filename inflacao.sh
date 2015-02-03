#!/bin/bash

# inflacao.sh
# Get inflation data from Brazilian Central Bank and print to stdout 

CURRENT_DATE="12%2F2014"
YEAR="2014"

function inflation_year() {
	RQ="aba=1&dataInicial=01%2F${1}&dataFinal=${CURRENT_DATE}&valorCorrecao=1%2C00&idIndice=&nomeIndicePadrao=&selIndice=10764IPC-E"
	curl https://www3.bcb.gov.br/CALCIDADAO/publico/corrigirPorIndice.do?method=corrigirPorIndice -d "${RQ}" 2>/dev/null >inf_curl.txt
	PERCENT=$(perl -ne 'print $1 if /(?<=\>)(.*)(?=\s%)/' inf_curl.txt)
	echo $PERCENT
}

function makejson() {
	STR="{"
	for i in $(seq 2000 $YEAR); do
		INF=$(inflation_year $i)
		INF=${INF//,/\.}
		STR+="\"$i\":$INF,"
	done

	echo "${STR%?}}"
}

makejson
