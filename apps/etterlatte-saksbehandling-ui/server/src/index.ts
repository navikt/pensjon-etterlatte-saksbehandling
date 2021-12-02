import express from "express";
import path from "path";
import { authMiddleware } from "./middleware/auth";
import { healthRouter } from "./routers/health";
import { modiaRouter } from "./routers/modia";
import { proxy } from "./routers/proxy";

const app = express();

const clientPath = path.resolve(__dirname, "../client");

app.set("trust proxy", 1);
app.use("/", express.static(clientPath));

app.use(express.json());
app.use(function (req, res, next) {
    res.header("Access-Control-Allow-Origin", "http://localhost:3000"); //Todo: fikse domene
    res.header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE");
    res.header("Access-Control-Allow-Headers", "Content-Type");
    res.header("Access-Control-Allow-Credentials", "true");
    next();
});

app.use("/health", healthRouter);
app.use("/modiacontextholder/api/", modiaRouter);

app.use("/proxy", authMiddleware, proxy);

app.listen(8080, () => {
    console.log("Mock-server kjører på port 8080");
});
