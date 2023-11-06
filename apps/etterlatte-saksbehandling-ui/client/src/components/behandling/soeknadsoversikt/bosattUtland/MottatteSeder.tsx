import { BodyShort, ErrorSummary, Heading } from '@navikt/ds-react'
import { isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import SEDLandMedDokumenter from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/SEDLandMedDokumenter'
import { hentAlleLand, ILand, sorterLand } from '~shared/api/trygdetid'
import { useEffect, useState } from 'react'
import { LandMedDokumenter } from '~shared/types/RevurderingInfo'
import { InformationSquareIcon } from '@navikt/aksel-icons'
import { ABlue500 } from '@navikt/ds-tokens/dist/tokens'
import Spinner from '~shared/Spinner'

export const MottatteSeder = () => {
  const [hentAlleLandRequest, fetchAlleLand] = useApiCall(hentAlleLand)
  const [alleLandKodeverk, setAlleLandKodeverk] = useState<ILand[] | null>(null)

  const initalStateLandMedDokumenter = [
    {
      landIsoKode: undefined,
      dokumenter: [
        { dokumenttype: 'P5000', dato: undefined, kommentar: '' },
        { dokumenttype: 'P6000', dato: undefined, kommentar: '' },
        { dokumenttype: 'P2100', dato: undefined, kommentar: '' },
      ],
    },
  ]

  const [landMedDokumenter, setLandMedDokumenter] = useState<LandMedDokumenter[]>(initalStateLandMedDokumenter)
  const [feilkoder, setFeilkoder] = useState<Set<string>>(new Set([]))

  useEffect(() => {
    fetchAlleLand(null, (landliste) => {
      setAlleLandKodeverk(sorterLand(landliste))
    })
  }, [])

  //TODO: onsave
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
        Mottatte SED
      </Heading>
      <BodyShort>Fyll inn hvilke SED som er mottatt i RINA pr land.</BodyShort>
      <BodyShort>
        <InformationSquareIcon stroke={ABlue500} fill={ABlue500} fontSize="1.2rem" />
        Det kan hende det allerede ligger P5000/P6000 i avdødes sak. Sjekk opp i dette før du etterspør info.
      </BodyShort>
      {isPending(hentAlleLandRequest) && <Spinner visible={true} label="Henter land" />}
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
