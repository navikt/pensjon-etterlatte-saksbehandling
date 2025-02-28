import { ViderefoertOpphoer } from '~shared/types/IDetaljertBehandling'
import { BodyLong, BodyShort, Box, Button, Detail, Heading, HStack, VStack } from '@navikt/ds-react'
import React, { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentVilkaartyper } from '~shared/api/vilkaarsvurdering'
import { JaNei, JaNeiRec } from '~shared/types/ISvar'
import { isPending, isSuccess } from '~shared/api/apiUtils'
import { formaterDato, formaterDatoMedTidspunkt } from '~utils/formatering/dato'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import { slettViderefoertOpphoer } from '~shared/api/behandling'
import { resetViderefoertOpphoer } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { KildeSaksbehandler } from '~shared/types/kilde'

export const ViderefoereOpphoerVisning = ({
  viderefoertOpphoer,
  behandlingId,
  setVisVurdering,
}: {
  viderefoertOpphoer: ViderefoertOpphoer
  behandlingId: string
  setVisVurdering: (visVurdering: boolean) => void
}) => {
  const dispatch = useAppDispatch()
  const [hentVilkaartyperStatus, hentVilkaartyperRequest] = useApiCall(hentVilkaartyper)
  const [slettViderefoertOpphoerStatus, slettViderefoertOpphoerRequest] = useApiCall(slettViderefoertOpphoer)

  useEffect(() => {
    hentVilkaartyperRequest(behandlingId)
  }, [behandlingId])

  const slett = () =>
    slettViderefoertOpphoerRequest({ behandlingId: behandlingId }, () => {
      dispatch(resetViderefoertOpphoer())
    })

  return (
    <Box paddingBlock="0 8">
      <Heading level="3" size="small">
        Er det nødvendig å fastsette til og med-dato?
      </Heading>

      <VStack gap="5">
        <VurderingKilde kilde={viderefoertOpphoer.kilde} />

        <Box>
          <BodyShort size="small">{JaNeiRec[viderefoertOpphoer.skalViderefoere]}</BodyShort>
        </Box>

        {viderefoertOpphoer.skalViderefoere == JaNei.JA && (
          <>
            <Box>
              <Heading size="xsmall">Opphørstidspunkt</Heading>
              <BodyShort>{formaterDato(viderefoertOpphoer.dato)}</BodyShort>
            </Box>
            <Box>
              <Heading size="xsmall">Vilkår som ikke lenger er oppfylt</Heading>
              <BodyShort>
                {isSuccess(hentVilkaartyperStatus) &&
                  hentVilkaartyperStatus.data.typer.find((n) => n.name == viderefoertOpphoer.vilkaar)?.tittel}
              </BodyShort>
            </Box>
          </>
        )}
        {viderefoertOpphoer.begrunnelse && (
          <Box>
            <Heading size="xsmall">Begrunnelse</Heading>
            <BodyLong>{viderefoertOpphoer.begrunnelse}</BodyLong>
          </Box>
        )}

        <HStack gap="3">
          <Button icon={<PencilIcon />} size="small" variant="tertiary" onClick={() => setVisVurdering(true)}>
            Rediger
          </Button>
          <Button
            icon={<TrashIcon />}
            loading={isPending(slettViderefoertOpphoerStatus)}
            size="small"
            variant="tertiary"
            onClick={slett}
          >
            Slett
          </Button>
        </HStack>

        {isFailureHandler({
          apiResult: slettViderefoertOpphoerStatus,
          errorMessage: 'Kunne ikke slette videreført opphør',
        })}
      </VStack>
    </Box>
  )
}

const VurderingKilde = ({ kilde }: { kilde: KildeSaksbehandler }) => {
  return (
    <VStack>
      <Detail>Manuelt av {kilde.ident}</Detail>
      <Detail>Sist endret {kilde.tidspunkt ? formaterDatoMedTidspunkt(new Date(kilde.tidspunkt)) : '-'}</Detail>
    </VStack>
  )
}
