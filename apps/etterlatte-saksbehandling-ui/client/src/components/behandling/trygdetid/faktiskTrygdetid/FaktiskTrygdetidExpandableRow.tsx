import React, { Dispatch, SetStateAction } from 'react'
import { ILand, ITrygdetid, ITrygdetidGrunnlag } from '~shared/api/trygdetid'
import { BodyShort, Box, Button, Detail, HStack, Label, Table } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import { isPending, Result } from '~shared/api/apiUtils'
import { VisRedigerTrygdetid } from '~components/behandling/trygdetid/faktiskTrygdetid/FaktiskTrygdetid'

interface Props {
  faktiskTrygdetidPeriode: ITrygdetidGrunnlag
  slettTrygdetid: (trygdetidGrunnlagId: string) => void
  slettTrygdetidResult: Result<ITrygdetid>
  setVisRedigerTrydgetid: Dispatch<SetStateAction<VisRedigerTrygdetid>>
  landListe: ILand[]
  redigerbar: boolean
}

export const FaktiskTrygdetidExpandableRow = ({
  faktiskTrygdetidPeriode,
  slettTrygdetid,
  slettTrygdetidResult,
  setVisRedigerTrydgetid,
  landListe,
  redigerbar,
}: Props) => {
  return (
    <Table.ExpandableRow
      content={
        <HStack gap="8">
          <Box maxWidth="7rem">
            <Label>Begrunnelse</Label>
            <BodyShort>{faktiskTrygdetidPeriode.begrunnelse}</BodyShort>
          </Box>
          <div>
            <Label>Poeng i inn år</Label>
            <BodyShort>{faktiskTrygdetidPeriode.poengInnAar ? 'Ja' : 'Nei'}</BodyShort>
          </div>
          <div>
            <Label>Poeng i ut år</Label>
            <BodyShort>{faktiskTrygdetidPeriode.poengUtAar ? 'Ja' : 'Nei'}</BodyShort>
          </div>
          <div>
            <Label>Ikke med i prorata</Label>
            <BodyShort>{faktiskTrygdetidPeriode.prorata ? 'Nei' : 'Ja'}</BodyShort>
          </div>
        </HStack>
      }
    >
      <Table.DataCell>
        {landListe.find((land) => land.isoLandkode === faktiskTrygdetidPeriode.bosted)?.beskrivelse.tekst}
      </Table.DataCell>
      <Table.DataCell>{formaterDato(faktiskTrygdetidPeriode.periodeFra)}</Table.DataCell>
      <Table.DataCell>{formaterDato(faktiskTrygdetidPeriode.periodeTil)}</Table.DataCell>
      <Table.DataCell>
        {faktiskTrygdetidPeriode.beregnet
          ? `${faktiskTrygdetidPeriode.beregnet.aar} år, ${faktiskTrygdetidPeriode.beregnet.maaneder} måneder, ${faktiskTrygdetidPeriode.beregnet.dager} dager`
          : '-'}
      </Table.DataCell>
      <Table.DataCell>
        <BodyShort>{faktiskTrygdetidPeriode.kilde.ident}</BodyShort>
        <Detail>Saksbehandler: {formaterDato(faktiskTrygdetidPeriode.kilde.tidspunkt)}</Detail>
      </Table.DataCell>
      {redigerbar && (
        <Table.DataCell>
          <HStack gap="2" align="center" wrap={false}>
            <Button
              size="small"
              variant="secondary"
              icon={<PencilIcon aria-hidden />}
              onClick={() => setVisRedigerTrydgetid({ vis: true, trydgetidGrunnlagId: faktiskTrygdetidPeriode.id })}
            >
              Rediger
            </Button>
            <Button
              size="small"
              variant="danger"
              icon={<TrashIcon aria-hidden />}
              loading={isPending(slettTrygdetidResult)}
              onClick={() => slettTrygdetid(`${faktiskTrygdetidPeriode.id}`)}
            >
              Slett
            </Button>
          </HStack>
        </Table.DataCell>
      )}
    </Table.ExpandableRow>
  )
}
