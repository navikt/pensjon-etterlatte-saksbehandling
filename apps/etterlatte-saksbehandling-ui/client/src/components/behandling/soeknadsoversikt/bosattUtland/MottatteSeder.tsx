import { BodyShort, ErrorSummary, Heading, TextField } from '@navikt/ds-react'
import SEDLandMedDokumenter from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/SEDLandMedDokumenter'
import { ILand } from '~shared/api/trygdetid'
import React from 'react'
import { LandMedDokumenter } from '~shared/types/RevurderingInfo'
import { InformationSquareIcon } from '@navikt/aksel-icons'
import { ABlue500 } from '@navikt/ds-tokens/dist/tokens'

export const MottatteSeder = ({
  landliste,
  feilkoder,
  setFeilkoderMottatte,
  landMedDokumenterMottatte,
  setLandMedDokumenterMottatte,
  rinanummer,
  setRinanummer,
}: {
  landliste: ILand[]
  feilkoder: Set<string>
  setFeilkoderMottatte: React.Dispatch<React.SetStateAction<Set<string>>>
  landMedDokumenterMottatte: LandMedDokumenter[]
  setLandMedDokumenterMottatte: React.Dispatch<React.SetStateAction<LandMedDokumenter[]>>
  rinanummer: string
  setRinanummer: React.Dispatch<React.SetStateAction<string>>
}) => {
  const resetFeilkoder = () => setFeilkoderMottatte(new Set([]))
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
        Mottatt krav fra utland
      </Heading>
      <BodyShort>Fyll inn hvilke SED som er mottatt i RINA pr land.</BodyShort>
      <BodyShort>
        <InformationSquareIcon stroke={ABlue500} fill={ABlue500} fontSize="1.2rem" />
        Det kan hende det allerede ligger P5000/P6000 i avdødes sak. Sjekk opp i dette før du etterspør info.
      </BodyShort>
      <div style={{ width: '12rem', maxWidth: '20rem', margin: '2rem 0rem' }}>
        <TextField
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
      />
    </>
  )
}
