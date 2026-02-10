import { HandshakeIcon, PencilIcon } from '@navikt/aksel-icons'
import { Box, Button, Heading, HStack, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import Spinner from '~shared/Spinner'
import {
  hentAlleTrygdetidAvtaleKriterier,
  hentAlleTrygdetidAvtaler,
  hentTrygdeavtaleForBehandling,
  Trygdeavtale,
  TrygdetidAvtale,
  TrygdetidAvtaleKriteria,
  TrygdetidAvtaleOptions,
} from '~shared/api/trygdetid'
import { useApiCall } from '~shared/hooks/useApiCall'
import { IconSize } from '~shared/types/Icon'
import { TrygdeavtaleVisning } from './TrygdeavtaleVisning'
import { isPending, isSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { AvdoedesTrygdetidReadMore } from '~components/behandling/trygdetid/components/AvdoedesTrygdetidReadMore'
import { ApiErrorAlert } from '~ErrorBoundary'
import { TrygdeAvtaleRedigering } from '~components/behandling/trygdetid/avtaler/TrygdeAvtaleRedigering'

interface Props {
  redigerbar: boolean
}

export const TrygdeAvtale = ({ redigerbar }: Props) => {
  const { behandlingId } = useParams()
  const [hentAlleTrygdetidAvtalerRequest, fetchTrygdetidAvtaler] = useApiCall(hentAlleTrygdetidAvtaler)
  const [hentAlleTrygdetidAvtalerKriterierRequest, fetchTrygdetidAvtaleKriterier] = useApiCall(
    hentAlleTrygdetidAvtaleKriterier
  )
  const [hentTrygdeavtaleRequest, fetchTrygdeavtale] = useApiCall(hentTrygdeavtaleForBehandling)
  const [avtalerListe, setAvtalerListe] = useState<TrygdetidAvtale[]>()
  const [avtaleKriterierListe, setAvtaleKriterierListe] = useState<TrygdetidAvtaleKriteria[]>()
  const [trygdeavtale, setTrygdeavtale] = useState<Trygdeavtale | undefined>(undefined)
  const [redigering, setRedigering] = useState<boolean>(true)

  useEffect(() => {
    fetchTrygdetidAvtaler(null, (avtaler: TrygdetidAvtale[]) => {
      setAvtalerListe(avtaler.sort((a: TrygdetidAvtale, b: TrygdetidAvtale) => trygdeavtaleOptionSort(a, b)))
    })

    fetchTrygdetidAvtaleKriterier(null, (avtaler: TrygdetidAvtaleKriteria[]) => {
      setAvtaleKriterierListe(
        avtaler.sort((a: TrygdetidAvtaleKriteria, b: TrygdetidAvtaleKriteria) => trygdeavtaleOptionSort(a, b))
      )
    })

    if (behandlingId) {
      fetchTrygdeavtale({ behandlingId: behandlingId }, (avtale: Trygdeavtale) => {
        if (avtale.avtaleKode) {
          setTrygdeavtale(avtale)
          setRedigering(false)
        }
      })
    }
  }, [])

  const oppdaterTrygdeavtale = (trygdeavtale: Trygdeavtale) => {
    setRedigering(false)
    setTrygdeavtale(trygdeavtale)
  }

  const trygdeavtaleOptionSort = (a: TrygdetidAvtaleOptions, b: TrygdetidAvtaleOptions) => {
    if (a.beskrivelse > b.beskrivelse) {
      return 1
    }
    return -1
  }

  return (
    <Box paddingBlock="space-8 space-0">
      <VStack gap="space-4">
        <HStack gap="space-2" align="center">
          <HandshakeIcon fontSize={IconSize.DEFAULT} />
          <Heading size="small" level="3">
            Vurdering av trygdeavtale (avdød)
          </Heading>
        </HStack>

        <AvdoedesTrygdetidReadMore />
        {!redigering &&
          avtalerListe &&
          avtaleKriterierListe &&
          (trygdeavtale ? (
            <>
              <TrygdeavtaleVisning
                avtaler={avtalerListe}
                kriterier={avtaleKriterierListe}
                trygdeavtale={trygdeavtale}
              />
              {redigerbar && (
                <div>
                  <Button
                    size="small"
                    variant="secondary"
                    onClick={() => setRedigering(true)}
                    type="button"
                    icon={<PencilIcon aria-hidden />}
                  >
                    Rediger avtale
                  </Button>
                </div>
              )}
            </>
          ) : (
            <ApiErrorAlert>
              Kunne ikke vise trygdeavtale. Meld sak i porten hvis dette ikke løser seg selv ved å laste siden på nytt
            </ApiErrorAlert>
          ))}
        {isSuccess(hentAlleTrygdetidAvtalerRequest) &&
          isSuccess(hentAlleTrygdetidAvtalerKriterierRequest) &&
          isSuccess(hentTrygdeavtaleRequest) &&
          redigerbar &&
          redigering && (
            <TrygdeAvtaleRedigering
              trygdeavtale={trygdeavtale ?? {}}
              avtaler={avtalerListe ?? []}
              kriterier={avtaleKriterierListe ?? []}
              oppdaterAvtale={oppdaterTrygdeavtale}
              avbryt={() => setRedigering(false)}
            />
          )}
        {(isPending(hentAlleTrygdetidAvtalerRequest) ||
          isPending(hentAlleTrygdetidAvtalerKriterierRequest) ||
          isPending(hentTrygdeavtaleRequest)) && <Spinner label="Henter trygdeavtaler" />}

        {isFailureHandler({
          apiResult: hentAlleTrygdetidAvtalerRequest,
          errorMessage: 'En feil har oppstått ved henting av trygdeavtaler',
        })}
        {isFailureHandler({
          apiResult: hentAlleTrygdetidAvtalerKriterierRequest,
          errorMessage: 'En feil har oppstått ved henting av trygdeavtalekriterier',
        })}
        {isFailureHandler({
          apiResult: hentTrygdeavtaleRequest,
          errorMessage: 'En feil har oppstått ved henting av trygdeavtale for behandlingen',
        })}
      </VStack>
    </Box>
  )
}
