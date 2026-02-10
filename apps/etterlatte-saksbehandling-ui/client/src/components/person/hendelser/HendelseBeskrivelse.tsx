import React, { useContext } from 'react'
import {
  FnrTilNavnMapContext,
  formaterFoedselsnummerMedNavn,
  formaterLandList,
  formaterRolle,
  grunnlagsendringsBeskrivelse,
  grunnlagsendringsKilde,
  ufoeretrygdVedtakstypeBeskrivelse,
} from '~components/person/hendelser/utils'
import {
  AdresseSamsvar,
  AnsvarligeForeldreSamsvar,
  BarnSamsvar,
  DoedsdatoSamsvar,
  Folkeregisteridentifikatorsamsvar,
  Grunnlagsendringshendelse,
  InstitusjonsoppholdSamsvar,
  Sivilstand,
  SivilstandSamsvar,
  UfoereHendelse,
  UtlandSamsvar,
  VergemaalEllerFremtidsfullmakt,
  VergemaalEllerFremtidsfullmaktForholdSamsvar,
} from '~components/person/typer'
import { formaterDato, formaterKanskjeStringDatoMedFallback } from '~utils/formatering/dato'
import styled from 'styled-components'
import { BodyShort, Label, VStack } from '@navikt/ds-react'
import { Adressevisning } from '~components/behandling/felles/Adressevisning'
import { SakType } from '~shared/types/sak'
import { institusjonstype } from '~shared/types/Institusjonsopphold'

const VisAnsvarligeForeldreSamsvar = ({ samsvar }: { samsvar: AnsvarligeForeldreSamsvar }) => {
  const navneMap = useContext(FnrTilNavnMapContext)
  return (
    <GrunnlagSammenligningWrapper>
      <div>
        <Label>Nytt grunnlag (PDL)</Label>
        <KortTekst size="small">
          {samsvar.fraPdl?.map((fnr) => formaterFoedselsnummerMedNavn(navneMap, fnr))?.join(', ') ??
            'Ingen ansvarlige foreldre'}
        </KortTekst>
      </div>
      <div>
        <Label>Eksisterende grunnlag</Label>
        <KortTekst size="small">
          {samsvar.fraGrunnlag?.map((fnr) => formaterFoedselsnummerMedNavn(navneMap, fnr))?.join(', ') ??
            'Ingen ansvarlige foreldre'}
        </KortTekst>
      </div>
    </GrunnlagSammenligningWrapper>
  )
}

const FolkeregisterSamsvarVisning = ({ samsvar }: { samsvar: Folkeregisteridentifikatorsamsvar }) => {
  return (
    <GrunnlagSammenligningWrapper>
      <div>
        <Label>Ny folkeregisteridentifikator (PDL)</Label>
        <KortTekst size="small">{samsvar.fraPdl}</KortTekst>
      </div>
      <div>
        <Label>Eksisterende folkeregisteridentifikator (grunnlag)</Label>
        <KortTekst size="small">{samsvar.fraGrunnlag}</KortTekst>
      </div>
    </GrunnlagSammenligningWrapper>
  )
}

const UtlandSamsvarVisning = ({ samsvar }: { samsvar: UtlandSamsvar }) => {
  return (
    <GrunnlagSammenligningWrapper>
      <div>
        <Label>Nytt grunnlag (PDL)</Label>
        <KortTekst size="small">Ut: {formaterLandList(samsvar.fraPdl?.utflyttingFraNorge ?? [])}</KortTekst>
        <KortTekst size="small">Inn: {formaterLandList(samsvar.fraPdl?.innflyttingTilNorge ?? [])}</KortTekst>
      </div>
      <div>
        <Label>Eksisterende grunnlag</Label>
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
        <Label>Nytt grunnlag (PDL)</Label>
        <KortTekst size="small">{formaterKanskjeStringDatoMedFallback('Ingen', samsvar.fraPdl)}</KortTekst>
      </div>
      <div>
        <Label>Eksisterende grunnlag</Label>
        <KortTekst size="small">{formaterKanskjeStringDatoMedFallback('Ingen', samsvar.fraGrunnlag)}</KortTekst>
      </div>
    </GrunnlagSammenligningWrapper>
  )
}

const Adresse = ({ adresse }: { adresse: AdresseSamsvar }) => {
  return (
    <GrunnlagSammenligningWrapper>
      <div>
        <Label>Nytt grunnlag (PDL)</Label>
        <KortTekst as="div">
          <Adressevisning adresser={adresse.fraPdl} soeknadsoversikt={false} />
        </KortTekst>
      </div>
      <div>
        <Label>Eksisterende grunnlag</Label>
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
        <Label>Nytt grunnlag (PDL)</Label>
        <KortTekst size="small">
          {samsvar.fraPdl?.map((fnr) => formaterFoedselsnummerMedNavn(fnrTilNavn, fnr)).join(', ') ?? 'Ingen barn'}
        </KortTekst>
      </div>
      <div>
        <Label>Eksisterende grunnlag</Label>
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
        <Label>Nytt grunnlag (PDL)</Label>
        <ListeWrapper>
          {samsvar.fraPdl?.map((sivilstand, i) => (
            <li key={i}>{sivilstandWrapper(sivilstand, i)}</li>
          ))}
        </ListeWrapper>
      </div>
      <div>
        <Label>Eksisterende grunnlag</Label>
        <ListeWrapper>
          {samsvar.fraGrunnlag?.map((sivilstand, i) => (
            <li key={i}>{sivilstandWrapper(sivilstand, i)}</li>
          ))}
        </ListeWrapper>
      </div>
    </GrunnlagSammenligningWrapper>
  )
}

const Ufoeretrygd = (props: { hendelse: UfoereHendelse }) => {
  const hendelse = props.hendelse
  return (
    <>
      <div>
        <Label>Vedtakstype</Label>
        <KortTekst>{ufoeretrygdVedtakstypeBeskrivelse[hendelse.vedtaksType]}</KortTekst>
      </div>
      <div>
        <Label>Virkningstidspunkt</Label>
        <KortTekst>{formaterDato(hendelse.virkningsdato)}</KortTekst>
      </div>
    </>
  )
}

const Institusjonsopphold = (props: { samsvar: InstitusjonsoppholdSamsvar }) => {
  const { samsvar } = props
  return (
    <>
      <KortTekst size="small">Oppholdstype: {samsvar.oppholdstype}</KortTekst>
      <KortTekst size="small">
        Startdato:{' '}
        {samsvar.oppholdBeriket.startdato ? formaterDato(samsvar.oppholdBeriket.startdato) : 'Ingen startdato'}
      </KortTekst>
      <KortTekst size="small">
        Faktisk sluttdato:{' '}
        {samsvar.oppholdBeriket.faktiskSluttdato
          ? formaterDato(samsvar.oppholdBeriket.faktiskSluttdato)
          : 'Ingen sluttdato'}
      </KortTekst>
      <KortTekst size="small">
        Forventet sluttdato:{' '}
        {samsvar.oppholdBeriket.forventetSluttdato
          ? formaterDato(samsvar.oppholdBeriket.forventetSluttdato)
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
        <Label>Nytt grunnlag (PDL)</Label>
        {props.samsvar.fraPdl?.map((verge, i) => <GrunnlagVergemaal key={i} vergemaal={verge} />) ?? (
          <KortTekst size="small">Ingen vergemål funnet</KortTekst>
        )}
      </div>
      <div>
        <Label>Eksisterende grunnlag</Label>
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

const HendelseKommentar = ({ kommentar }: { kommentar?: string }) => {
  return (
    <div>
      <Label>Kommentar</Label>
      <KortTekst size="small">{!!kommentar ? kommentar : <i>Ingen kommentar på hendelse</i>}</KortTekst>
    </div>
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
        <VStack gap="space-4">
          <HendelseDetaljer sakType={sakType} hendelse={hendelse} />
          <UtlandSamsvarVisning samsvar={hendelse.samsvarMellomKildeOgGrunnlag} />
          <HendelseKommentar kommentar={hendelse.kommentar} />
        </VStack>
      )
    case 'FOLKEREGISTERIDENTIFIKATOR':
      return (
        <VStack gap="space-4">
          <HendelseDetaljer sakType={sakType} hendelse={hendelse} />
          <FolkeregisterSamsvarVisning samsvar={hendelse.samsvarMellomKildeOgGrunnlag} />
          <HendelseKommentar kommentar={hendelse.kommentar} />
        </VStack>
      )
    case 'ANSVARLIGE_FORELDRE':
      return (
        <VStack gap="space-4">
          <HendelseDetaljer sakType={sakType} hendelse={hendelse} />
          <VisAnsvarligeForeldreSamsvar samsvar={hendelse.samsvarMellomKildeOgGrunnlag} />
          <HendelseKommentar kommentar={hendelse.kommentar} />
        </VStack>
      )
    case 'DOEDSDATO':
      return (
        <VStack gap="space-4">
          <HendelseDetaljer sakType={sakType} hendelse={hendelse} />
          <Doedsdato samsvar={hendelse.samsvarMellomKildeOgGrunnlag} />
          <HendelseKommentar kommentar={hendelse.kommentar} />
        </VStack>
      )
    case 'ADRESSE':
      return (
        <VStack gap="space-4">
          <HendelseDetaljer sakType={sakType} hendelse={hendelse} />
          <Adresse adresse={hendelse.samsvarMellomKildeOgGrunnlag} />
          <HendelseKommentar kommentar={hendelse.kommentar} />
        </VStack>
      )
    case 'BARN':
      return (
        <VStack gap="space-4">
          <HendelseDetaljer sakType={sakType} hendelse={hendelse} />
          <Barn samsvar={hendelse.samsvarMellomKildeOgGrunnlag} />
          <HendelseKommentar kommentar={hendelse.kommentar} />
        </VStack>
      )
    case 'SIVILSTAND':
      return (
        <VStack gap="space-4">
          <HendelseDetaljer sakType={sakType} hendelse={hendelse} />
          <VisSivilstand samsvar={hendelse.samsvarMellomKildeOgGrunnlag} />
          <HendelseKommentar kommentar={hendelse.kommentar} />
        </VStack>
      )
    case 'INSTITUSJONSOPPHOLD':
      return (
        <VStack gap="space-4">
          <HendelseDetaljer sakType={sakType} hendelse={hendelse} />
          <Institusjonsopphold samsvar={hendelse.samsvarMellomKildeOgGrunnlag} />
          <HendelseKommentar kommentar={hendelse.kommentar} />
        </VStack>
      )
    case 'UFOERETRYGD':
      return (
        <VStack gap="space-4">
          <HendelseDetaljer sakType={sakType} hendelse={hendelse} />
          <Ufoeretrygd hendelse={hendelse.samsvarMellomKildeOgGrunnlag.hendelse} />
          <HendelseKommentar kommentar={hendelse.kommentar} />
        </VStack>
      )
    case 'VERGEMAAL_ELLER_FREMTIDSFULLMAKT':
      return (
        <VStack gap="space-4">
          <HendelseDetaljer sakType={sakType} hendelse={hendelse} />
          <Vergemaal samsvar={hendelse.samsvarMellomKildeOgGrunnlag} />
          <HendelseKommentar kommentar={hendelse.kommentar} />
        </VStack>
      )
    case 'GRUNNBELOEP':
      return (
        <VStack gap="space-4">
          <KortTekst size="small">
            Hendelse fra {grunnlagsendringsKilde(hendelse.samsvarMellomKildeOgGrunnlag.type)}
          </KortTekst>
          <KortTekst size="small">{grunnlagsendringsBeskrivelse[hendelse.samsvarMellomKildeOgGrunnlag.type]}</KortTekst>
          <HendelseKommentar kommentar={hendelse.kommentar} />
        </VStack>
      )
  }
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

const ListeWrapper = styled.ul`
  list-style-type: none;
`
