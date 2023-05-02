import { Alert, BodyShort, Button, Heading, Label } from '@navikt/ds-react'
import styled from 'styled-components'
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
import {
  FnrTilNavnMapContext,
  formaterFoedselsnummerMedNavn,
  formaterLandList,
  grunnlagsendringsBeskrivelse,
  grunnlagsendringsKilde,
  grunnlagsendringsTittel,
  rolletekst,
  stoetterDoffenRevurderingAvHendelse,
} from '~components/person/uhaandtereHendelser/utils'
import { formaterKanskjeStringDatoMedFallback, formaterStringDato } from '~utils/formattering'
import { isSuccess, Result } from '~shared/hooks/useApiCall'
import React, { useContext, useMemo } from 'react'
import { PersonerISakResponse } from '~shared/api/grunnlag'

type Props = {
  hendelser: Array<Grunnlagsendringshendelse>
  startRevurdering: () => void
  disabled: boolean
  grunnlag: Result<PersonerISakResponse>
}

const UhaandterteHendelser = (props: Props) => {
  const { hendelser, disabled, startRevurdering, grunnlag } = props
  if (hendelser.length === 0) return null

  const navneMap = useMemo(() => {
    if (isSuccess(grunnlag)) {
      return grunnlag.data.personer
    } else {
      return {}
    }
  }, [grunnlag])

  return (
    <div>
      <FnrTilNavnMapContext.Provider value={navneMap}>
        <StyledAlert>Ny hendelse som kan kreve revurdering. Vurder om det har konsekvens for ytelsen.</StyledAlert>
        <Heading size="medium">Hendelser</Heading>
        {hendelser.map((hendelse) => (
          <UhaandtertHendelse
            key={hendelse.id}
            hendelse={hendelse}
            disabled={disabled}
            startRevurdering={startRevurdering}
          />
        ))}
      </FnrTilNavnMapContext.Provider>
    </div>
  )
}

const UhaandtertHendelse = (props: {
  hendelse: Grunnlagsendringshendelse
  disabled: boolean
  startRevurdering: () => void
}) => {
  const { hendelse, disabled, startRevurdering } = props
  const { type, opprettet } = hendelse
  const stoetterRevurdering = stoetterDoffenRevurderingAvHendelse(hendelse)

  return (
    <Wrapper>
      <Label spacing>{grunnlagsendringsTittel[type]}</Label>
      <Content>
        <Header>
          <BodySmall>Dato</BodySmall>
          <BodySmall>{formaterStringDato(opprettet)}</BodySmall>
        </Header>
        <HendelseBeskrivelse hendelse={hendelse} />
        {stoetterRevurdering ? (
          <Button disabled={disabled} onClick={startRevurdering}>
            Start revurdering
          </Button>
        ) : (
          <Alert variant="warning" size="small" inline>
            Doffen støtter ikke revurdering
          </Alert>
        )}
      </Content>
    </Wrapper>
  )
}

const AnsvarligeForeldre = (props: { samsvar: AnsvarligeForeldreSamsvar }) => {
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

const BodySmall = styled(BodyShort).attrs({ size: 'small' })`
  max-width: 25em;
`

const HendelseDetaljer = (props: { hendelse: Grunnlagsendringshendelse }) => {
  const navneMap = useContext(FnrTilNavnMapContext)
  return (
    <DetaljerWrapper>
      <BodySmall>
        {`
        ${rolletekst[props.hendelse.hendelseGjelderRolle]}
        ${formaterFoedselsnummerMedNavn(navneMap, props.hendelse.gjelderPerson)} har
        ${grunnlagsendringsBeskrivelse[props.hendelse.type]}
          `}
      </BodySmall>
    </DetaljerWrapper>
  )
}

const HendelseBeskrivelse = (props: { hendelse: Grunnlagsendringshendelse }) => {
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
          <AnsvarligeForeldre samsvar={hendelse.samsvarMellomKildeOgGrunnlag} />
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
          <BodySmall>Hendelse fra {grunnlagsendringsKilde(hendelse.type)}</BodySmall>
          <BodySmall>{grunnlagsendringsBeskrivelse[hendelse.type]}</BodySmall>
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

const Wrapper = styled.div`
  margin-bottom: 1rem;
  max-width: 50rem;
`
const Header = styled.div`
  display: flex;
  flex-direction: column;
  align-self: flex-start;
`

const Content = styled.div`
  display: grid;
  grid-template-columns: 5rem auto 13rem;
  gap: 2em;
  align-items: center;
`

const StyledAlert = styled(Alert).attrs({ variant: 'warning' })`
  display: inline-flex;
  margin: 1em 0;
`

export default UhaandterteHendelser
