
create TABLE institusjonsopphold_personer(
    person_ident TEXT,
    status TEXT,
    PRIMARY KEY (person_ident)
);

create TABLE institusjonsopphold_hentet(
    opphold_id BIGINT NOT NULL,
    person_ident TEXT,
    institusjonstype TEXT,
    startdato DATE,
    faktisk_sluttdato DATE,
    forventet_sluttdato DATE,
    PRIMARY KEY (opphold_id)
);