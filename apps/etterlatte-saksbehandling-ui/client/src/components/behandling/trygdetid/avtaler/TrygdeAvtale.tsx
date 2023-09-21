import { HandshakeIcon } from '@navikt/aksel-icons'
import { Button, Heading, Select } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import { ApiErrorAlert } from '~ErrorBoundary'
import { FlexHeader, IconWrapper } from '~components/behandling/soeknadsoversikt/familieforhold/styled'
import Spinner from '~shared/Spinner'
import {
  hentAlleTrygdetidAvtaleKriterier,
  hentAlleTrygdetidAvtaler,
  hentTrygdeavtaleForBehandling,
  lagreTrygdeavtaleForBehandling,
  Trygdeavtale,
  TrygdeavtaleRequest,
  TrygdetidAvtale,
  TrygdetidAvtaleKriteria,
  TrygdetidAvtaleOptions,
} from '~shared/api/trygdetid'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { IconSize } from '~shared/types/Icon'
import { FormWrapper, Innhold } from '../styled'
import { TrygdeavtaleVisning } from './TrygdeavtaleVisning'
import { FlexRow } from '~shared/styled'

interface TrygdetidAvtaleOptionProps {
  defaultBeskrivelse: string
  trygdeavtaleOptions: TrygdetidAvtaleOptions[]
}

const TrygdetidAvtaleOptions = ({ defaultBeskrivelse, trygdeavtaleOptions }: TrygdetidAvtaleOptionProps) => {
  return (
    <>
      <option value="">{defaultBeskrivelse}</option>
      {trygdeavtaleOptions.map((trygdeavtaleOption) => (
        <option key={`${trygdeavtaleOption.kode}`} value={trygdeavtaleOption.kode}>
          {trygdeavtaleOption.beskrivelse}
        </option>
      ))}
    </>
  )
}

interface Props {
  redigerbar: boolean
}

export const TrygdeAvtale = ({ redigerbar }: Props) => {
  const { behandlingId } = useParams()
  const [hentAlleTrygdetidAvtalerRequest, fetchTrygdetidAvtaler] = useApiCall(hentAlleTrygdetidAvtaler)
  const [hentAlleTrygdetidAvtalerKriterierRequest, fetchTrygdetidAvtaleKriterier] = useApiCall(
    hentAlleTrygdetidAvtaleKriterier
  )
  const [lagreTrygdeavtaleRequest, lagreTrygdeavtale] = useApiCall(lagreTrygdeavtaleForBehandling)
  const [hentTrygdeavtaleRequest, fetchTrygdeavtale] = useApiCall(hentTrygdeavtaleForBehandling)
  const [avtalerListe, setAvtalerListe] = useState<TrygdetidAvtale[]>()
  const [avtaleKriterierListe, setAvtaleKriterierListe] = useState<TrygdetidAvtaleKriteria[]>()
  const [trygdeavtale, setTrygdeavtale] = useState<Trygdeavtale>({} as Trygdeavtale)
  const [valgtAvtale, setValgtAvtale] = useState<TrygdetidAvtale>()
  const [redigering, setRedigering] = useState<Boolean>(true)
  const [harLagretVerdi, setHarLagretVerdi] = useState<Boolean>(false)

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
          setHarLagretVerdi(true)
        }
      })
    }
  }, [])

  const avbryt = () => {
    if (redigering) {
      setRedigering(false)
    }
  }

  const rediger = () => {
    if (!redigering) {
      velgAvtale(trygdeavtale?.avtaleKode)
      setRedigering(true)
    }
  }

  const lagre = () => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    lagreTrygdeavtale(
      {
        behandlingsId: behandlingId,
        avtaleRequest: {
          avtaleKode: trygdeavtale.avtaleKode,
          avtaleDatoKode: trygdeavtale.avtaleDatoKode,
          avtaleKriteriaKode: trygdeavtale.avtaleKriteriaKode,
          id: trygdeavtale.id,
        } as TrygdeavtaleRequest,
      },
      (respons) => {
        setTrygdeavtale(respons)
        setRedigering(false)
      }
    )
  }

  const velgAvtale = (kode?: string) => {
    if (kode && avtalerListe) {
      setValgtAvtale(avtalerListe.find((avtale) => avtale.kode === kode))
    } else {
      setValgtAvtale(undefined)
    }
  }

  const trygdeavtaleOptionSort = (a: TrygdetidAvtaleOptions, b: TrygdetidAvtaleOptions) => {
    if (a.beskrivelse > b.beskrivelse) {
      return 1
    }
    return -1
  }

  return (
    <TrygdeAvtaleWrapper>
      <FlexHeader>
        <IconWrapper>
          <HandshakeIcon fontSize={IconSize.DEFAULT} />
        </IconWrapper>
        <Heading size="small" level="3">
          Vurdering av trygdeavtale
        </Heading>
      </FlexHeader>

      {!redigering && avtalerListe && avtaleKriterierListe && (
        <>
          <TrygdeavtaleVisning avtaler={avtalerListe} kriterier={avtaleKriterierListe} trygdeavtale={trygdeavtale} />
          {redigerbar && (
            <Button size="small" onClick={rediger} type="button">
              Rediger
            </Button>
          )}
        </>
      )}

      {redigerbar && redigering && avtalerListe && avtaleKriterierListe && (
        <>
          <Innhold>
            <TrygdeAvtaleForm>
              <Rows>
                <FormWrapper>
                  <Select
                    label="Avtale"
                    autoComplete="off"
                    value={trygdeavtale.avtaleKode}
                    onChange={(e) => {
                      if (e.target.value) {
                        setTrygdeavtale({ ...trygdeavtale, avtaleKode: e.target.value })
                      }
                      velgAvtale(e.target.value)
                    }}
                  >
                    <TrygdetidAvtaleOptions defaultBeskrivelse="Velg avtale" trygdeavtaleOptions={avtalerListe} />
                  </Select>
                  {valgtAvtale && valgtAvtale.datoer.length > 0 && (
                    <Select
                      label="Dato"
                      autoComplete="off"
                      value={trygdeavtale.avtaleDatoKode}
                      onChange={(e) => {
                        setTrygdeavtale({ ...trygdeavtale, avtaleDatoKode: e.target.value })
                      }}
                    >
                      <TrygdetidAvtaleOptions
                        defaultBeskrivelse="Velg avtaledato"
                        trygdeavtaleOptions={valgtAvtale.datoer}
                      />
                    </Select>
                  )}
                </FormWrapper>
              </Rows>
              <Rows>
                <FormWrapper>
                  <Select
                    label="Kriterier for å omfattes av avtalen"
                    autoComplete="off"
                    value={trygdeavtale.avtaleKriteriaKode}
                    onChange={(e) => {
                      setTrygdeavtale({ ...trygdeavtale, avtaleKriteriaKode: e.target.value })
                    }}
                  >
                    <TrygdetidAvtaleOptions
                      defaultBeskrivelse="Velg kriteria"
                      trygdeavtaleOptions={avtaleKriterierListe}
                    />
                  </Select>
                </FormWrapper>
              </Rows>
              <Rows>
                <FlexRow $spacing>
                  <Button size="small" loading={isPending(lagreTrygdeavtaleRequest)} type="button" onClick={lagre}>
                    Lagre
                  </Button>
                  {harLagretVerdi && (
                    <Button size="small" onClick={avbryt} type="button">
                      Avbryt
                    </Button>
                  )}
                </FlexRow>
              </Rows>
            </TrygdeAvtaleForm>
          </Innhold>
        </>
      )}
      {(isPending(hentAlleTrygdetidAvtalerRequest) ||
        isPending(hentAlleTrygdetidAvtalerKriterierRequest) ||
        isPending(hentTrygdeavtaleRequest)) && <Spinner visible={true} label="Henter trgydeavtaler" />}
      {isFailure(hentAlleTrygdetidAvtalerRequest) && (
        <ApiErrorAlert>En feil har oppstått ved henting av trygdeavtaler</ApiErrorAlert>
      )}
      {isFailure(hentAlleTrygdetidAvtalerKriterierRequest) && (
        <ApiErrorAlert>En feil har oppstått ved henting av trygdeavtalekriterier</ApiErrorAlert>
      )}
      {isFailure(hentTrygdeavtaleRequest) && (
        <ApiErrorAlert>En feil har oppstått ved henting av trygdeavtale for behandlingen</ApiErrorAlert>
      )}
      {isFailure(lagreTrygdeavtaleRequest) && (
        <ApiErrorAlert>En feil har oppstått ved lagring av trygdeavtale for behandlingen</ApiErrorAlert>
      )}
    </TrygdeAvtaleWrapper>
  )
}

const TrygdeAvtaleWrapper = styled.div`
  margin-top: 2em;
`

const TrygdeAvtaleForm = styled.form`
  display: flex;
  flex-direction: column;
  margin-bottom: 2em;
`

const Rows = styled.div`
  display: flex;
  margin-bottom: 1em;
`
