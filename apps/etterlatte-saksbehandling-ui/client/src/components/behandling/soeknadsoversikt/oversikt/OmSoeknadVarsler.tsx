import { AlertVarsel } from './AlertVarsel'

interface Props {
  feilForelderOppgittSomAvdoed: boolean
  forelderIkkeDoed: boolean
  dodsfallMerEnn3AarSiden: boolean
}
export const OmSoeknadVarsler: React.FC<Props> = ({
  feilForelderOppgittSomAvdoed,
  forelderIkkeDoed,
  dodsfallMerEnn3AarSiden,
}) => {
  return (
    <>
      {feilForelderOppgittSomAvdoed && <AlertVarsel varselType="ikke riktig oppgitt avdød i søknad" />}
      {forelderIkkeDoed && <AlertVarsel varselType="forelder ikke død" />}

      {dodsfallMerEnn3AarSiden && <AlertVarsel varselType="dødsfall 3 år" />}
    </>
  )
}
