import React from "react";
import OppgaveHeader from "./OppgaveHeader";
import OppgaveListe from "./OppgaveListe";

import styled from 'styled-components';

const OppgavebenkContainer = styled.div`
  max-width: 60em;
  padding: 2rem;
`;

const Oppgavebenken = () => {

    return (
        <OppgavebenkContainer>
            <OppgaveHeader/>
            <OppgaveListe />
        </OppgavebenkContainer>
    )
}

export default Oppgavebenken;