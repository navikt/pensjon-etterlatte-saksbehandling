import React, { Dispatch, SetStateAction } from 'react'
import { BodyShort, Box, Button, Detail, HStack, Label, Table } from '@navikt/ds-react'
import { ILand, ITrygdetid, ITrygdetidGrunnlag } from '~shared/api/trygdetid'
import { formaterDato } from '~utils/formatering/dato'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import { isPending, Result } from '~shared/api/apiUtils'
import { VisRedigerTrygdetid } from '~components/behandling/trygdetid/faktiskTrygdetid/FaktiskTrygdetid'

interface Props {
  fremtidigTrygdetidPerioder: Array<ITrygdetidGrunnlag>
  slettTrygdetid: (trygdetidGrunnlagId: string) => void
  slettTrygdetidResult: Result<ITrygdetid>
  setVisRedigerTrydgetid: Dispatch<SetStateAction<VisRedigerTrygdetid>>
  redigerbar: boolean
  landListe: ILand[]
}

export const FremtidigTrygdetidTable = ({
  fremtidigTrygdetidPerioder,
  slettTrygdetid,
  slettTrygdetidResult,
  setVisRedigerTrydgetid,
  redigerbar,
  landListe,
}: Props) => {
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
        {!!fremtidigTrygdetidPerioder?.length ? (
          fremtidigTrygdetidPerioder.map((fremtidigTrygdetidPeriode: ITrygdetidGrunnlag, index: number) => {
            return (
              <Table.ExpandableRow
                key={index}
                content={
                  <Box maxWidth="42.5rem">
                    <Label>Begrunnelse</Label>
                    <BodyShort>{fremtidigTrygdetidPeriode.begrunnelse}</BodyShort>
                  </Box>
                }
              >
                <Table.DataCell>
                  {landListe.find((land) => land.isoLandkode === fremtidigTrygdetidPeriode.bosted)?.beskrivelse.tekst}
                </Table.DataCell>
                <Table.DataCell>{formaterDato(fremtidigTrygdetidPeriode.periodeFra)}</Table.DataCell>
                <Table.DataCell>{formaterDato(fremtidigTrygdetidPeriode.periodeTil)}</Table.DataCell>
                <Table.DataCell>
                  {fremtidigTrygdetidPeriode.beregnet
                    ? `${fremtidigTrygdetidPeriode.beregnet.aar} år, ${fremtidigTrygdetidPeriode.beregnet.maaneder} måneder, ${fremtidigTrygdetidPeriode.beregnet.dager} dager`
                    : '-'}
                </Table.DataCell>
                <Table.DataCell>
                  <BodyShort>{fremtidigTrygdetidPeriode.kilde.ident}</BodyShort>
                  <Detail>Saksbehandler: {formaterDato(fremtidigTrygdetidPeriode.kilde.tidspunkt)}</Detail>
                </Table.DataCell>
                {redigerbar && (
                  <Table.DataCell>
                    <HStack gap="2" align="center" wrap={false}>
                      <Button
                        size="small"
                        variant="secondary"
                        icon={<PencilIcon aria-hidden />}
                        onClick={() =>
                          setVisRedigerTrydgetid({ vis: true, trydgetidGrunnlagId: fremtidigTrygdetidPeriode.id })
                        }
                      >
                        Rediger
                      </Button>
                      <Button
                        size="small"
                        variant="danger"
                        icon={<TrashIcon aria-hidden />}
                        loading={isPending(slettTrygdetidResult)}
                        onClick={() => slettTrygdetid(`${fremtidigTrygdetidPeriode.id}`)}
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
            <Table.DataCell colSpan={redigerbar ? 7 : 6}>Ingen perioder for fremtidig trygdetid</Table.DataCell>
          </Table.Row>
        )}
      </Table.Body>
    </Table>
  )
}
