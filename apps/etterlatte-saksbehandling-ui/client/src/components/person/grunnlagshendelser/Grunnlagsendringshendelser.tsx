import {
  AnsvarligeForeldreSamsvar,
  BarnSamsvar,
  DoedsdatoSamsvar,
  Grunnlagsendringshendelse,
  GrunnlagsendringStatus,
  SamsvarMellomGrunnlagOgPdl,
  UtlandSamsvar,
  VergemaalEllerFremtidsfullmaktForholdSamsvar,
} from '../typer'
import {
  formaterKanskjeStringDato,
  formaterKanskjeStringDatoMedFallback,
  formaterStringDato,
} from '~utils/formattering'
import { Heading } from '@navikt/ds-react'
import { teksterForGrunnlagshendelser, teksterForSaksrolle } from '~components/person/grunnlagshendelser/tekster'
import { HendelseMetaItem, HendelseMetaWrapper, HendelseSammenligning, HendelseWrapper } from './styled'

const VisDoedsdatoSamsvar = (props: { data: DoedsdatoSamsvar }) => {
  return (
    <HendelseSammenligning>
      <dt>Dødsdato i grunnlag</dt>
      <dd>{formaterKanskjeStringDatoMedFallback('Ingen', props.data.fraGrunnlag)}</dd>
      <dt>Dødsdato i PDL</dt>
      <dd>{formaterKanskjeStringDatoMedFallback('Ingen', props.data.fraPdl)}</dd>
    </HendelseSammenligning>
  )
}

const VisBarnSamsvar = (props: { data: BarnSamsvar }) => {
  return (
    <HendelseSammenligning>
      <dt>Barn i grunnlag</dt>
      <dd>{props.data.fraGrunnlag?.join(', ') ?? 'Ingen barn'}</dd>
      <dt>Barn i PDL</dt>
      <dd>{props.data.fraPdl?.join(', ') ?? 'Ingen barn'}</dd>
    </HendelseSammenligning>
  )
}

const LandListeMedDatoOgOverskrift = (props: { tittel: string; land: { land?: string; dato?: string }[] }) => {
  if (!props.land || props.land.length === 0) {
    return (
      <>
        <strong>{props.tittel}</strong>
        <p>Ingen</p>
      </>
    )
  }
  return (
    <>
      <strong>{props.tittel}</strong>
      <ul style={{ listStyle: 'none' }}>
        {props.land.map(({ land, dato }, i) => (
          <li key={i}>
            {land} - {formaterKanskjeStringDato(dato)}
          </li>
        ))}
      </ul>
    </>
  )
}

const VisUtlandSamsvar = (props: { data: UtlandSamsvar }) => {
  return (
    <HendelseSammenligning>
      <dt>Ut-/innflyttinger i grunnlag</dt>
      <dd>
        <LandListeMedDatoOgOverskrift
          tittel={'Utflyttinger'}
          land={
            props.data.fraGrunnlag?.utflyttingFraNorge?.map(({ tilflyttingsland, dato }) => ({
              land: tilflyttingsland,
              dato,
            })) ?? []
          }
        />
        <LandListeMedDatoOgOverskrift
          tittel={'Innflyttinger'}
          land={
            props.data.fraGrunnlag?.innflyttingTilNorge?.map(({ fraflyttingsland, dato }) => ({
              land: fraflyttingsland,
              dato,
            })) ?? []
          }
        />
      </dd>
      <dt>Ut-/innflyttinger i PDL</dt>
      <dd>
        <LandListeMedDatoOgOverskrift
          tittel={'Utflyttinger'}
          land={
            props.data.fraPdl?.utflyttingFraNorge?.map(({ tilflyttingsland, dato }) => ({
              land: tilflyttingsland,
              dato,
            })) ?? []
          }
        />
        <LandListeMedDatoOgOverskrift
          tittel={'Innflyttinger'}
          land={
            props.data.fraPdl?.innflyttingTilNorge?.map(({ fraflyttingsland, dato }) => ({
              land: fraflyttingsland,
              dato,
            })) ?? []
          }
        />
      </dd>
    </HendelseSammenligning>
  )
}

const VisAnsvarligeForeldreSamsvar = (props: { data: AnsvarligeForeldreSamsvar }) => (
  <HendelseSammenligning>
    <dt>Ansvarlige foreldre i grunnlag</dt>
    <dd>{props.data.fraGrunnlag?.join(', ') ?? 'Ingen ansvarlige foreldre'}</dd>
    <dt>Ansvarlige foreldre i PDL</dt>
    <dd>{props.data.fraPdl?.join(', ') ?? 'Ingen ansvarlige foreldre'}</dd>
  </HendelseSammenligning>
)

const VisVergemaalEllerFremtidsfullmaktForholdSamsvar = (props: {
  data: VergemaalEllerFremtidsfullmaktForholdSamsvar
}) => (
  <HendelseSammenligning>
    <dt>Vergemål i grunnlag</dt>
    <dd>{props.data.fraGrunnlag?.join(', ') ?? 'Ingen vergemål funnet'}</dd>
    <dt>Vergemål i PDL</dt>
    <dd>{props.data.fraPdl?.join(', ') ?? 'Ingen vergemål funnet'}</dd>
  </HendelseSammenligning>
)

const HendelseVisning = (props: { data?: SamsvarMellomGrunnlagOgPdl }) => {
  switch (props.data?.type) {
    case 'DOEDSDATO':
      return <VisDoedsdatoSamsvar data={props.data} />
    case 'BARN':
      return <VisBarnSamsvar data={props.data} />
    case 'UTLAND':
      return <VisUtlandSamsvar data={props.data} />
    case 'ANSVARLIGE_FORELDRE':
      return <VisAnsvarligeForeldreSamsvar data={props.data} />
    case 'VERGEMAAL_ELLER_FREMTIDSFULLMAKT':
      return <VisVergemaalEllerFremtidsfullmaktForholdSamsvar data={props.data} />
    default:
      return <p>Ukjent hendelse</p>
  }
}

const ListeAvHendelser = ({ hendelser }: { hendelser: Grunnlagsendringshendelse[] }) => (
  <div>
    {hendelser.map((hendelse) => (
      <HendelseWrapper key={hendelse.id}>
        <HendelseMetaWrapper>
          <HendelseMetaItem>{teksterForGrunnlagshendelser[hendelse.type]}</HendelseMetaItem>
          <HendelseMetaItem>
            Gjelder {teksterForSaksrolle[hendelse.hendelseGjelderRolle].toLowerCase()}
            {hendelse.gjelderPerson ? ` (${hendelse.gjelderPerson})` : ''}
          </HendelseMetaItem>
          <HendelseMetaItem>Opprettet {formaterStringDato(hendelse.opprettet)}</HendelseMetaItem>
        </HendelseMetaWrapper>
        <div>
          <HendelseVisning data={hendelse.samsvarMellomPdlOgGrunnlag} />
        </div>
      </HendelseWrapper>
    ))}
  </div>
)

const STATUSER_SOM_ER_UHAANDTERTE: Readonly<GrunnlagsendringStatus[]> = ['VENTER_PAA_JOBB', 'SJEKKET_AV_JOBB'] as const

export const Grunnlagshendelser = ({ hendelser }: { hendelser: Grunnlagsendringshendelse[] }) => {
  const uhaandterteHendelser = hendelser.filter((hendelse) => STATUSER_SOM_ER_UHAANDTERTE.includes(hendelse.status))
  const vurderteHendelser = hendelser.filter((hendelse) => !STATUSER_SOM_ER_UHAANDTERTE.includes(hendelse.status))

  return (
    <>
      {uhaandterteHendelser.length > 0 ? (
        <div>
          <Heading size="medium" level="2">
            Uhåndterte hendelser
          </Heading>
          <ListeAvHendelser hendelser={uhaandterteHendelser} />
        </div>
      ) : null}
      {vurderteHendelser.length > 0 ? (
        <div>
          <Heading size="medium" level="2">
            Tidligere hendelser
          </Heading>
          <ListeAvHendelser hendelser={vurderteHendelser} />
        </div>
      ) : null}
    </>
  )
}
