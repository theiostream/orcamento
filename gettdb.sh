#!/bin/bash

# gettdb.sh
# update tdb index for orçamento
# (c) 2015 Daniel Ferreira

# Prefix: Website download path
PREFIX="https://www1.siop.planejamento.gov.br/downloads/rdf"

# Year: Current year
YEAR=2014

# loaderpath: path to tdbloader2
LOADERPATH=~/Downloads/apache-jena-2.12.1/bin/tdbloader2

# dlpath: Download path for rdf
DLPATH="./tdbtest/"

function getrdf() {
	echo "******* Generating database for $1"
	
	echo "******* Downloading ${PREFIX}/loa${1}.zip"
	curl -k ${PREFIX}/loa${1}.zip > ${DLPATH}/${1}.zip

	echo "******* Unzipping..."
	unzip -j ${DLPATH}/${1}.zip "loa${1}.nt" -d ${DLPATH}
	rm ${DLPATH}/${1}.zip
	
	echo "******* Generating TDB..."
	~/Downloads/apache-jena-2.12.1/bin/tdbloader2 --loc ${DLPATH}/${1} ${DLPATH}/loa${1}.nt
	rm ${DLPATH}/loa${1}.nt
}

getall() {
	for i in $(seq 2000 $YEAR); do
		getrdf $i
	done
}

if [ "$1" = "all" ]; then getall; fi
getrdf $1
