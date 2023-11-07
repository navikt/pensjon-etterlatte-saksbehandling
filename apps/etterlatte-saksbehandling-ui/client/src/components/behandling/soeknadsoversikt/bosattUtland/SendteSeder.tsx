import { ILand } from '~shared/api/trygdetid'
import { useState } from 'react'
import { LandMedDokumenter } from '~shared/types/RevurderingInfo'
import { BodyShort, ErrorSummary, Heading } from '@navikt/ds-react'
import SEDLandMedDokumenter from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/SEDLandMedDokumenter'

export const SendteSeder = ({ landliste }: { landliste: ILand[] }) => {
  const initalStateLandMedDokumenter = [
    {
      landIsoKode: undefined,
      dokumenter: [{ dokumenttype: 'P8000', dato: undefined, kommentar: '' }],
    },
  ]

  const [landMedDokumenter, setLandMedDokumenter] = useState<LandMedDokumenter[]>(initalStateLandMedDokumenter)
  const [feilkoder, setFeilkoder] = useState<Set<string>>(new Set([]))

  const validerSkjema = () => {
    const feilkoder: Set<string> = new Set([])
    if (landMedDokumenter.find((landmedDokument) => !landmedDokument.landIsoKode)) {
      feilkoder.add('Du må velge et land for hver SED`er(land rad i tabellen under)')
    }
    if (landMedDokumenter.find((landMedDokument) => landMedDokument.dokumenter.length === 0)) {
      feilkoder.add('Du må legge til minst et dokument per land rad, eller slette landraden.')
    }
    landMedDokumenter.forEach((landMedDokument) => {
      if (landMedDokument.dokumenter.find((e) => !e.dokumenttype)) {
        feilkoder.add('Du må skrive inn en dokumenttype(P2000 feks) eller fjerne dokumentraden.')
      }
      if (landMedDokument.dokumenter.find((e) => !e.dato)) {
        feilkoder.add('Du må legge til dato for hvert dokument')
      }
    })
    setFeilkoder(feilkoder)
    return feilkoder
  }

  const lagre = () => {
    //TODO: koble mot backend
    validerSkjema()
  }

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
      <Heading onClick={() => lagre()} level="2" size="medium" style={{ marginTop: '2rem' }}>
        Sendte SED
      </Heading>
      <BodyShort>Fyll inn hvilke SED som er mottatt i RINA pr land.</BodyShort>
      <SEDLandMedDokumenter
        landListe={landliste}
        landMedDokumenter={landMedDokumenter}
        setLandMedDokumenter={setLandMedDokumenter}
        resetFeilkoder={() => setFeilkoder(new Set([]))}
      />
    </>
  )
}
