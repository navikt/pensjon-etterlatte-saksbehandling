import { mapAllApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { hentBehandling } from '~shared/api/behandling'
import { useEffect } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Utenlandstilsnitt } from '~components/behandling/soeknadsoversikt/soeknadoversikt/utenlandstilsnitt/Utenlandstilsnitt'
import { BoddEllerArbeidetUtlandet } from '~components/behandling/soeknadsoversikt/soeknadoversikt/boddEllerArbeidetUtlandet/BoddEllerArbeidetUtlandet'

export default function SluttbehandlingUtlandFelter({ tilknyttetBehandling }: { tilknyttetBehandling: string }) {
  const [fetchBehandlingStatus, fetchBehandling] = useApiCall(hentBehandling)

  useEffect(() => {
    fetchBehandling(tilknyttetBehandling)
  }, [])

  return (
    <>
      {mapAllApiResult(
        fetchBehandlingStatus,
        <Spinner visible={true} label="Henter førstegangsbehandling" />,
        null,
        () => (
          <ApiErrorAlert>Kunne ikke hente behandling</ApiErrorAlert>
        ),
        (detaljertbehandling) => (
          <>
            <Utenlandstilsnitt behandling={detaljertbehandling} redigerbar={false} />
            <BoddEllerArbeidetUtlandet behandling={detaljertbehandling} redigerbar={false} />
          </>
        )
      )}
    </>
  )
}
