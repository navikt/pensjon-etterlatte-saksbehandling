-- ryddingen må rekjøres med riktig / oppdatert kode, siden den ikke ryddet riktig
update tilbakestilte_behandlinger set ryddet = false where ryddet = true;