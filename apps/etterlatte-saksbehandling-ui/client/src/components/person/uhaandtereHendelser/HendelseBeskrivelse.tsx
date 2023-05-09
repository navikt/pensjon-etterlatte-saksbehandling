import React, { useContext } from 'react'
import {
  FnrTilNavnMapContext,
  formaterFoedselsnummerMedNavn,
  formaterLandList,
  grunnlagsendringsBeskrivelse,
  grunnlagsendringsKilde,
  rolletekst,
} from '~components/person/uhaandtereHendelser/utils'
import {
  AnsvarligeForeldreSamsvar,
  BarnSamsvar,
  DoedsdatoSamsvar,
  Grunnlagsendringshendelse,
  InstitusjonsoppholdSamsvar,
  UtlandSamsvar,
  VergemaalEllerFremtidsfullmakt,
  VergemaalEllerFremtidsfullmaktForholdSamsvar,
} from '~components/person/typer'
import { formaterKanskjeStringDatoMedFallback } from '~utils/formattering'
import styled from 'styled-components'
import { BodyShort } from '@navikt/ds-react'

const AnsvarligeForeldreSamsvar = (props: { samsvar: AnsvarligeForeldreSamsvar }) => {
  const navneMap = useContext(FnrTilNavnMapContext)
  return (
    <GrunnlagSammenligningWrapper>
      <div>
        <BodySmall>Nytt grunnlag (PDL)</BodySmall>
        <BodySmall>
          {props.samsvar.fraPdl?.map((fnr) => formaterFoedselsnummerMedNavn(navneMap, fnr))?.join(', ') ??
            'Ingen ansvarlige foreldre'}
        </BodySmall>
      </div>
      <div>
        <BodySmall>Eksisterende grunnlag</BodySmall>
        <BodySmall>
          {props.samsvar.fraGrunnlag?.map((fnr) => formaterFoedselsnummerMedNavn(navneMap, fnr))?.join(', ') ??
            'Ingen ansvarlige foreldre'}
        </BodySmall>
      </div>
    </GrunnlagSammenligningWrapper>
  )
}

const UtlandSamsvarVisning = (props: { samsvar: UtlandSamsvar }) => {
  const { samsvar } = props

  return (
    <GrunnlagSammenligningWrapper>
      <div>
        <BodySmall>Nytt grunnlag (PDL)</BodySmall>
        <BodySmall>Ut: {formaterLandList(samsvar.fraPdl?.utflyttingFraNorge ?? [])}</BodySmall>
        <BodySmall>Inn: {formaterLandList(samsvar.fraPdl?.innflyttingTilNorge ?? [])}</BodySmall>
      </div>
      <div>
        <BodySmall>Eksisterende grunnlag</BodySmall>
        <BodySmall>Ut: {formaterLandList(samsvar.fraGrunnlag?.utflyttingFraNorge ?? [])}</BodySmall>
        <BodySmall>Inn: {formaterLandList(samsvar.fraGrunnlag?.innflyttingTilNorge ?? [])}</BodySmall>
      </div>
    </GrunnlagSammenligningWrapper>
  )
}

const Doedsdato = (props: { samsvar: DoedsdatoSamsvar }) => {
  const { samsvar } = props
  return (
    <GrunnlagSammenligningWrapper>
      <div>
        <BodySmall>Nytt grunnlag (PDL)</BodySmall>
        <BodySmall>{formaterKanskjeStringDatoMedFallback('Ingen', samsvar.fraPdl)}</BodySmall>
      </div>
      <div>
        <BodySmall>Eksisterende grunnlag</BodySmall>
        <BodySmall>{formaterKanskjeStringDatoMedFallback('Ingen', samsvar.fraGrunnlag)}</BodySmall>
      </div>
    </GrunnlagSammenligningWrapper>
  )
}

const Barn = (props: { samsvar: BarnSamsvar }) => {
  const { samsvar } = props
  const fnrTilNavn = useContext(FnrTilNavnMapContext)
  return (
    <GrunnlagSammenligningWrapper>
      <div>
        <BodySmall>Nytt grunnlag (PDL)</BodySmall>
        <BodySmall>
          {samsvar.fraPdl?.map((fnr) => formaterFoedselsnummerMedNavn(fnrTilNavn, fnr)).join(', ') ?? 'Ingen barn'}
        </BodySmall>
      </div>
      <div>
        <BodySmall>Eksisterende grunnlag</BodySmall>
        <BodySmall>
          {samsvar.fraGrunnlag?.map((fnr) => formaterFoedselsnummerMedNavn(fnrTilNavn, fnr)).join(', ') ?? 'Ingen barn'}
        </BodySmall>
      </div>
    </GrunnlagSammenligningWrapper>
  )
}

const GrunnlagVergemaal = (props: { vergemaal: VergemaalEllerFremtidsfullmakt }) => {
  const { vergemaal } = props
  const { vergeEllerFullmektig, embete, type } = vergemaal
  const { navn, omfang, motpartsPersonident, omfangetErInnenPersonligOmraade } = vergeEllerFullmektig
  return (
    <>
      <BodySmall>Embete: {embete ?? 'ikke angitt'}</BodySmall>
      <BodySmall>Type: {type ?? 'ikke angitt'}</BodySmall>

      <BodySmall>
        Omfang: {omfang ?? 'ikke angitt '}
        {omfangetErInnenPersonligOmraade ? 'Er innenfor personlig område' : 'Er ikke innenfor personlig område'}
      </BodySmall>
      <BodySmall>
        Verge: {navn ?? 'ikke angitt'} ({motpartsPersonident})
      </BodySmall>
    </>
  )
}

const Institusjonsopphold = (props: { samsvar: InstitusjonsoppholdSamsvar }) => {
  const { samsvar } = props
  return (
    <>
      <BodySmall>Oppholdstype</BodySmall>
      <BodySmall>{samsvar.oppholdstype}</BodySmall>
    </>
  )
}

const Vergemaal = (props: { samsvar: VergemaalEllerFremtidsfullmaktForholdSamsvar }) => {
  return (
    <GrunnlagSammenligningWrapper>
      <div>
        <BodySmall>Nytt grunnlag (PDL)</BodySmall>
        {props.samsvar.fraPdl?.map((verge, i) => <GrunnlagVergemaal key={i} vergemaal={verge} />) ?? (
          <BodySmall>Ingen vergemål funnet</BodySmall>
        )}
      </div>
      <div>
        <BodySmall>Eksisterende grunnlag</BodySmall>
        {props.samsvar.fraGrunnlag?.map((verge, i) => <GrunnlagVergemaal key={i} vergemaal={verge} />) ?? (
          <BodySmall>Ingen vergemål funnet</BodySmall>
        )}
      </div>
    </GrunnlagSammenligningWrapper>
  )
}

const HendelseDetaljer = (props: { hendelse: Grunnlagsendringshendelse }) => {
  const navneMap = useContext(FnrTilNavnMapContext)
  return (
    <DetaljerWrapper>
      <BodySmall>
        {`
        ${rolletekst[props.hendelse.hendelseGjelderRolle]}
        ${formaterFoedselsnummerMedNavn(navneMap, props.hendelse.gjelderPerson)} har
        ${grunnlagsendringsBeskrivelse[props.hendelse.samsvarMellomKildeOgGrunnlag.type]}
          `}
      </BodySmall>
    </DetaljerWrapper>
  )
}

export const HendelseBeskrivelse = (props: { hendelse: Grunnlagsendringshendelse }) => {
  const { hendelse } = props
  switch (hendelse.samsvarMellomKildeOgGrunnlag.type) {
    case 'UTLAND':
      return (
        <Header>
          <HendelseDetaljer hendelse={hendelse} />
          <UtlandSamsvarVisning samsvar={hendelse.samsvarMellomKildeOgGrunnlag} />
        </Header>
      )
    case 'ANSVARLIGE_FORELDRE':
      return (
        <Header>
          <HendelseDetaljer hendelse={hendelse} />
          <AnsvarligeForeldreSamsvar samsvar={hendelse.samsvarMellomKildeOgGrunnlag} />
        </Header>
      )
    case 'DOEDSDATO':
      return (
        <Header>
          <HendelseDetaljer hendelse={hendelse} />
          <Doedsdato samsvar={hendelse.samsvarMellomKildeOgGrunnlag} />
        </Header>
      )
    case 'BARN':
      return (
        <Header>
          <HendelseDetaljer hendelse={hendelse} />
          <Barn samsvar={hendelse.samsvarMellomKildeOgGrunnlag} />
        </Header>
      )
    case 'INSTITUSJONSOPPHOLD':
      return (
        <Header>
          <HendelseDetaljer hendelse={hendelse} />
          <Institusjonsopphold samsvar={hendelse.samsvarMellomKildeOgGrunnlag} />
        </Header>
      )
    case 'VERGEMAAL_ELLER_FREMTIDSFULLMAKT':
      return (
        <Header>
          <HendelseDetaljer hendelse={hendelse} />
          <Vergemaal samsvar={hendelse.samsvarMellomKildeOgGrunnlag} />
        </Header>
      )
    case 'GRUNNBELOEP':
      return (
        <Header>
          <BodySmall>Hendelse fra {grunnlagsendringsKilde(hendelse.samsvarMellomKildeOgGrunnlag.type)}</BodySmall>
          <BodySmall>{grunnlagsendringsBeskrivelse[hendelse.samsvarMellomKildeOgGrunnlag.type]}</BodySmall>
        </Header>
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
const BodySmall = styled(BodyShort).attrs({ size: 'small' })`
  max-width: 25em;
`
const Header = styled.div`
  display: flex;
  flex-direction: column;
  align-self: flex-start;
`
