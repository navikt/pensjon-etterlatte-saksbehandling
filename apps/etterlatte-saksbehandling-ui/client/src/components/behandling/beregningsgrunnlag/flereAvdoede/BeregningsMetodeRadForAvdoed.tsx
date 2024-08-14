import React, { useState } from 'react'
import { PeriodisertBeregningsgrunnlag } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { BeregningsGrunnlagPostDto, BeregningsmetodeForAvdoed } from '~shared/types/Beregning'
import { Button, Table } from '@navikt/ds-react'
import { format, startOfMonth } from 'date-fns'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import { BeregningsMetodeForAvdoded } from '~components/behandling/beregningsgrunnlag/flereAvdoede/BeregningsMetodeForAvdoded'
import { isPending } from '~shared/api/apiUtils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreBeregningsGrunnlag } from '~shared/api/beregning'
import { oppdaterBeregningsGrunnlag } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { formaterEnumTilLesbarString } from '~utils/formatering/formatering'

interface Props {
  behandlingId: string
  ident: string
  navn: string
  beregningsMetodeForAvdoed: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed> | undefined
  redigerbar: boolean
  patchGrunnlagOppdaterMetode: (
    nyMetode: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed>
  ) => BeregningsGrunnlagPostDto
  patchGrunnlagSlettMetode: (ident: string) => BeregningsGrunnlagPostDto
}

export const BeregningsMetodeRadForAvdoed = ({
  behandlingId,
  ident,
  beregningsMetodeForAvdoed,
  navn,
  redigerbar,
  patchGrunnlagOppdaterMetode,
  patchGrunnlagSlettMetode,
}: Props) => {
  const dispatch = useAppDispatch()
  const [redigerModus, setRedigerModus] = useState<boolean>(false)
  const [lagreBeregningsgrunnlagResult, lagreBeregningsgrunnlagRequest] = useApiCall(lagreBeregningsGrunnlag)

  return (
    <Table.ExpandableRow
      open={redigerModus}
      onOpenChange={(open) => {
        setRedigerModus(open)
      }}
      key={ident}
      content={
        redigerModus ? (
          <BeregningsMetodeForAvdoded
            ident={ident}
            navn={navn}
            eksisterendeMetode={beregningsMetodeForAvdoed}
            paaAvbryt={() => {
              setRedigerModus(false)
            }}
            oppdaterBeregningsMetodeForAvdoed={oppdaterBeregningsMetodeForAvdoed}
            lagreBeregningsgrunnlagResult={lagreBeregningsgrunnlagResult}
          />
        ) : (
          ''
        )
      }
    >
      <Table.DataCell>{navn}</Table.DataCell>
      <Table.DataCell>
        {beregningsMetodeForAvdoed?.data.beregningsMetode.beregningsMetode
          ? formaterEnumTilLesbarString(beregningsMetodeForAvdoed.data.beregningsMetode.beregningsMetode)
          : 'Metode er ikke satt'}
      </Table.DataCell>
      <Table.DataCell>
        {beregningsMetodeForAvdoed?.fom ? format(startOfMonth(beregningsMetodeForAvdoed.fom), 'yyyy-MM-dd') : '-'}
      </Table.DataCell>
      <Table.DataCell>
        {beregningsMetodeForAvdoed?.tom ? format(startOfMonth(beregningsMetodeForAvdoed.tom), 'yyyy-MM-dd') : '-'}
      </Table.DataCell>
      <Table.DataCell>
        {redigerbar && (
          <>
            <Button
              type="button"
              variant="secondary"
              size="small"
              icon={<PencilIcon aria-hidden />}
              disabled={redigerModus}
              onClick={() => setRedigerModus(true)}
            >
              {beregningsMetodeForAvdoed ? 'Rediger' : 'Legg til'}
            </Button>
          </>
        )}
      </Table.DataCell>
      <Table.DataCell>
        {redigerbar && beregningsMetodeForAvdoed && (
          <Button
            size="small"
            variant="secondary"
            icon={<TrashIcon aria-hidden />}
            loading={isPending(lagreBeregningsgrunnlagResult)}
            onClick={slettBeregningsMetodeForAvdoed}
          >
            Slett
          </Button>
        )}
      </Table.DataCell>
    </Table.ExpandableRow>
  )

  function oppdaterBeregningsMetodeForAvdoed(nyMetode: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed>) {
    lagre(patchGrunnlagOppdaterMetode(nyMetode))
  }

  function slettBeregningsMetodeForAvdoed() {
    lagre(patchGrunnlagSlettMetode(ident))
  }

  function lagre(grunnlag: BeregningsGrunnlagPostDto) {
    lagreBeregningsgrunnlagRequest(
      {
        behandlingId: behandlingId,
        grunnlag,
      },
      () => {
        dispatch(oppdaterBeregningsGrunnlag(grunnlag))
        setRedigerModus(false)
      }
    )
  }
}
