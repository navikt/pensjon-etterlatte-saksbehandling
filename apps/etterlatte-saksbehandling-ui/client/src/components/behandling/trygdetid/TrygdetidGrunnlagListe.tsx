import {
  ILand,
  ITrygdetid,
  ITrygdetidGrunnlag,
  ITrygdetidGrunnlagType,
  slettTrygdetidsgrunnlag,
} from '~shared/api/trygdetid'
import { FlexHeader, IconWrapper, TableWrapper } from '~components/behandling/soeknadsoversikt/familieforhold/styled'
import { IconSize } from '~shared/types/Icon'
import { BodyShort, Button, Detail, Heading, Table } from '@navikt/ds-react'
import { CalendarIcon } from '@navikt/aksel-icons'
import { formaterStringDato } from '~utils/formattering'
import React, { useState } from 'react'
import styled from 'styled-components'
import { useApiCall } from '~shared/hooks/useApiCall'
import { TrygdetidGrunnlag } from '~components/behandling/trygdetid/TrygdetidGrunnlag'
import { ApiErrorAlert } from '~ErrorBoundary'
import Spinner from '~shared/Spinner'

import { isFailure, isPending } from '~shared/api/apiUtils'

type Props = {
  trygdetid: ITrygdetid
  setTrygdetid: (trygdetid: ITrygdetid) => void
  trygdetidGrunnlagType: ITrygdetidGrunnlagType
  landListe: ILand[]
  redigerbar: boolean
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
  redigerbar,
}) => {
  const [endreModus, setEndreModus] = useState(initialEndreModusState)
  const trygdetidGrunnlagListe = trygdetid.trygdetidGrunnlag
    .filter((tg) => tg.type == trygdetidGrunnlagType)
    .sort((a, b) => (a.periodeFra > b.periodeFra ? 1 : -1))
  const grunnlagTypeTekst = trygdetidGrunnlagType == ITrygdetidGrunnlagType.FAKTISK ? 'Faktisk' : 'Fremtidig'

  const oppdaterStateOgSettTrygdetid = (trygdetid: ITrygdetid) => {
    setEndreModus(initialEndreModusState)
    setTrygdetid(trygdetid)
  }

  const leggTilNyPeriodeTilgjengelig =
    redigerbar &&
    !endreModus.status &&
    !(trygdetidGrunnlagType === ITrygdetidGrunnlagType.FREMTIDIG && trygdetidGrunnlagListe.length > 0)

  return (
    <GrunnlagListe>
      <FlexHeader>
        <IconWrapper>
          <CalendarIcon fontSize={IconSize.DEFAULT} />
        </IconWrapper>
        <Heading size="small" level="3">
          {grunnlagTypeTekst} trygdetid
        </Heading>
      </FlexHeader>
      {trygdetidGrunnlagType == ITrygdetidGrunnlagType.FAKTISK ? (
        <p>
          Legg til aktuell trygdetid fra aktuelle land (inkludert Norge) fra avdøde var 16 år frem til og med måneden
          før hen døde. Hvis trygdetid fra flere land med ulike avtaler, må det foretas beregning innen hver avtale. Huk
          da av for &quot;Ikke med i prorata&quot; for trygdetidsperioder i land som ikke skal med i de ulike
          beregningene. Velg beste alternativ for prorata-beregning.
        </p>
      ) : (
        <p>
          Det registreres maks fremtidig trygdetid fra dødsdato til og med kalenderåret avdøde hadde blitt 66 år. Denne
          vil automatisk bli justert i beregningen hvis faktisk trygdetid er mindre enn 4/5 av opptjeningstiden. Hvis
          det er annen grunn for reduksjon av fremtidig trygdetid må perioden redigeres.
        </p>
      )}
      {trygdetidGrunnlagListe.length > 0 && (
        <TableWrapper>
          <Table size="medium">
            <Table.Header>
              <Table.Row>
                <Table.HeaderCell />
                <Table.HeaderCell scope="col">Land</Table.HeaderCell>
                <Table.HeaderCell scope="col">Fra dato</Table.HeaderCell>
                <Table.HeaderCell scope="col">Til dato</Table.HeaderCell>
                <Table.HeaderCell scope="col">{grunnlagTypeTekst} trygdetid</Table.HeaderCell>
                <Table.HeaderCell scope="col">Kilde</Table.HeaderCell>
                {redigerbar && (
                  <>
                    <Table.HeaderCell scope="col"> </Table.HeaderCell>
                    <Table.HeaderCell scope="col"> </Table.HeaderCell>
                  </>
                )}
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
                    redigerbar={redigerbar}
                    trygdetidGrunnlagType={trygdetidGrunnlagType}
                  />
                )
              })}
            </Table.Body>
          </Table>
        </TableWrapper>
      )}

      <NyEllerOppdatertPeriode>
        {endreModus.status && (
          <TrygdetidGrunnlag
            setTrygdetid={oppdaterStateOgSettTrygdetid}
            avbryt={() => setEndreModus({ status: false, trygdetidGrunnlagId: '' })}
            eksisterendeGrunnlag={trygdetidGrunnlagListe.find((tg) => tg.id == endreModus.trygdetidGrunnlagId)}
            trygdetidGrunnlagType={trygdetidGrunnlagType}
            landListe={landListe}
          />
        )}
        {leggTilNyPeriodeTilgjengelig && (
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
  redigerbar,
  trygdetidGrunnlagType,
}: {
  trygdetidGrunnlag: ITrygdetidGrunnlag
  behandlingId: string
  setTrygdetid: (trygdetid: ITrygdetid) => void
  endrePeriode: (trygdetidGrunnlagId: string) => void
  landListe: ILand[]
  redigerbar: boolean
  trygdetidGrunnlagType: ITrygdetidGrunnlagType
}) => {
  const [slettTrygdetidStatus, slettTrygdetidsgrunnlagRequest] = useApiCall(slettTrygdetidsgrunnlag)

  const slettGrunnlag = (grunnlagId: string) => {
    slettTrygdetidsgrunnlagRequest(
      {
        behandlingId: behandlingId,
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
      content={
        <>
          <ExpandableInfo>
            <Heading size="small">Begrunnelse</Heading>
            {trygdetidGrunnlag.begrunnelse}
          </ExpandableInfo>
          {trygdetidGrunnlagType === ITrygdetidGrunnlagType.FAKTISK && (
            <>
              <ExpandableInfo>
                <Heading size="small">Poeng i inn år</Heading>
                {trygdetidGrunnlag.poengInnAar ? 'Ja' : 'Nei'}
              </ExpandableInfo>
              <ExpandableInfo>
                <Heading size="small">Poeng i ut år</Heading>
                {trygdetidGrunnlag.poengUtAar ? 'Ja' : 'Nei'}
              </ExpandableInfo>
              <ExpandableInfo>
                <Heading size="small">Ikke med i prorata</Heading>
                {trygdetidGrunnlag.prorata ? 'Nei' : 'Ja'}
              </ExpandableInfo>
            </>
          )}
        </>
      }
    >
      <Table.DataCell>
        {landListe.find((land) => land.isoLandkode == trygdetidGrunnlag.bosted)?.beskrivelse?.tekst}
      </Table.DataCell>
      <Table.DataCell>
        <Datofelt>{formaterStringDato(trygdetidGrunnlag.periodeFra)}</Datofelt>
      </Table.DataCell>
      <Table.DataCell>
        <Datofelt>{formaterStringDato(trygdetidGrunnlag.periodeTil)}</Datofelt>
      </Table.DataCell>
      <Table.DataCell>{beregnetTrygdetid}</Table.DataCell>
      <Table.DataCell>
        <BodyShort>{trygdetidGrunnlag.kilde.ident}</BodyShort>
        <Detail>{`saksbehandler: ${formaterStringDato(trygdetidGrunnlag.kilde.tidspunkt)}`}</Detail>
      </Table.DataCell>
      {redigerbar && (
        <>
          <Table.DataCell>
            <RedigerWrapper onClick={() => endrePeriode(trygdetidGrunnlag.id)}>Rediger</RedigerWrapper>
          </Table.DataCell>
          <Table.DataCell>
            {isPending(slettTrygdetidStatus) ? (
              <Spinner visible={true} variant="neutral" label="Sletter" margin="1em" />
            ) : (
              <RedigerWrapper onClick={() => slettGrunnlag(trygdetidGrunnlag.id)}>Slett</RedigerWrapper>
            )}
            {isFailure(slettTrygdetidStatus) && <ApiErrorAlert>En feil har oppstått</ApiErrorAlert>}
          </Table.DataCell>
        </>
      )}
    </Table.ExpandableRow>
  )
}

const ExpandableInfo = styled.div`
  display: inline-block;
  margin-right: 50px;
`

const GrunnlagListe = styled.div`
  margin-top: 2em;
  margin-bottom: 2em;
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
