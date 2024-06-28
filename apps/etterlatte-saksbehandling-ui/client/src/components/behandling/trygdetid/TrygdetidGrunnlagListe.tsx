import {
  ILand,
  ITrygdetid,
  ITrygdetidGrunnlag,
  ITrygdetidGrunnlagType,
  slettTrygdetidsgrunnlag,
} from '~shared/api/trygdetid'
import { IconSize } from '~shared/types/Icon'
import { BodyShort, Button, Detail, Heading, HStack, Table, VStack } from '@navikt/ds-react'
import { CalendarIcon } from '@navikt/aksel-icons'
import { formaterDato } from '~utils/formatering/dato'
import React, { useState } from 'react'
import styled from 'styled-components'
import { useApiCall } from '~shared/hooks/useApiCall'
import { TrygdetidGrunnlag } from '~components/behandling/trygdetid/TrygdetidGrunnlag'
import Spinner from '~shared/Spinner'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

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

export const TrygdetidGrunnlagListe = ({
  trygdetid,
  setTrygdetid,
  trygdetidGrunnlagType,
  landListe,
  redigerbar,
}: Props) => {
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
    <VStack gap="4">
      <HStack gap="2">
        <CalendarIcon fontSize={IconSize.DEFAULT} />
        <Heading size="small" level="3">
          {grunnlagTypeTekst} trygdetid
        </Heading>
      </HStack>
      {trygdetidGrunnlagType == ITrygdetidGrunnlagType.FAKTISK ? (
        <BodyShort>
          Legg til aktuell trygdetid fra aktuelle land (inkludert Norge) fra avdøde var 16 år frem til og med måneden
          før hen døde. Hvis trygdetid fra flere land med ulike avtaler, må det foretas beregning innen hver avtale. Huk
          da av for &quot;Ikke med i prorata&quot; for trygdetidsperioder i land som ikke skal med i de ulike
          beregningene. Velg beste alternativ for prorata-beregning.
        </BodyShort>
      ) : (
        <BodyShort>
          Det registreres maks fremtidig trygdetid fra dødsdato til og med kalenderåret avdøde hadde blitt 66 år. Denne
          vil automatisk bli justert i beregningen hvis faktisk trygdetid er mindre enn 4/5 av opptjeningstiden. Hvis
          det er annen grunn for reduksjon av fremtidig trygdetid må perioden redigeres.
        </BodyShort>
      )}
      {trygdetidGrunnlagListe.length > 0 && (
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
                  trygdetidId={trygdetid.id}
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
      )}

      <NyEllerOppdatertPeriode>
        {endreModus.status && (
          <TrygdetidGrunnlag
            trygdetidId={trygdetid.id}
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
    </VStack>
  )
}

const PeriodeRow = ({
  trygdetidGrunnlag,
  trygdetidId,
  behandlingId,
  setTrygdetid,
  endrePeriode,
  landListe,
  redigerbar,
  trygdetidGrunnlagType,
}: {
  trygdetidGrunnlag: ITrygdetidGrunnlag
  behandlingId: string
  trygdetidId: string
  setTrygdetid: (trygdetid: ITrygdetid) => void
  endrePeriode: (trygdetidGrunnlagId: string) => void
  landListe: ILand[]
  redigerbar: boolean
  trygdetidGrunnlagType: ITrygdetidGrunnlagType
}) => {
  const [slettTrygdetidStatus, slettTrygdetidsgrunnlagRequest] = useApiCall(slettTrygdetidsgrunnlag)

  const slettGrunnlag = (trygdetidGrunnlagId: string) => {
    slettTrygdetidsgrunnlagRequest(
      {
        trygdetidId,
        behandlingId,
        trygdetidGrunnlagId,
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
        <Datofelt>{formaterDato(trygdetidGrunnlag.periodeFra)}</Datofelt>
      </Table.DataCell>
      <Table.DataCell>
        <Datofelt>{formaterDato(trygdetidGrunnlag.periodeTil)}</Datofelt>
      </Table.DataCell>
      <Table.DataCell>{beregnetTrygdetid}</Table.DataCell>
      <Table.DataCell>
        <BodyShort>{trygdetidGrunnlag.kilde.ident}</BodyShort>
        <Detail>{`saksbehandler: ${formaterDato(trygdetidGrunnlag.kilde.tidspunkt)}`}</Detail>
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
            {isFailureHandler({
              apiResult: slettTrygdetidStatus,
              errorMessage: 'En feil har oppstått',
            })}
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
