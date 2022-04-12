CREATE TABLE grunnlagshendelse(
    behandling UUID NOT NULL,
    opplysning TEXT,
    kilde TEXT,
    opplysningtype TEXT,
    hendelsenummer BIGINT NOT NULL,
    hendelsetype TEXT NOT NULL,
    hendelseref BIGINT,
    PRIMARY KEY(behandling, hendelsenummer)
);

TRUNCATE TABLE vurdertvilkaar;
ALTER TABLE vurdertvilkaar ADD CONSTRAINT fk_vurdertvilkaar_hendlese FOREIGN KEY (behandling, versjon) REFERENCES grunnlagshendelse (behandling, hendelsenummer);
ALTER TABLE grunnlagshendelse ADD CONSTRAINT fk_grunnlagshendelse_self FOREIGN KEY (behandling, hendelseref) REFERENCES grunnlagshendelse (behandling, hendelsenummer);
