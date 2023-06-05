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
import React, { useState } from 'react'
import styled from 'styled-components'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { TrygdetidGrunnlag } from '~components/behandling/trygdetid/TrygdetidGrunnlag'
import { ApiErrorAlert } from '~ErrorBoundary'
import Spinner from '~shared/Spinner'

type Props = {
  trygdetid: ITrygdetid
  setTrygdetid: (trygdetid: ITrygdetid) => void
  trygdetidGrunnlagType: ITrygdetidGrunnlagType
  landListe: ILand[]
}

const initialEndreModusState = {
  status: false,
  trygdetidGrunnlagId: '',
}

export const TrygdetidGrunnlagListe: React.FC<Props> = ({
  trygdetid,
  setTrygdetid,
  trygdetidGrunnlagType,
  landListe,
}) => {
  const [endreModus, setEndreModus] = useState(initialEndreModusState)
  const trygdetidGrunnlagListe = trygdetid.trygdetidGrunnlag
    .filter((tg) => tg.type == trygdetidGrunnlagType)
    .sort((a, b) => (a.periodeFra!! > b.periodeFra!! ? 1 : -1))
  const grunnlagTypeTekst = trygdetidGrunnlagType == ITrygdetidGrunnlagType.FAKTISK ? 'Faktisk' : 'Fremtidig'

  const oppdaterStateOgSettTrygdetid = (trygdetid: ITrygdetid) => {
    setEndreModus(initialEndreModusState)
    setTrygdetid(trygdetid)
  }

  return (
    <GrunnlagListe>
      <FlexHeader>
        <IconWrapper>
          <CalendarIcon fontSize={IconSize.DEFAULT} />
        </IconWrapper>
        <Heading size={'small'} level={'3'}>
          {grunnlagTypeTekst} trygdetid
        </Heading>
      </FlexHeader>
      {trygdetidGrunnlagType == ITrygdetidGrunnlagType.FAKTISK ? (
        <p>Legg til trygdetid fra avdøde var 16 år frem til hen døde.</p>
      ) : (
        <p>Legg til trygdetid fra dødsdato til og med kalenderåret avdøde hadde blitt 66 år.</p>
      )}
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
                return (
                  <PeriodeRow
                    trygdetidGrunnlag={periode}
                    behandlingId={trygdetid.behandlingId}
                    setTrygdetid={setTrygdetid}
                    endrePeriode={(trygdetidGrunnlagId) => {
                      setEndreModus({ status: true, trygdetidGrunnlagId: trygdetidGrunnlagId })
                    }}
                    landListe={landListe}
                    key={periode.id}
                  />
                )
              })}
            </Table.Body>
          </Table>
        </TableWrapper>
      ) : (
        <p>Ingen perioder registert</p>
      )}

      <NyEllerOppdatertPeriode>
        {endreModus.status ? (
          <TrygdetidGrunnlag
            setTrygdetid={oppdaterStateOgSettTrygdetid}
            avbryt={() => setEndreModus({ status: false, trygdetidGrunnlagId: '' })}
            eksisterendeGrunnlag={trygdetidGrunnlagListe.find((tg) => tg.id == endreModus.trygdetidGrunnlagId)}
            trygdetidGrunnlagType={trygdetidGrunnlagType}
            landListe={landListe}
          />
        ) : (
          <Button size="small" onClick={() => setEndreModus({ status: true, trygdetidGrunnlagId: '' })}>
            Legg til ny periode
          </Button>
        )}
      </NyEllerOppdatertPeriode>
    </GrunnlagListe>
  )
}

const PeriodeRow = ({
  trygdetidGrunnlag,
  behandlingId,
  setTrygdetid,
  endrePeriode,
  landListe,
}: {
  trygdetidGrunnlag: ITrygdetidGrunnlag
  behandlingId: string
  setTrygdetid: (trygdetid: ITrygdetid) => void
  endrePeriode: (trygdetidGrunnlagId: string) => void
  landListe: ILand[]
}) => {
  const [slettTrygdetidStatus, slettTrygdetidsgrunnlagRequest] = useApiCall(slettTrygdetidsgrunnlag)

  const slettGrunnlag = (grunnlagId: string) => {
    slettTrygdetidsgrunnlagRequest(
      {
        behandlingsId: behandlingId,
        trygdetidGrunnlagId: grunnlagId,
      },
      (oppdatertTrygdetid) => {
        setTrygdetid(oppdatertTrygdetid)
      }
    )
  }

  const beregnetTrygdetid = trygdetidGrunnlag?.beregnet
    ? `${trygdetidGrunnlag.beregnet.aar} år ${trygdetidGrunnlag.beregnet.maaneder} måneder ${trygdetidGrunnlag.beregnet.dager} dager`
    : '-'

  return (
    <Table.ExpandableRow
      expansionDisabled={!trygdetidGrunnlag.begrunnelse}
      content={
        <div>
          <Heading size={'small'}>Begrunnelse</Heading>
          {trygdetidGrunnlag.begrunnelse}
        </div>
      }
    >
      <Table.DataCell>
        {landListe.find((land) => land.isoLandkode == trygdetidGrunnlag.bosted)?.beskrivelse?.tekst}
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
        <RedigerWrapper onClick={() => endrePeriode(trygdetidGrunnlag.id!!)}>Rediger</RedigerWrapper>
      </Table.DataCell>
      <Table.DataCell>
        {isPending(slettTrygdetidStatus) ? (
          <Spinner visible={true} variant={'neutral'} label="Sletter" margin={'1em'} />
        ) : (
          <RedigerWrapper onClick={() => slettGrunnlag(trygdetidGrunnlag.id!!)}>Slett</RedigerWrapper>
        )}
        {isFailure(slettTrygdetidStatus) && <ApiErrorAlert>En feil har oppstått</ApiErrorAlert>}
      </Table.DataCell>
    </Table.ExpandableRow>
  )
}

const GrunnlagListe = styled.div`
  margin-top: 2em;
`

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

const NyEllerOppdatertPeriode = styled.div`
  margin-top: 1em;
`
