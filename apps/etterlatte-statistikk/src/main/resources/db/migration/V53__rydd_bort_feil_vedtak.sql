-- rydder bort noen rader for vedtak som ikke skulle blitt med (kode 7)
delete from maaned_stoenad where sakid in (select sakid from stoenad where id in (31406, 10474, 30609, 14139, 29508));
delete from stoenad where id in (31406, 10474, 30609, 14139, 29508);
