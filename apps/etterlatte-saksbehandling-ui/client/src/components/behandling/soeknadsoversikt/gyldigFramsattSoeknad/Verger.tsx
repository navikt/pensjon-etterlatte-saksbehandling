import { Info } from '../Info'
import { useApiCall } from '~shared/hooks/useApiCall'
import { getGrunnlagsAvOpplysningstype } from '~shared/api/grunnlag'
import { KildePdl } from '~shared/types/kilde'
import { IPdlPerson } from '~shared/types/Person'
import { Grunnlagsopplysning } from '~shared/types/grunnlag'
import { useEffect } from 'react'
import { formaterKildePdl } from '~components/behandling/soeknadsoversikt/utils'
import { mapResult } from '~shared/api/apiUtils'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { BodyShort, VStack } from '@navikt/ds-react'
import { ApiErrorAlert } from '~ErrorBoundary'
import { formaterDato } from '~utils/formatering/dato'

interface Props {
  behandlingId: string
  sakId: number
}

export const Verger = ({ sakId, behandlingId }: Props) => {
  const [soeker, getSoekerFraGrunnlag] = useApiCall(getGrunnlagsAvOpplysningstype)
  useEffect(() => {
    getSoekerFraGrunnlag({
      sakId: sakId,
      behandlingId: behandlingId,
      opplysningstype: 'SOEKER_PDL_V1',
    })
  }, [sakId, behandlingId])

  function successContents(soekerOpplysning: Grunnlagsopplysning<IPdlPerson, KildePdl>) {
    const vergeList = soekerOpplysning.opplysning.vergemaalEllerFremtidsfullmakt || []
    if (!vergeList?.length) {
      return (
        <Info label="Vergemål" tekst="Ingen verge registrert" undertekst={formaterKildePdl(soekerOpplysning.kilde)} />
      )
    }

    const omfangMap = new Map([
      ['personligeOgOekonomiskeInteresser', 'Personlige og økonomiske interesser'],
      ['utlendingssakerPersonligeOgOekonomiskeInteresser', 'Personlige og økonomiske interesser (utlendingssaker)'],
      ['oekonomiskeInteresser', 'Økonomiske interesser'],
      ['personligeInteresser', 'Personlige interesser'],
    ])

    return (
      <>
        {vergeList.map((it, index) => (
          <Info
            label="Verge"
            tekst={
              <VStack>
                {it.vergeEllerFullmektig.motpartsPersonident ? (
                  <div>
                    <KopierbarVerdi value={it.vergeEllerFullmektig.motpartsPersonident!} />
                  </div>
                ) : (
                  'Fødselsnummer ikke registrert'
                )}
                {it.vergeEllerFullmektig.omfang && (
                  <BodyShort>
                    {omfangMap.get(it.vergeEllerFullmektig.omfang) ?? it.vergeEllerFullmektig.omfang}
                  </BodyShort>
                )}
                {!!it.opphoerstidspunkt && <BodyShort>Opphører: {formaterDato(it.opphoerstidspunkt)}</BodyShort>}
              </VStack>
            }
            undertekst={formaterKildePdl(soekerOpplysning.kilde)}
            key={index}
          />
        ))}
      </>
    )
  }

  return (
    <VStack gap="space-4">
      {mapResult(soeker, {
        error: (error) => <ApiErrorAlert>Kunne ikke hente info om verge(r): {error.detail}</ApiErrorAlert>,
        success: (data) => successContents(data),
      })}
    </VStack>
  )
}
