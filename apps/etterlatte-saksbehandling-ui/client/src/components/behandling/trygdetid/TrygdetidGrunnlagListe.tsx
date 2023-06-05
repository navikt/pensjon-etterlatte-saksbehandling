import {
  ILand,
  ITrygdetid,
  ITrygdetidGrunnlag,
  ITrygdetidGrunnlagType,
  slettTrygdetidsgrunnlag,
} from '~shared/api/trygdetid'
import { FlexHeader, IconWrapper, TableWrapper } from '~components/behandling/soeknadsoversikt/familieforhold/styled'
import { IconSize } from '~shared/types/Icon'
import { Button, Heading, Table } from '@navikt/ds-react'
import { CalendarIcon } from '@navikt/aksel-icons'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterStringDato } from '~utils/formattering'
import React from 'react'
import styled from 'styled-components'
import { useApiCall } from '~shared/hooks/useApiCall'

type Props = {
  trygdetid: ITrygdetid
  setTrygdetid: (trygdetid: ITrygdetid) => void
  trygdetidGrunnlagType: ITrygdetidGrunnlagType
  landListe: ILand[]
}

export const TrygdetidGrunnlagListe: React.FC<Props> = ({
  trygdetid,
  setTrygdetid,
  trygdetidGrunnlagType,
  landListe,
}) => {
  const [nyTrygdetid, slettTrygdetidsgrunnlagRequest] = useApiCall(slettTrygdetidsgrunnlag)
  const trygdetidGrunnlagListe = trygdetid.trygdetidGrunnlag.filter((tg) => tg.type == trygdetidGrunnlagType)
  const grunnlagTypeTekst = trygdetidGrunnlagType == ITrygdetidGrunnlagType.FAKTISK ? 'Faktisk' : 'Fremtidig'

  const slettTrygdetid = (grunnlagId: string) => {
    slettTrygdetidsgrunnlagRequest(
      {
        behandlingsId: trygdetid.behandlingId,
        trygdetidGrunnlagId: grunnlagId,
      },
      (oppdatertTrygdetid) => {
        setTrygdetid(oppdatertTrygdetid)
      }
    )
  }

  return (
    <div>
      <FlexHeader>
        <IconWrapper>
          <CalendarIcon fontSize={IconSize.DEFAULT} />
        </IconWrapper>
        <Heading size={'small'} level={'3'}>
          {grunnlagTypeTekst} trygdetid
        </Heading>
      </FlexHeader>
      {trygdetidGrunnlagListe.length ? (
        <TableWrapper>
          <Table size={'medium'}>
            <Table.Header>
              <Table.Row>
                <Table.HeaderCell />
                <Table.HeaderCell scope={'col'}>Land</Table.HeaderCell>
                <Table.HeaderCell scope={'col'}>Fra dato</Table.HeaderCell>
                <Table.HeaderCell scope={'col'}>Til dato</Table.HeaderCell>
                <Table.HeaderCell scope={'col'}>{grunnlagTypeTekst} trygdetid</Table.HeaderCell>
                <Table.HeaderCell scope={'col'}>Kilde</Table.HeaderCell>
                <Table.HeaderCell scope={'col'}> </Table.HeaderCell>
                <Table.HeaderCell scope={'col'}> </Table.HeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {trygdetidGrunnlagListe.map((periode) => {
                return <PeriodeRow trygdetidGrunnlag={periode} slettGrunnlag={slettTrygdetid} landListe={landListe} />
              })}
            </Table.Body>
          </Table>
        </TableWrapper>
      ) : (
        <p>Ingen perioder registert</p>
      )}

      <NyPeriode>
        <Button size="small">Legg til ny periode</Button>
      </NyPeriode>
    </div>
  )
}

const PeriodeRow = ({
  trygdetidGrunnlag,
  slettGrunnlag,
  landListe,
}: {
  trygdetidGrunnlag: ITrygdetidGrunnlag
  slettGrunnlag: (grunnlagId: string) => void
  landListe: ILand[]
}) => {
  const beregnetTrygdetid = trygdetidGrunnlag?.beregnet
    ? `${trygdetidGrunnlag.beregnet.aar} år ${trygdetidGrunnlag.beregnet.maaneder} måneder ${trygdetidGrunnlag.beregnet.dager} dager`
    : '-'

  return (
    <Table.ExpandableRow key={trygdetidGrunnlag.id} content="Her kommer beskrivelse.">
      <Table.DataCell>
        {landListe.find((land) => land.isoLandkode == trygdetidGrunnlag.bosted).beskrivelse.tekst}
      </Table.DataCell>
      <Table.DataCell>
        <Datofelt>{formaterStringDato(trygdetidGrunnlag.periodeFra!!)}</Datofelt>
      </Table.DataCell>
      <Table.DataCell>
        <Datofelt>{formaterStringDato(trygdetidGrunnlag.periodeTil!!)}</Datofelt>
      </Table.DataCell>
      <Table.DataCell>{beregnetTrygdetid}</Table.DataCell>
      <Table.DataCell>
        <Info
          tekst={trygdetidGrunnlag.kilde.ident}
          label={''}
          undertekst={`saksbehandler: ${formaterStringDato(trygdetidGrunnlag.kilde.tidspunkt)}`}
        />
      </Table.DataCell>
      <Table.DataCell>
        <RedigerWrapper>Rediger</RedigerWrapper>
      </Table.DataCell>
      <Table.DataCell>
        <RedigerWrapper onClick={() => slettGrunnlag(trygdetidGrunnlag.id!!)}>Slett</RedigerWrapper>
      </Table.DataCell>
    </Table.ExpandableRow>
  )
}

const Datofelt = styled.div`
  width: 5em;
`

const RedigerWrapper = styled.div`
  display: inline-flex;
  float: left;
  cursor: pointer;
  color: #0067c5;

  &:hover {
    text-decoration-line: underline;
  }
`

const NyPeriode = styled.div`
  margin-top: 1em;
`
