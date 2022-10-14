import {
  Doedshendelse,
  Grunnlagsendringshendelse,
  GrunnlagsendringsType,
  Grunnlagsinformasjon,
  Utflyttingshendelse,
} from '../typer'
import { Table } from '@navikt/ds-react'
import { formaterStringDato } from '../../../utils/formattering'

const teksterForGrunnlagshendelser: Record<GrunnlagsendringsType, string> = {
  GJENLEVENDE_FORELDER_DOED: 'Dødsfall gjenlevende forelder',
  SOEKER_DOED: 'Dødsfall søker',
  SOESKEN_DOED: 'Dødsfall søsken',
  UTFLYTTING: 'Utflytting',
}

function VisUtflytting({ utflytting }: { utflytting: Utflyttingshendelse }) {
  return (
    <p>
      {`${utflytting.fnr} har flyttet ut til ${utflytting.tilflyttingsLand}
      ${utflytting?.tilflyttingsstedIUtlandet ? utflytting.tilflyttingsstedIUtlandet : ''} den 
      ${formaterStringDato(utflytting.utflyttingsdato)}`}
    </p>
  )
}

function formaterKanskjeStringDato(dato?: string): string {
  if (dato) {
    return formaterStringDato(dato)
  }
  return 'Ukjent dato'
}

function VisDoedshendelse({ doedshendelse }: { doedshendelse: Doedshendelse }) {
  return (
    <p>
      {doedshendelse.avdoedFnr} døde {formaterKanskjeStringDato(doedshendelse.doedsdato)}
    </p>
  )
}

function FormaterHendelseData(props: { data?: Grunnlagsinformasjon }) {
  switch (props.data?.type) {
    case 'UTFLYTTING':
      return <VisUtflytting utflytting={props.data.hendelse} />
    case 'GJENLEVENDE_FORELDER_DOED':
    case 'SOEKER_DOED':
    case 'SOESKEN_DOED':
      return <VisDoedshendelse doedshendelse={props.data.hendelse} />
    default:
      return <p>Ukjent hendelse</p>
  }
}

function TabellForHendelser({ hendelser }: { hendelser: Grunnlagsendringshendelse[] }) {
  return (
    <Table>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell>Type</Table.HeaderCell>
          <Table.HeaderCell>Dato</Table.HeaderCell>
          <Table.HeaderCell>Ekstra data</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {hendelser.map((hendelse) => (
          <Table.Row key={hendelse.id}>
            <Table.DataCell>{teksterForGrunnlagshendelser[hendelse.type]}</Table.DataCell>
            <Table.DataCell>{formaterStringDato(hendelse.opprettet)}</Table.DataCell>
            <Table.DataCell>
              <FormaterHendelseData data={hendelse.data} />
            </Table.DataCell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  )
}

export function Grunnlagshendelser({ hendelser }: { hendelser: Grunnlagsendringshendelse[] }) {
  const uhaandterteHendelser = hendelser.filter((hendelse) =>
    ['IKKE_VURDERT', 'GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING'].includes(hendelse.status)
  )
  const vurderteHendelser = hendelser.filter(
    (hendelse) => !['IKKE_VURDERT', 'GYLDIG_OG_KAN_TAS_MED_I_BEHANDLING'].includes(hendelse.status)
  )

  return (
    <>
      {uhaandterteHendelser.length > 0 ? (
        <div>
          <h2>Uhåndterte hendelser</h2>
          <TabellForHendelser hendelser={uhaandterteHendelser} />
        </div>
      ) : null}
      {vurderteHendelser.length > 0 ? (
        <div>
          <h2>Tidligere hendelser</h2>
          <TabellForHendelser hendelser={vurderteHendelser} />
        </div>
      ) : null}
    </>
  )
}
