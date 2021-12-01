import express from "express";
import { healthRouter } from "./routers/health";
import { modiaRouter } from "./routers/modia";

const app = express();

app.use(express.json());
app.use(function (req, res, next) {
    res.header("Access-Control-Allow-Origin", "http://localhost:3000"); //Todo: fikse domene
    res.header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE");
    res.header("Access-Control-Allow-Headers", "Content-Type");
    res.header("Access-Control-Allow-Credentials", "true");
    next();
});


app.use(healthRouter);
app.use(modiaRouter);

app.listen(4000, () => {
    console.log("Mock-server kjører på port 4000");
});
