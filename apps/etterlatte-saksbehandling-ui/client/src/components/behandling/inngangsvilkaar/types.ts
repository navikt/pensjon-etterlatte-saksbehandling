export enum VilkaarStatus {
  OPPFYLT = 'oppfylt',
  IKKE_OPPFYLT = 'ikke oppfylt',
}

export enum Status {
  DONE = 'done',
  NOT_DONE = 'not done',
}

export enum OpplysningsType {
  avdoed_doedsfall = 'avdoed_doedsfall:v1',
  soeker_foedselsdato = 'soeker_foedselsdato:v1',
  avdoedes_forutgaaende_medlemsskap = 'avdoedes_forutgaaende_medlemsskap:v1',
  relasjon_foreldre = 'soeker_relasjon_foreldre:v1',
  soeker_personinfo = 'soeker_personinfo:v1',
  avdoed_personinfo = 'avdoed_personinfo:v1',
  soeknad_mottatt = 'soeknad_mottatt_dato',
  relasjon_soksken = 'soeker_relasjon_soesken:v1',
  omsorg = 'soker_daglig_omsorg:v1'
}
