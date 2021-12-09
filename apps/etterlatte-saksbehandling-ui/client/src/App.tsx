import React, { useContext } from "react";
import { Grid, Cell } from "@navikt/ds-react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import "@navikt/ds-css";
import "./App.css";
import { AppContext, IAppContext } from "./store/AppContext";
import { Decorator } from "./components/decorator";
import { ws } from "./mocks/wsmock";
import Oppgavebenken from "./components/oppgavebenken/Oppgavebenken";
import { Container } from "./shared/styled";
import { Modal } from "./shared/modal/modal";
import { StatusBar } from "./components/statusbar";
import { Behandling } from "./components/behandling";
import { Link } from "react-router-dom";

ws();

function App() {
    const ctx = useContext<IAppContext>(AppContext);
    console.log(ctx);
    return (
        <div className="app">
            <Decorator />
            <BrowserRouter>
                <Routes>
                    <Route
                        path="/"
                        element={
                            // dras ut i egen component
                            <>
                                <Container>
                                    <Grid>
                                        <Cell className={"navds-story-cell"} xs={12} sm={6} lg={4}>
                                            <h1>De etterlatte</h1>
                                            <Link to="/behandling/personopplysninger">Gå til behandling</Link>
                                        </Cell>
                                    </Grid>
                                </Container>
                            </>
                        }
                    />
                    <Route path="/oppgavebenken" element={<Oppgavebenken />} />
                    <Route
                        path="/behandling/*"
                        element={
                            <>
                                <StatusBar />
                                <Behandling />
                            </>
                        }
                    />
                    <Route
                        path="/testside"
                        element={
                            <Container>
                                <Modal
                                    onClose={() => {
                                        console.log("Lukker modal");
                                    }}
                                >
                                    Test
                                </Modal>
                            </Container>
                        }
                    />
                </Routes>
            </BrowserRouter>
        </div>
    );
}

export default App;
