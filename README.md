# Orçamento

> Website written in Java (using [Apache Jena](https://jena.apache.org/)) to browse the Brazilian Federal Government's expenses database in RDF format.

## Table of contents

- [Instructions](#instructions)
- [Details](#details)
- [Contributing](#contributing)
- [License](#license)

## Instructions

1. Download Apache Jena 2.1.12 (*Jena 3 is not supported yet!*) binaries [here](http://jena.apache.org) and set `$LOADERPATH` on `gettdb.sh` in order to point to `tdbloader2`.

2. Build the website

  `$ mvn package`
3. Download available RDF files from the Government and create/patch TDBs.

  `$ ./gettdb.sh all`
4. Download inflation data from the Government and populate Inflation.json

  `$ ./inflacao.sh > ./r/static/Inflation.json`
5. Start the website

  `$ ./run`

## Details

> Since this likely only interests Brazilians regarding the credibility of this data, this fragment will be written in Portuguese.

A partir da aprovação da Lei de Acesso à Informação em 2011, a cláusula de que dados orçamentários governamentais deveriam ser publicados em formato legível por máquina começou a ser implementada. De qualquer modo, isso resultou em diversas bases de dados para registrar gastos públicos, sendo as principais:

- Sistema Integrado de Administração Financeira do Governo Federal - SIAFI (Tesouro Nacional)
- Sistema Integrado de Orçamento e Planejamento - SIOP (Ministério do Planejamento)
- Portal da Transparência (CGU)
- Contas Públicas (TCU)

O Portal da Transparência disponibiliza a base de dados SIAFI aberta em formato CSV, enquanto a base de dados SIOP possui um formato RDF que permite a programação de queries SPARQL extremamente convenientes, assim como da navegação mais conveniente por um programa que leia esta base de dados. Por esta razão, a base SIOP foi escolhida para compor este website.

O motivador para este projeto foi o fato de diversas ONGs (Transparência Brasil, Contas Abertas) divulgarem frequentemente notícias sobre gastos governamentais sem permitir que o cidadão realize estas consultas diretamente, medida que permitiria tanto maior controle social da corrupção quanto maior fiscalização da veracidade dos dados divulgados (por mais que seja exemplar a credibilidade dos exemplos de ONG citados).

Os atuais meios de consulta para o cidadão das contas públicas são os portais da Transparência de diversas esferas de governo (sendo o mais notório o do Governo Federal). O Portal oferece transparência exemplar evidenciando despesas ou transferências até o nível de uma transferência bancária para um fornecedor ou prestador de serviços, permitindo um controle social sobre a corrupção ótimo. Outro meio é o portal de contas públicas do TCU, que tem caráter de consulta mais técnica sobre os dados.

Para a análise de determinadas instâncias por parte do cidadão, os portais são inigualáveis no nível de detalhamento fornecido. Já para a análise do cumprimento de promessas eleitorais ou de políticas de governo em nível de planejamento mais amplo, o Portal da Transparência mostra-se mais insuficiente. É com este intuito que programo este website: permitir uma visualização gráfica e intuitiva para o cidadão comum de como o Orçamento Federal está sendo investido em prol da população -- seja como prometido ou não por governantes.

Depois de ter investido já quatro meses neste projeto, descobri a existência de um site de uma ONG que tem o mesmo propósito do que programava (usando, porém, uma outra base de dados fechada para um cidadão qualquer -- o Siga Brasil, do Senado Federal). Trata-se do portal "Orçamento ao seu Alcance", da Inesc. Vi coisas que poderiam ser melhoradas e o portal era open-source, porém o trabalho que já havia sido investido neste projeto, adicionado às conveniências da base de dados SIOP e à minha infamiliaridade com Ruby -- a linguagem na qual aquele website havia sido programada -- me levou a continuar a empreender esforços neste projeto.

## Contributing

> I need a Java project manager to... manage this. Seriously, I'm just adding everything to one package and manually importing stuff with vim and so on. *Please help*.

I'm an iPhone programmer. I can't make websites. I can't program in Java. I can't design straight. 90% of this project is pretty much a glue of Bootstrap, d3.js, random stylesheets, random JavaScript plugins etc. If you have more experience than me on the area (which is absolutely easy) **please** contribute by making this less ugly.

Even though there's this, I think the most worrisome thing in my code is the fact that I use this weird JavaScript element generating system instead of a good templating system like Mustache. Maybe I should use that *through* JavaScript, or find a better way to serve my content on the Java side.

If you'd like to contribute, these [guidelines](master/CONTRIBUTING.md) may help you.

### Reporting a bug

1. Look for any related issues [here](https://github.com/theiostream/orcamento/issues).
2. If you find an issue that seems related, please comment there instead of creating a new issue. If it is determined to be a unique bug, we will let you know that a new issue can be created.
3. If you find no related issue, create a new issue by clicking [here](https://github.com/theiostream/orcamento/issues/new).
If we find an issue that's related, we will reference it and close your issue, showing you where to follow the bug.
4. Tell us important details like what operating system you are using.
5. Include any errors that may be displayed.
6. Update us if you have any new info, or if the problem resolves itself!

### The 5 magic steps

1. Fork it!
2. Create your feature branch: `git checkout -b my-new-feature`
3. Commit your changes: `git commit -m 'Add some feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request :)

## License

[Orçamento](https://github.com/theiostream/orcamento) is distributed under the GNU General Public License, version 3, [available in this repository](master/LICENSE.md). All contributions are assumed to be also licensed under the GPLv3.
