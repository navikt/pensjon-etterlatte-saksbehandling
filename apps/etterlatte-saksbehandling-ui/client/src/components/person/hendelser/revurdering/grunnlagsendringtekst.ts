import { GrunnlagsendringsType } from '~components/person/typer'

interface Grunnlagsendringstekst {
  tittel: string
  beskrivelse: string
}

export const grunnlagsendringstekster = new Map<GrunnlagsendringsType, Grunnlagsendringstekst>([
  [
    GrunnlagsendringsType.DOEDSFALL,
    {
      tittel: 'Dødsfall',
      beskrivelse: 'Dødsfallsbeskrivelse her',
    },
  ],
  [
    GrunnlagsendringsType.UTFLYTTING,
    {
      tittel: 'Utflytting',
      beskrivelse: 'Utflyttingsbeskrivelse',
    },
  ],
  [
    GrunnlagsendringsType.ADRESSE,
    {
      tittel: 'Adresse',
      beskrivelse: 'Adressebeskrivelse',
    },
  ],
  [
    GrunnlagsendringsType.FORELDER_BARN_RELASJON,
    {
      tittel: 'Foreldre-barn-relasjon',
      beskrivelse: 'Foreldre-barn-relasjon-beskrivelse',
    },
  ],
  [
    GrunnlagsendringsType.VERGEMAAL_ELLER_FREMTIDSFULLMAKT,
    {
      tittel: 'Vergemål eller fremtidsfullmakt',
      beskrivelse: 'Vergemål, fremtidsfullmakt, beskrivelse her',
    },
  ],
  [
    GrunnlagsendringsType.SIVILSTAND,
    {
      tittel: 'Sivilstand',
      beskrivelse: 'Sivilstand-beskrivelse',
    },
  ],
  [
    GrunnlagsendringsType.GRUNNBELOEP,
    {
      tittel: 'Grunnbeløp endra',
      beskrivelse: 'Grunnbeløpet veldig endra med ein ganske lang tekst her',
    },
  ],
  [
    GrunnlagsendringsType.INSTITUSJONSOPPHOLD,
    {
      tittel: 'Institusjonsopphold',
      beskrivelse: 'Institusjonsoppholdbeskrivelse her',
    },
  ],
])
