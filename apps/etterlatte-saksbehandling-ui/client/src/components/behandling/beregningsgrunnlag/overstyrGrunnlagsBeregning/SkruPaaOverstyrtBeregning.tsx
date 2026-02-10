import React, { Dispatch, useState } from 'react'
import { ArrowCirclepathIcon, ArrowsCirclepathIcon } from '@navikt/aksel-icons'
import { Button, Heading, HStack, Select, Textarea, VStack } from '@navikt/ds-react'
import { OverstyrtBeregningKategori } from '~shared/types/OverstyrtBeregning'
import { OverstyrBeregning } from '~shared/types/Beregning'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettOverstyrBeregning } from '~shared/api/beregning'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { oppdaterBehandlingsstatus } from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { useAppDispatch } from '~store/Store'

export const SkruPaaOverstyrtBeregning = ({
  behandlingId,
  setOverstyrt,
}: {
  behandlingId: string
  setOverstyrt: Dispatch<OverstyrBeregning>
}) => {
  const [aarsak, setAarsak] = useState<OverstyrtBeregningKategori>()
  const [begrunnelse, setBegrunnelse] = useState<string>('')

  const [aarsakError, setAarsakError] = useState<string>('')

  const [opprettOverstyrBeregningResult, opprettOverstyrBeregningRequest] = useApiCall(opprettOverstyrBeregning)
  const dispatch = useAppDispatch()

  const overstyrBeregning = () => {
    if (!!aarsak) {
      setAarsakError('')
      opprettOverstyrBeregningRequest(
        {
          behandlingId,
          beskrivelse: begrunnelse,
          kategori: aarsak,
        },
        (result) => {
          if (result) {
            setOverstyrt(result)
            dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.TRYGDETID_OPPDATERT))
          }
        }
      )
    } else setAarsakError('Du må velge en årsak')
  }

  return (
    <VStack gap="space-4" maxWidth="20rem">
      <HStack gap="space-2" align="center">
        <ArrowsCirclepathIcon aria-hidden fontSize="1.5rem" />
        <Heading size="small">Skal beregning overstyres?</Heading>
      </HStack>
      <Select
        label="Årsak for overstyring"
        error={aarsakError}
        onChange={(e) => setAarsak(e.target.value as OverstyrtBeregningKategori)}
      >
        <option value="">Velg årsak</option>
        {Object.entries(OverstyrtBeregningKategori).map(([key, value]) => (
          <option key={key} value={key}>
            {value}
          </option>
        ))}
      </Select>
      <Textarea label="Begrunnelse (valgfri)" onChange={(e) => setBegrunnelse(e.target.value)} />

      {isFailureHandler({
        apiResult: opprettOverstyrBeregningResult,
        errorMessage: 'Feil under overstyring av beregning',
      })}

      <div>
        <Button
          size="small"
          icon={<ArrowCirclepathIcon aria-hidden />}
          loading={isPending(opprettOverstyrBeregningResult)}
          onClick={overstyrBeregning}
        >
          Overstyr
        </Button>
      </div>
    </VStack>
  )
}
