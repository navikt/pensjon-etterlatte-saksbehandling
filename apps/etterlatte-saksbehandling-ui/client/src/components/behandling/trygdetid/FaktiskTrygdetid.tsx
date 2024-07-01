import React, { useState } from 'react'
import { BodyShort, Box, Button, Detail, Heading, HStack, Label, Table, VStack } from '@navikt/ds-react'
import { CalendarIcon, PencilIcon, PlusIcon, TrashIcon } from '@navikt/aksel-icons'
import {
  ILand,
  ITrygdetid,
  ITrygdetidGrunnlag,
  ITrygdetidGrunnlagType,
  slettTrygdetidsgrunnlag,
} from '~shared/api/trygdetid'
import { formaterDato } from '~utils/formatering/dato'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { isPending } from '~shared/api/apiUtils'
import { TrygdetidGrunnlag } from '~components/behandling/trygdetid/TrygdetidGrunnlag'

interface VisRedigerTrygdetid {
  vis: boolean
  trydgetidGrunnlagId: string
}

const initialVisRedigerTrygdetid = {
  vis: false,
  trydgetidGrunnlagId: '',
}

interface Props {
  trygdetid: ITrygdetid
  oppdaterTrygdetid: (trygdetid: ITrygdetid) => void
  landListe: ILand[]
  redigerbar: boolean
}

export const FaktiskTrygdetid = ({ redigerbar, trygdetid, oppdaterTrygdetid, landListe }: Props) => {
  const [slettTrygdetidResult, slettTrygdetidsgrunnlagRequest] = useApiCall(slettTrygdetidsgrunnlag)

  const [visRedigerTrydgetid, setVisRedigerTrydgetid] = useState<VisRedigerTrygdetid>(initialVisRedigerTrygdetid)

  const faktiskTrygdetidPerioder = trygdetid.trygdetidGrunnlag
    .filter((trygdetid) => trygdetid.type === ITrygdetidGrunnlagType.FAKTISK)
    .sort((a, b) => (a.periodeFra > b.periodeFra ? 1 : -1))

  const kanLeggeTilNyTrydgetidPeriode = redigerbar && !visRedigerTrydgetid.vis

  const slettTrygdetid = (trygdetidGrunnlagId: string) => {
    slettTrygdetidsgrunnlagRequest(
      {
        trygdetidId: trygdetid.id,
        behandlingId: trygdetid.behandlingId,
        trygdetidGrunnlagId,
      },
      oppdaterTrygdetid
    )
  }

  return (
    <VStack gap="4">
      <HStack gap="2" align="center">
        <CalendarIcon aria-hidden height="1.5rem" width="1.5rem" />
        <Heading size="small">Faktisk trygdetid</Heading>
      </HStack>
      <BodyShort>
        Legg til aktuell trygdetid fra aktuelle land (inkludert Norge) fra avdøde var 16 år frem til og med måneden før
        hen døde. Hvis trygdetid fra flere land med ulike avtaler, må det foretas beregning innen hver avtale. Huk da av
        for &quot;Ikke med i prorata&quot; for trygdetidsperioder i land som ikke skal med i de ulike beregningene. Velg
        beste alternativ for prorata-beregning.
      </BodyShort>

      <Table size="small">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell />
            <Table.HeaderCell scope="col">Land</Table.HeaderCell>
            <Table.HeaderCell scope="col">Fra dato</Table.HeaderCell>
            <Table.HeaderCell scope="col">Til dato</Table.HeaderCell>
            <Table.HeaderCell scope="col">Faktisk trygdetid</Table.HeaderCell>
            <Table.HeaderCell scope="col">Kilde</Table.HeaderCell>
            {redigerbar && <Table.HeaderCell scope="col" />}
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!faktiskTrygdetidPerioder?.length ? (
            faktiskTrygdetidPerioder.map((trydgetidGrunnlagPeriode: ITrygdetidGrunnlag, index: number) => {
              return (
                <Table.ExpandableRow
                  key={index}
                  content={
                    <HStack gap="8">
                      <Box maxWidth="7rem">
                        <Label>Begrunnelse</Label>
                        <BodyShort>{trydgetidGrunnlagPeriode.begrunnelse}</BodyShort>
                      </Box>
                      <div>
                        <Label>Poeng i inn år</Label>
                        <BodyShort>{trydgetidGrunnlagPeriode.poengInnAar ? 'Ja' : 'Nei'}</BodyShort>
                      </div>
                      <div>
                        <Label>Poeng i ut år</Label>
                        <BodyShort>{trydgetidGrunnlagPeriode.poengUtAar ? 'Ja' : 'Nei'}</BodyShort>
                      </div>
                      <div>
                        <Label>Ikke med i prorata</Label>
                        <BodyShort>{trydgetidGrunnlagPeriode.prorata ? 'Nei' : 'Ja'}</BodyShort>
                      </div>
                    </HStack>
                  }
                >
                  <Table.DataCell>
                    {landListe.find((land) => land.isoLandkode == trydgetidGrunnlagPeriode.bosted)?.beskrivelse.tekst}
                  </Table.DataCell>
                  <Table.DataCell>{formaterDato(trydgetidGrunnlagPeriode.periodeFra)}</Table.DataCell>
                  <Table.DataCell>{formaterDato(trydgetidGrunnlagPeriode.periodeTil)}</Table.DataCell>
                  <Table.DataCell>
                    {trydgetidGrunnlagPeriode.beregnet
                      ? `${trydgetidGrunnlagPeriode.beregnet.aar} år ${trydgetidGrunnlagPeriode.beregnet.maaneder} måneder ${trydgetidGrunnlagPeriode.beregnet.dager} dager`
                      : '-'}
                  </Table.DataCell>
                  <Table.DataCell>
                    <BodyShort>{trydgetidGrunnlagPeriode.kilde.ident}</BodyShort>
                    <Detail>Saksbehandler: {formaterDato(trydgetidGrunnlagPeriode.kilde.tidspunkt)}</Detail>
                  </Table.DataCell>
                  {redigerbar && (
                    <Table.DataCell>
                      <HStack gap="2" align="center">
                        <Button
                          size="small"
                          variant="secondary"
                          icon={<PencilIcon aria-hidden />}
                          onClick={() =>
                            setVisRedigerTrydgetid({ vis: true, trydgetidGrunnlagId: trydgetidGrunnlagPeriode.id })
                          }
                        >
                          Rediger
                        </Button>
                        <Button
                          size="small"
                          variant="danger"
                          icon={<TrashIcon aria-hidden />}
                          loading={isPending(slettTrygdetidResult)}
                          onClick={() => slettTrygdetid(trydgetidGrunnlagPeriode.id)}
                        >
                          Slett
                        </Button>
                      </HStack>
                    </Table.DataCell>
                  )}
                </Table.ExpandableRow>
              )
            })
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={redigerbar ? 7 : 6}>Ingen perioder for faktisk trygdetid</Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>

      {isFailureHandler({ apiResult: slettTrygdetidResult, errorMessage: 'Feil oppstått i slettingen av trygdetid' })}

      {visRedigerTrydgetid.vis && (
        <TrygdetidGrunnlag
          trygdetidId={trygdetid.id}
          setTrygdetid={(trygdetid) => {
            setVisRedigerTrydgetid(initialVisRedigerTrygdetid)
            oppdaterTrygdetid(trygdetid)
          }}
          avbryt={() => setVisRedigerTrydgetid(initialVisRedigerTrygdetid)}
          eksisterendeGrunnlag={faktiskTrygdetidPerioder.find(
            (trygdetid) => trygdetid.id == visRedigerTrydgetid.trydgetidGrunnlagId
          )}
          trygdetidGrunnlagType={ITrygdetidGrunnlagType.FAKTISK}
          landListe={landListe}
        />
      )}

      {kanLeggeTilNyTrydgetidPeriode && (
        <div>
          <Button size="small" variant="secondary" icon={<PlusIcon aria-hidden />}>
            Ny periode
          </Button>
        </div>
      )}
    </VStack>
  )
}
