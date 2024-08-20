import React from 'react'
import { LandMedDokumenter } from '~shared/types/RevurderingInfo'
import { BodyShort, ErrorSummary, Heading } from '@navikt/ds-react'
import SEDLandMedDokumenter from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/SEDLandMedDokumenter'
import { ILand } from '~utils/kodeverk'

export const SendteSeder = ({
  landliste,
  feilkoder,
  setFeilkoder,
  setLandMedDokumenter,
  landMedDokumenter,
  redigerbar,
}: {
  landliste: ILand[]
  feilkoder: Set<string>
  setFeilkoder: React.Dispatch<React.SetStateAction<Set<string>>>
  landMedDokumenter: LandMedDokumenter[]
  setLandMedDokumenter: React.Dispatch<React.SetStateAction<LandMedDokumenter[]>>
  redigerbar: boolean
}) => {
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
        Sendte SED
      </Heading>
      <BodyShort>Fyll inn hvilke SED som er sendt i RINA til aktuelt land.</BodyShort>
      <SEDLandMedDokumenter
        landListe={landliste}
        landMedDokumenter={landMedDokumenter}
        setLandMedDokumenter={setLandMedDokumenter}
        resetFeilkoder={() => setFeilkoder(new Set([]))}
        redigerbar={redigerbar}
        label="Sendt dato"
      />
    </>
  )
}
