import { Alert, BodyShort, ErrorSummary, TextField } from '@navikt/ds-react'
import SEDLandMedDokumenter from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/SEDLandMedDokumenter'
import React from 'react'
import { LandMedDokumenter } from '~shared/types/RevurderingInfo'
import { ILand } from '~utils/kodeverk'

export const MottatteSeder = ({
  landliste,
  feilkoder,
  setFeilkoderMottatte,
  landMedDokumenterMottatte,
  setLandMedDokumenterMottatte,
  rinanummer,
  setRinanummer,
  redigerbar,
}: {
  landliste: ILand[]
  feilkoder: Set<string>
  setFeilkoderMottatte: React.Dispatch<React.SetStateAction<Set<string>>>
  landMedDokumenterMottatte: LandMedDokumenter[]
  setLandMedDokumenterMottatte: React.Dispatch<React.SetStateAction<LandMedDokumenter[]>>
  rinanummer: string
  setRinanummer: React.Dispatch<React.SetStateAction<string>>
  redigerbar: boolean
}) => {
  const resetFeilkoder = () => setFeilkoderMottatte(new Set([]))
  return (
    <>
      {!!feilkoder?.size && (
        <ErrorSummary
          style={{ marginTop: '10rem' }}
          heading="SED`ene er ufullstendig utfylt, vennligst rett opp så du kan gå videre i revurderingen."
        >
          {Array.from(feilkoder).map((feilmelding, i) => (
            <ErrorSummary.Item key={i}>{feilmelding}</ErrorSummary.Item>
          ))}
        </ErrorSummary>
      )}

      <BodyShort spacing>Fyll inn hvilke SED som er mottatt i RINA pr. land.</BodyShort>
      <Alert variant="info" style={{ width: '35rem' }}>
        Det kan hende det allerede ligger P5000/P4000 i avdødes eller i gjenlevendes/søskens sak. Sjekk dette før du
        etterspør informasjon.
      </Alert>
      <div style={{ width: '12rem', maxWidth: '20rem', margin: '2rem 0rem' }}>
        <TextField
          disabled={!redigerbar}
          label="Saksnummer RINA"
          value={rinanummer}
          onChange={(e) => {
            setRinanummer(e.target.value)
            resetFeilkoder()
          }}
        />
      </div>
      <SEDLandMedDokumenter
        landListe={landliste}
        landMedDokumenter={landMedDokumenterMottatte}
        setLandMedDokumenter={setLandMedDokumenterMottatte}
        resetFeilkoder={resetFeilkoder}
        redigerbar={redigerbar}
        label="Mottatt dato"
      />
    </>
  )
}
