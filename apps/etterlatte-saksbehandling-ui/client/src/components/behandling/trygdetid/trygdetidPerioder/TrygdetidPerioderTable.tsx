import React, { useState } from 'react'
import { ITrygdetid, ITrygdetidGrunnlag, ITrygdetidGrunnlagType } from '~shared/api/trygdetid'
import { isPending, Result } from '~shared/api/apiUtils'
import { BodyShort, Box, Button, Detail, HStack, Label, Table } from '@navikt/ds-react'
import { FaktiskTrygdetidExpandableRowContent } from '~components/behandling/trygdetid/trygdetidPerioder/components/FaktiskTrygdetidExpandableRowContent'
import { formaterEnumTilLesbarString } from '~utils/formatering/formatering'
import { formaterDato } from '~utils/formatering/dato'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import { TrygdetidGrunnlag } from '~components/behandling/trygdetid/TrygdetidGrunnlag'
import { ILand } from '~utils/kodeverk'
import { TekstMedMellomrom } from '~shared/TekstMedMellomrom'

interface PeriodeRedigeringModus {
  redigerPeriode: boolean
  erAapen: boolean
  trygdetidGrunnlagId: string
}

const defaultPeriodeRedigeringModus: PeriodeRedigeringModus = {
  redigerPeriode: false,
  erAapen: false,
  trygdetidGrunnlagId: '',
}

interface Props {
  trygdetidId: string
  trygdetidPerioder: Array<ITrygdetidGrunnlag>
  trygdetidGrunnlagType: ITrygdetidGrunnlagType
  oppdaterTrygdetid: (trygdetid: ITrygdetid) => void
  slettTrygdetid: (trygdetidGrunnlagId: string) => void
  slettTrygdetidResult: Result<ITrygdetid>
  landListe: ILand[]
  redigerbar: boolean
}

export const TrygdetidPerioderTable = ({
  trygdetidId,
  trygdetidPerioder,
  trygdetidGrunnlagType,
  oppdaterTrygdetid,
  slettTrygdetid,
  slettTrygdetidResult,
  landListe,
  redigerbar,
}: Props) => {
  const [periodeRedigeringModus, setPeriodeRedigeringModus] =
    useState<PeriodeRedigeringModus>(defaultPeriodeRedigeringModus)

  return (
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
        {!!trygdetidPerioder?.length ? (
          trygdetidPerioder.map((trygdetidPeriode: ITrygdetidGrunnlag, index: number) => {
            return (
              <Table.ExpandableRow
                key={index}
                open={
                  periodeRedigeringModus.erAapen && trygdetidPeriode.id === periodeRedigeringModus.trygdetidGrunnlagId
                }
                onOpenChange={(open) =>
                  setPeriodeRedigeringModus({
                    redigerPeriode: open && periodeRedigeringModus.redigerPeriode,
                    erAapen: open,
                    trygdetidGrunnlagId: trygdetidPeriode.id,
                  })
                }
                content={
                  !periodeRedigeringModus.redigerPeriode ? (
                    trygdetidGrunnlagType === ITrygdetidGrunnlagType.FAKTISK ? (
                      <FaktiskTrygdetidExpandableRowContent trygdetidPeriode={trygdetidPeriode} />
                    ) : (
                      <Box maxWidth="42.5rem">
                        <Label>Begrunnelse</Label>
                        <TekstMedMellomrom>{trygdetidPeriode.begrunnelse}</TekstMedMellomrom>
                      </Box>
                    )
                  ) : (
                    <TrygdetidGrunnlag
                      eksisterendeGrunnlag={trygdetidPeriode}
                      trygdetidId={trygdetidId}
                      setTrygdetid={(trygdetid) => {
                        oppdaterTrygdetid(trygdetid)
                        setPeriodeRedigeringModus(defaultPeriodeRedigeringModus)
                      }}
                      avbryt={() => setPeriodeRedigeringModus(defaultPeriodeRedigeringModus)}
                      trygdetidGrunnlagType={trygdetidGrunnlagType}
                      landListe={landListe}
                    />
                  )
                }
              >
                <Table.DataCell>
                  {landListe.find((land) => land.isoLandkode === trygdetidPeriode.bosted)?.beskrivelse.tekst}
                </Table.DataCell>
                <Table.DataCell>{formaterDato(trygdetidPeriode.periodeFra)}</Table.DataCell>
                <Table.DataCell>{formaterDato(trygdetidPeriode.periodeTil)}</Table.DataCell>
                <Table.DataCell>
                  {trygdetidPeriode.beregnet
                    ? `${trygdetidPeriode.beregnet.aar} år, ${trygdetidPeriode.beregnet.maaneder} måneder, ${trygdetidPeriode.beregnet.dager} dager`
                    : '-'}
                </Table.DataCell>
                <Table.DataCell>
                  <BodyShort>{trygdetidPeriode.kilde.ident}</BodyShort>
                  <Detail>Saksbehandler: {formaterDato(trygdetidPeriode.kilde.tidspunkt)}</Detail>
                </Table.DataCell>
                {redigerbar && (
                  <Table.DataCell>
                    <HStack gap="2" align="center" wrap={false}>
                      <Button
                        size="small"
                        variant="secondary"
                        icon={<PencilIcon aria-hidden />}
                        disabled={
                          periodeRedigeringModus.redigerPeriode &&
                          trygdetidPeriode.id === periodeRedigeringModus.trygdetidGrunnlagId
                        }
                        onClick={() =>
                          setPeriodeRedigeringModus({
                            redigerPeriode: true,
                            erAapen: true,
                            trygdetidGrunnlagId: trygdetidPeriode.id,
                          })
                        }
                      >
                        Rediger
                      </Button>
                      <Button
                        size="small"
                        variant="secondary"
                        icon={<TrashIcon aria-hidden />}
                        loading={isPending(slettTrygdetidResult)}
                        onClick={() => slettTrygdetid(`${trygdetidPeriode.id}`)}
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
            <Table.DataCell colSpan={redigerbar ? 7 : 6}>
              Ingen perioder for {formaterEnumTilLesbarString(trygdetidGrunnlagType).toLowerCase()} trygdetid
            </Table.DataCell>
          </Table.Row>
        )}
      </Table.Body>
    </Table>
  )
}
