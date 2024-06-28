import { formaterDato } from '~utils/formatering/dato'

export const Soeknadsdato = ({ mottattDato }: { mottattDato: string }) => (
  <div>
    <strong>Søknad mottatt: </strong>
    {formaterDato(mottattDato)}
  </div>
)
