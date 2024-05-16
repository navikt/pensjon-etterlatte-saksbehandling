import React, { ReactNode, useContext } from 'react'
import {
  FnrTilNavnMapContext,
  formaterFoedselsnummerMedNavn,
  formaterLandList,
  formaterRolle,
  grunnlagsendringsBeskrivelse,
  grunnlagsendringsKilde,
} from '~components/person/hendelser/utils'
import {
  AdresseSamsvar,
  AnsvarligeForeldreSamsvar,
  BarnSamsvar,
  DoedsdatoSamsvar,
  Grunnlagsendringshendelse,
  InstitusjonsoppholdSamsvar,
  Sivilstand,
  SivilstandSamsvar,
  UtlandSamsvar,
  VergemaalEllerFremtidsfullmakt,
  VergemaalEllerFremtidsfullmaktForholdSamsvar,
} from '~components/person/typer'
import { formaterKanskjeStringDatoMedFallback, formaterStringDato } from '~utils/formattering'
import styled from 'styled-components'
import { BodyShort } from '@navikt/ds-react'
import { institusjonstype } from '~components/behandling/beregningsgrunnlag/Insthendelser'
import { Adressevisning } from '~components/behandling/felles/Adressevisning'
import { SakType } from '~shared/types/sak'

const VisAnsvarligeForeldreSamsvar = ({ samsvar }: { samsvar: AnsvarligeForeldreSamsvar }) => {
  const navneMap = useContext(FnrTilNavnMapContext)
  return (
    <GrunnlagSammenligningWrapper>
      <div>
        <Broedtekst>Nytt grunnlag (PDL)</Broedtekst>
        <KortTekst size="small">
          {samsvar.fraPdl?.map((fnr) => formaterFoedselsnummerMedNavn(navneMap, fnr))?.join(', ') ??
            'Ingen ansvarlige foreldre'}
        </KortTekst>
      </div>
      <div>
        <Broedtekst>Eksisterende grunnlag</Broedtekst>
        <KortTekst size="small">
          {samsvar.fraGrunnlag?.map((fnr) => formaterFoedselsnummerMedNavn(navneMap, fnr))?.join(', ') ??
            'Ingen ansvarlige foreldre'}
        </KortTekst>
      </div>
    </GrunnlagSammenligningWrapper>
  )
}

const UtlandSamsvarVisning = ({ samsvar }: { samsvar: UtlandSamsvar }) => {
  return (
    <GrunnlagSammenligningWrapper>
      <div>
        <Broedtekst>Nytt grunnlag (PDL)</Broedtekst>
        <KortTekst size="small">Ut: {formaterLandList(samsvar.fraPdl?.utflyttingFraNorge ?? [])}</KortTekst>
        <KortTekst size="small">Inn: {formaterLandList(samsvar.fraPdl?.innflyttingTilNorge ?? [])}</KortTekst>
      </div>
      <div>
        <Broedtekst>Eksisterende grunnlag</Broedtekst>
        <KortTekst size="small">Ut: {formaterLandList(samsvar.fraGrunnlag?.utflyttingFraNorge ?? [])}</KortTekst>
        <KortTekst size="small">Inn: {formaterLandList(samsvar.fraGrunnlag?.innflyttingTilNorge ?? [])}</KortTekst>
      </div>
    </GrunnlagSammenligningWrapper>
  )
}

const Doedsdato = ({ samsvar }: { samsvar: DoedsdatoSamsvar }) => {
  return (
    <GrunnlagSammenligningWrapper>
      <div>
        <Broedtekst>Nytt grunnlag (PDL)</Broedtekst>
        <KortTekst size="small">{formaterKanskjeStringDatoMedFallback('Ingen', samsvar.fraPdl)}</KortTekst>
      </div>
      <div>
        <Broedtekst>Eksisterende grunnlag</Broedtekst>
        <KortTekst size="small">{formaterKanskjeStringDatoMedFallback('Ingen', samsvar.fraGrunnlag)}</KortTekst>
      </div>
    </GrunnlagSammenligningWrapper>
  )
}

const Adresse = ({ adresse }: { adresse: AdresseSamsvar }) => {
  return (
    <GrunnlagSammenligningWrapper>
      <div>
        <Broedtekst>Nytt grunnlag (PDL)</Broedtekst>
        <KortTekst as="div">
          <Adressevisning adresser={adresse.fraPdl} soeknadsoversikt={false} />
        </KortTekst>
      </div>
      <div>
        <Broedtekst>Eksisterende grunnlag</Broedtekst>
        <KortTekst as="div">
          <Adressevisning adresser={adresse.fraGrunnlag} soeknadsoversikt={false} />
        </KortTekst>
      </div>
    </GrunnlagSammenligningWrapper>
  )
}

const Barn = ({ samsvar }: { samsvar: BarnSamsvar }) => {
  const fnrTilNavn = useContext(FnrTilNavnMapContext)
  return (
    <GrunnlagSammenligningWrapper>
      <div>
        <Broedtekst>Nytt grunnlag (PDL)</Broedtekst>
        <KortTekst size="small">
          {samsvar.fraPdl?.map((fnr) => formaterFoedselsnummerMedNavn(fnrTilNavn, fnr)).join(', ') ?? 'Ingen barn'}
        </KortTekst>
      </div>
      <div>
        <Broedtekst>Eksisterende grunnlag</Broedtekst>
        <KortTekst size="small">
          {samsvar.fraGrunnlag?.map((fnr) => formaterFoedselsnummerMedNavn(fnrTilNavn, fnr)).join(', ') ?? 'Ingen barn'}
        </KortTekst>
      </div>
    </GrunnlagSammenligningWrapper>
  )
}

const GrunnlagVergemaal = ({ vergemaal }: { vergemaal: VergemaalEllerFremtidsfullmakt }) => {
  const { vergeEllerFullmektig, embete, type } = vergemaal
  const { navn, omfang, motpartsPersonident, omfangetErInnenPersonligOmraade } = vergeEllerFullmektig
  return (
    <>
      <KortTekst size="small">Embete: {embete ?? 'ikke angitt'}</KortTekst>
      <KortTekst size="small">Type: {type ?? 'ikke angitt'}</KortTekst>

      <KortTekst size="small">
        Omfang: {omfang ?? 'ikke angitt '}
        {omfangetErInnenPersonligOmraade ? 'Er innenfor personlig område' : 'Er ikke innenfor personlig område'}
      </KortTekst>
      <KortTekst size="small">
        Verge: {navn ?? 'ikke angitt'} ({motpartsPersonident})
      </KortTekst>
    </>
  )
}

const VisSivilstand = ({ samsvar }: { samsvar: SivilstandSamsvar }) => {
  const sivilstandWrapper = (sivilstand: Sivilstand, i: number) => {
    return (
      <ListeWrapper key={i}>
        <li>
          <KortTekst size="small">Gyldig fra og med: {sivilstand.gyldigFraOgMed ?? 'ikke angitt'}</KortTekst>
        </li>
        <li>
          <KortTekst size="small">Sivilstatus: {sivilstand.sivilstatus}</KortTekst>
        </li>
        <li>
          <KortTekst size="small">Ektefelle/samboer: {sivilstand.relatertVedSiviltilstand ?? 'ikke angitt'}</KortTekst>
        </li>
        <li>
          <KortTekst size="small">Kilde: {sivilstand.kilde}</KortTekst>
        </li>
      </ListeWrapper>
    )
  }

  return (
    <GrunnlagSammenligningWrapper>
      <div>
        <Broedtekst>Nytt grunnlag (PDL)</Broedtekst>
        <ListeWrapper>
          {samsvar.fraPdl?.map((sivilstand, i) => <li key={i}>{sivilstandWrapper(sivilstand, i)}</li>)}
        </ListeWrapper>
      </div>
      <div>
        <Broedtekst>Eksisterende grunnlag</Broedtekst>
        <ListeWrapper>
          {samsvar.fraGrunnlag?.map((sivilstand, i) => <li key={i}>{sivilstandWrapper(sivilstand, i)}</li>)}
        </ListeWrapper>
      </div>
    </GrunnlagSammenligningWrapper>
  )
}

const Institusjonsopphold = (props: { samsvar: InstitusjonsoppholdSamsvar }) => {
  const { samsvar } = props
  return (
    <>
      <KortTekst size="small">Oppholdstype: {samsvar.oppholdstype}</KortTekst>
      <KortTekst size="small">
        Startdato:{' '}
        {samsvar.oppholdBeriket.startdato ? formaterStringDato(samsvar.oppholdBeriket.startdato) : 'Ingen startdato'}
      </KortTekst>
      <KortTekst size="small">
        Faktisk sluttdato:{' '}
        {samsvar.oppholdBeriket.faktiskSluttdato
          ? formaterStringDato(samsvar.oppholdBeriket.faktiskSluttdato)
          : 'Ingen sluttdato'}
      </KortTekst>
      <KortTekst size="small">
        Forventet sluttdato:{' '}
        {samsvar.oppholdBeriket.forventetSluttdato
          ? formaterStringDato(samsvar.oppholdBeriket.forventetSluttdato)
          : 'Ingen forventet sluttdato'}
      </KortTekst>
      <KortTekst size="small">
        Institusjonstype:{' '}
        {samsvar.oppholdBeriket.institusjonsType
          ? institusjonstype[samsvar.oppholdBeriket.institusjonsType]
          : `Ukjent type ${samsvar.oppholdBeriket.institusjonsType}`}
      </KortTekst>
      <KortTekst size="small"> Institusjonsnavn: {samsvar.oppholdBeriket.institusjonsnavn}</KortTekst>
      <KortTekst size="small">
        Organisasjonsnummer:{' '}
        {samsvar.oppholdBeriket.organisasjonsnummer
          ? samsvar.oppholdBeriket.organisasjonsnummer
          : 'Ingen organisasjonsnummer funnet'}
      </KortTekst>
    </>
  )
}

const Vergemaal = (props: { samsvar: VergemaalEllerFremtidsfullmaktForholdSamsvar }) => {
  return (
    <GrunnlagSammenligningWrapper>
      <div>
        <Broedtekst>Nytt grunnlag (PDL)</Broedtekst>
        {props.samsvar.fraPdl?.map((verge, i) => <GrunnlagVergemaal key={i} vergemaal={verge} />) ?? (
          <KortTekst size="small">Ingen vergemål funnet</KortTekst>
        )}
      </div>
      <div>
        <Broedtekst>Eksisterende grunnlag</Broedtekst>
        {props.samsvar.fraGrunnlag?.map((verge, i) => <GrunnlagVergemaal key={i} vergemaal={verge} />) ?? (
          <KortTekst size="small">Ingen vergemål funnet</KortTekst>
        )}
      </div>
    </GrunnlagSammenligningWrapper>
  )
}

const HendelseDetaljer = ({ hendelse, sakType }: { hendelse: Grunnlagsendringshendelse; sakType: SakType }) => {
  const navneMap = useContext(FnrTilNavnMapContext)
  return (
    <DetaljerWrapper>
      <KortTekst size="small">
        {formaterRolle(sakType, hendelse.hendelseGjelderRolle)}{' '}
        {formaterFoedselsnummerMedNavn(navneMap, hendelse.gjelderPerson)} har{' '}
        {grunnlagsendringsBeskrivelse[hendelse.samsvarMellomKildeOgGrunnlag.type]}
      </KortTekst>
    </DetaljerWrapper>
  )
}

export const HendelseBeskrivelse = ({
  sakType,
  hendelse,
}: {
  hendelse: Grunnlagsendringshendelse
  sakType: SakType
}) => {
  switch (hendelse.samsvarMellomKildeOgGrunnlag.type) {
    case 'UTLAND':
      return (
        <Header>
          <HendelseDetaljer sakType={sakType} hendelse={hendelse} />
          <UtlandSamsvarVisning samsvar={hendelse.samsvarMellomKildeOgGrunnlag} />
        </Header>
      )
    case 'ANSVARLIGE_FORELDRE':
      return (
        <Header>
          <HendelseDetaljer sakType={sakType} hendelse={hendelse} />
          <VisAnsvarligeForeldreSamsvar samsvar={hendelse.samsvarMellomKildeOgGrunnlag} />
        </Header>
      )
    case 'DOEDSDATO':
      return (
        <Header>
          <HendelseDetaljer sakType={sakType} hendelse={hendelse} />
          <Doedsdato samsvar={hendelse.samsvarMellomKildeOgGrunnlag} />
        </Header>
      )
    case 'ADRESSE':
      return (
        <Header>
          <HendelseDetaljer sakType={sakType} hendelse={hendelse} />
          <Adresse adresse={hendelse.samsvarMellomKildeOgGrunnlag} />
        </Header>
      )
    case 'BARN':
      return (
        <Header>
          <HendelseDetaljer sakType={sakType} hendelse={hendelse} />
          <Barn samsvar={hendelse.samsvarMellomKildeOgGrunnlag} />
        </Header>
      )
    case 'SIVILSTAND':
      return (
        <Header>
          <HendelseDetaljer sakType={sakType} hendelse={hendelse} />
          <VisSivilstand samsvar={hendelse.samsvarMellomKildeOgGrunnlag} />
        </Header>
      )
    case 'INSTITUSJONSOPPHOLD':
      return (
        <Header>
          <HendelseDetaljer sakType={sakType} hendelse={hendelse} />
          <Institusjonsopphold samsvar={hendelse.samsvarMellomKildeOgGrunnlag} />
        </Header>
      )
    case 'VERGEMAAL_ELLER_FREMTIDSFULLMAKT':
      return (
        <Header>
          <HendelseDetaljer sakType={sakType} hendelse={hendelse} />
          <Vergemaal samsvar={hendelse.samsvarMellomKildeOgGrunnlag} />
        </Header>
      )
    case 'GRUNNBELOEP':
      return (
        <Header>
          <KortTekst size="small">
            Hendelse fra {grunnlagsendringsKilde(hendelse.samsvarMellomKildeOgGrunnlag.type)}
          </KortTekst>
          <KortTekst size="small">{grunnlagsendringsBeskrivelse[hendelse.samsvarMellomKildeOgGrunnlag.type]}</KortTekst>
        </Header>
      )
  }
}

const Broedtekst = ({ children }: { children: ReactNode | Array<ReactNode> }) => {
  return (
    <KortTekst size="small" weight="semibold">
      {children}
    </KortTekst>
  )
}

const DetaljerWrapper = styled.div`
  margin-bottom: 1rem;
`

const GrunnlagSammenligningWrapper = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
`
const KortTekst = styled(BodyShort)`
  max-width: 25em;
`
const Header = styled.div`
  display: flex;
  flex-direction: column;
  align-self: flex-start;
`

const ListeWrapper = styled.ul`
  list-style-type: none;
`
