import { Select, VStack, BodyShort } from '@navikt/ds-react'
import { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoerListe } from '~shared/api/etteroppgjoer'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { etteroppgjoerStatusTekst } from '~shared/types/EtteroppgjoerForbehandling'

type Props = {
  sakId: string
  value: string
  onChange: (value: string) => void
}

export const VelgEtteroppgjoersAar = ({ sakId, value, onChange }: Props) => {
  const [result, fetchEtteroppgjoer] = useApiCall(hentEtteroppgjoerListe)

  useEffect(() => {
    if (sakId) {
      fetchEtteroppgjoer(sakId)
    }
  }, [sakId])

  return mapResult(result, {
    pending: <Spinner label="Henter etteroppgjør..." />,
    error: (error) => <ApiErrorAlert>Kunne ikke hente etteroppgjør: {error.detail}</ApiErrorAlert>,
    success: (etteroppgjoerListe) => (
      <VStack gap="4">
        {etteroppgjoerListe.length === 0 ? (
          <BodyShort>Ingen etteroppgjør funnet.</BodyShort>
        ) : (
          <Select
            label="Velg hvilket etteroppgjør svaret tilhører"
            value={value}
            onChange={(e) => onChange(e.target.value)}
          >
            <option value="">Velg år</option>

            {etteroppgjoerListe.map((etteroppgjoer) => (
              <option key={etteroppgjoer.inntektsaar} value={etteroppgjoer.inntektsaar}>
                {etteroppgjoer.inntektsaar} – {etteroppgjoerStatusTekst[etteroppgjoer.status]}
              </option>
            ))}
          </Select>
        )}
      </VStack>
    ),
  })
}
