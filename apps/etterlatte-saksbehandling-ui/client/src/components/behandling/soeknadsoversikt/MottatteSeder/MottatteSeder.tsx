import { BodyShort, ErrorSummary, Heading } from '@navikt/ds-react'
import { isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import SEDLandMedDokumenter from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/SEDLandMedDokumenter'
import { hentAlleLand, ILand, sorterLand } from '~shared/api/trygdetid'
import { useEffect, useState } from 'react'
import { LandMedDokumenter } from '~shared/types/RevurderingInfo'

export const MottatteSeder = () => {
  const [hentAlleLandRequest, fetchAlleLand] = useApiCall(hentAlleLand)
  const [alleLandKodeverk, setAlleLandKodeverk] = useState<ILand[] | null>(null)

  const initalStateLandMedDokumenter = [
    { landIsoKode: undefined, dokumenter: [{ dokumenttype: '', dato: undefined, kommentar: '' }] },
  ]

  const [landMedDokumenter, setLandMedDokumenter] = useState<LandMedDokumenter[]>(initalStateLandMedDokumenter)

  const [feilkoder, setFeilkoder] = useState<Set<string>>(new Set([]))

  useEffect(() => {
    fetchAlleLand(null, (landliste) => {
      setAlleLandKodeverk(sorterLand(landliste))
    })
  }, [])

  return (
    <>
      {!!feilkoder?.size ? (
        <ErrorSummary
          style={{ marginTop: '10rem' }}
          heading="SED`ene er ufullstendig utfylt, vennligst rett opp så du kan gå videre i revurderingen."
        >
          {Array.from(feilkoder).map((feilmelding, i) => (
            <ErrorSummary.Item key={i}>{feilmelding}</ErrorSummary.Item>
          ))}
        </ErrorSummary>
      ) : null}
      <Heading level="2" size="medium" style={{ marginTop: '2rem' }}>
        Mottatte SED
      </Heading>
      <BodyShort>Fyll inn hvilke SED som er mottatt i RINA pr land.</BodyShort>
      {isSuccess(hentAlleLandRequest) && alleLandKodeverk && (
        <SEDLandMedDokumenter
          landListe={alleLandKodeverk}
          landMedDokumenter={landMedDokumenter}
          setLandMedDokumenter={setLandMedDokumenter}
          resetFeilkoder={() => setFeilkoder(new Set([]))}
        />
      )}
    </>
  )
}
