import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import {
    connectToGame,
    sendAnswer,
    sendStartGame,
    type GameAnswerResult,
    type GameQuestion,
    type GameReveal,
    type GameScores,
} from "../services/gameSocket";
import type { Client } from "@stomp/stompjs";

import GameTimer from "../components/game/GameTimer";
import AnswerButton from "../components/game/AnswerButton";
import Leaderboard from "../components/game/Leaderboard";
import AnsweredIndicators from "../components/game/AnsweredIndicators";
import ResultsPodium from "../components/game/ResultsPodium";

type Phase =
    | "waiting"
    | "countdown"
    | "playing"
    | "answered"
    | "reveal"
    | "finished";

const TIMER_SECONDS = 15;

function shuffle<T>(arr: T[]): T[] {
    const a = [...arr];

    for (let i = a.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [a[i], a[j]] = [a[j], a[i]];
    }

    return a;
}

function playTone(
    freq: number,
    dur: number,
    type: OscillatorType = "sine",
    vol = 0.12
) {
    try {
        const ctx = new AudioContext();
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();

        osc.connect(gain);
        gain.connect(ctx.destination);

        osc.type = type;
        osc.frequency.setValueAtTime(freq, ctx.currentTime);

        gain.gain.setValueAtTime(vol, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(
            0.0001,
            ctx.currentTime + dur
        );

        osc.start();
        osc.stop(ctx.currentTime + dur);
    } catch {}
}

const playCorrect = () => {
    playTone(523, 0.1, "sine", 0.18);
    setTimeout(() => playTone(784, 0.2, "sine", 0.18), 120);
};

const playWrong = () => playTone(200, 0.35, "sawtooth", 0.1);

const playTick = () => playTone(880, 0.05, "square", 0.04);

const playGo = () => {
    playTone(660, 0.12, "sine", 0.18);
    setTimeout(() => playTone(880, 0.25, "sine", 0.18), 140);
};

function spawnConfetti(container: HTMLElement) {
    const colors = [
        "#6BAB90",
        "#E1F0C4",
        "#FFE2D1",
        "#a78bfa",
        "#fb923c",
        "#38bdf8",
    ];

    for (let i = 0; i < 32; i++) {
        const el = document.createElement("div");

        el.className = "confetti-piece";

        el.style.cssText = `
            left:${Math.random() * 100}%;
            background:${colors[Math.floor(Math.random() * colors.length)]};
            width:${6 + Math.random() * 8}px;
            height:${6 + Math.random() * 8}px;
            border-radius:${Math.random() > 0.5 ? "50%" : "2px"};
            animation-delay:${Math.random() * 0.4}s;
            animation-duration:${0.8 + Math.random() * 0.6}s;
        `;

        container.appendChild(el);

        el.addEventListener("animationend", () => el.remove());
    }
}

export default function Game() {
    const { code } = useParams<{ code: string }>();
    const { currentUser } = useAuth();
    const navigate = useNavigate();

    const clientRef = useRef<Client | null>(null);
    const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
    const confettiRef = useRef<HTMLDivElement>(null);
    const submitting = useRef(false);

    const [connected, setConnected] = useState(false);
    const [phase, setPhase] = useState<Phase>("waiting");

    const [countdown, setCountdown] = useState<number | null>(null);

    const [question, setQuestion] =
        useState<GameQuestion | null>(null);

    const [scores, setScores] =
        useState<GameScores | null>(null);

    const [lastResult, setLastResult] =
        useState<GameAnswerResult | null>(null);

    const [selectedId, setSelectedId] =
        useState<number | null>(null);

    const [revealId, setRevealId] =
        useState<number | null>(null);

    const [finalScores, setFinalScores] =
        useState<GameScores | null>(null);

    const [timeLeft, setTimeLeft] =
        useState(TIMER_SECONDS);

    const [pointsPopup, setPointsPopup] =
        useState<{ pts: number; key: number } | null>(null);

    // Réponse saisie par le joueur pour SAISIE_LIBRE
    const [freeAnswer, setFreeAnswer] = useState("");

    const clearTimer = useCallback(() => {
        if (timerRef.current) {
            clearInterval(timerRef.current);
            timerRef.current = null;
        }
    }, []);

    const startTimer = useCallback(
        (serverTs?: number) => {
            clearTimer();

            const elapsed = serverTs
                ? Math.max(0, (Date.now() - serverTs) / 1000)
                : 0;

            const initial = Math.max(
                0,
                Math.round(TIMER_SECONDS - elapsed)
            );

            setTimeLeft(initial);

            timerRef.current = setInterval(() => {
                setTimeLeft((prev) => {
                    if (prev <= 1) {
                        clearTimer();
                        return 0;
                    }

                    if (prev <= 6) {
                        playTick();
                    }

                    return prev - 1;
                });
            }, 1000);
        },
        [clearTimer]
    );

    useEffect(() => {
        if (!code || !currentUser) return;

        const client = connectToGame(
            code,
            currentUser.id,
            {
                onConnected: () => {
                    setConnected(true);
                },

                onCountdown: (c) => {
                    setCountdown(c.value);
                    setPhase("countdown");
                },

                onQuestion: (q) => {
                    setQuestion({
                        ...q,
                        reponses: shuffle(q.reponses),
                    });

                    setPhase("playing");
                    setLastResult(null);
                    setSelectedId(null);
                    setRevealId(null);
                    setCountdown(null);

                    // Nouvelle question = nouvelle réponse libre vide
                    setFreeAnswer("");

                    submitting.current = false;

                    startTimer(q.serverTimestamp);

                    playGo();
                },

                onScores: (s) => {
                    setScores(s);
                },

                onAnswerResult: (r) => {
                    setLastResult(r);
                    setPhase("answered");

                    if (r.correct) {
                        playCorrect();

                        if (confettiRef.current) {
                            spawnConfetti(confettiRef.current);
                        }

                        setPointsPopup({
                            pts: r.pointsEarned,
                            key: Date.now(),
                        });
                    } else {
                        playWrong();
                    }
                },

                onReveal: (r: GameReveal) => {
                    clearTimer();
                    setRevealId(r.correctReponseId);
                    setPhase("reveal");
                },

                onResults: (r) => {
                    clearTimer();
                    setFinalScores(r);
                    setPhase("finished");
                },
            }
        );

        clientRef.current = client;

        return () => {
            client.deactivate();
            clearTimer();
        };
    }, [
        code,
        currentUser,
        startTimer,
        clearTimer,
    ]);

    /*
     * Envoi d'une réponse.
     *
     * CHOIX :
     *   reponseId = ID de la réponse
     *   answer = ""
     *
     * SAISIE_LIBRE :
     *   reponseId = null
     *   answer = texte saisi
     */
    const handleAnswer = (
        reponseId: number | null = null,
        answerText = ""
    ) => {
        if (
            !clientRef.current ||
            !code ||
            !question ||
            phase !== "playing" ||
            timeLeft === 0 ||
            submitting.current
        ) {
            return;
        }

        // Question à saisie libre
        if (question.saisieLibre) {
            const value = answerText.trim();

            if (!value) {
                return;
            }

            submitting.current = true;

            setFreeAnswer(value);

            sendAnswer(
                clientRef.current,
                code,
                null,
                value
            );

            return;
        }

        // Question à choix
        if (reponseId === null) {
            return;
        }

        submitting.current = true;

        setSelectedId(reponseId);

        sendAnswer(
            clientRef.current,
            code,
            reponseId,
            ""
        );
    };

    const handleFreeAnswerSubmit = () => {
        handleAnswer(null, freeAnswer);
    };

    if (!code) {
        return <p className="error">Code manquant.</p>;
    }

    // ----------------------------------------------------------------
    // FIN DE PARTIE
    // ----------------------------------------------------------------

    if (phase === "finished" && finalScores) {
        const myName = currentUser?.username ?? "";

        const myRank =
            Object.entries(finalScores.scores)
                .sort(([, a], [, b]) => b - a)
                .findIndex(([u]) => u === myName) + 1;

        return (
            <div className="results-page">
                <div className="results-header">
                    <h1>Résultats</h1>

                    {myRank > 0 && (
                        <p className="my-rank">
                            Tu termines{" "}
                            <strong>
                                {myRank === 1
                                    ? "1er"
                                    : myRank === 2
                                        ? "2e"
                                        : myRank === 3
                                            ? "3e"
                                            : `${myRank}e`}
                            </strong>

                            {myRank === 1 &&
                                " — Félicitations !"}
                        </p>
                    )}
                </div>

                <ResultsPodium
                    finalScores={finalScores}
                    myName={myName}
                />

                <button
                    onClick={() => navigate("/dashboard")}
                    style={{
                        marginTop: 24,
                        width: "100%",
                    }}
                >
                    Retour au dashboard
                </button>
            </div>
        );
    }

    const myName = currentUser?.username ?? "";

    // ----------------------------------------------------------------
    // PAGE DE JEU
    // ----------------------------------------------------------------

    return (
        <div className="game-page">

            <div
                className="confetti-container"
                ref={confettiRef}
            />

            {pointsPopup && (
                <div
                    className="points-popup"
                    key={pointsPopup.key}
                >
                    +{pointsPopup.pts} pts
                </div>
            )}

            <div className="game-header">
                <h1>Quiz Arena</h1>

                <p>
                    Salon : <strong>{code}</strong> —{" "}
                    {connected
                        ? "Connecté"
                        : "Connexion..."}
                </p>
            </div>

            {/* --------------------------------------------------------
                ATTENTE
            -------------------------------------------------------- */}

            {phase === "waiting" && (
                <div style={{ textAlign: "center" }}>
                    <p>En attente du lancement...</p>

                    <button
                        onClick={() =>
                            clientRef.current &&
                            code &&
                            sendStartGame(
                                clientRef.current,
                                code
                            )
                        }
                        disabled={!connected}
                    >
                        Démarrer le quiz
                    </button>
                </div>
            )}

            {/* --------------------------------------------------------
                COUNTDOWN
            -------------------------------------------------------- */}

            {phase === "countdown" && (
                <div className="game-countdown">
                    <p className="countdown-label">
                        Préparez-vous !
                    </p>

                    <div
                        className="countdown-number"
                        key={countdown}
                    >
                        {countdown}
                    </div>
                </div>
            )}

            {/* --------------------------------------------------------
                QUESTION
            -------------------------------------------------------- */}

            {(phase === "playing" ||
                    phase === "answered" ||
                    phase === "reveal") &&
                question && (
                    <div
                        className="question-transition"
                        key={`${question.questionId}-${phase}`}
                    >
                        <div className="question-meta">
                            <span className="game-progress">
                                Q{question.questionNumber} /{" "}
                                {question.totalQuestions}
                            </span>
                        </div>

                        {phase !== "reveal" && (
                            <GameTimer
                                timeLeft={timeLeft}
                            />
                        )}

                        {phase === "reveal" && (
                            <div className="reveal-banner">
                                Question suivante...
                            </div>
                        )}

                        <p className="game-question">
                            {question.text}
                        </p>

                        {lastResult &&
                            lastResult.streak >= 2 &&
                            phase === "answered" && (
                                <div className="streak-badge">
                                    Série de{" "}
                                    {lastResult.streak} !{" "}
                                    {lastResult.multiplier > 1
                                        ? `×${lastResult.multiplier}`
                                        : ""}
                                </div>
                            )}

                        {/* =================================================
                            QUESTION À CHOIX
                        ================================================= */}

                        {!question.saisieLibre && (
                            <div className="game-answers">
                                {question.reponses.map(
                                    (r) => (
                                        <AnswerButton
                                            key={r.id}
                                            reponse={r}
                                            phase={phase}
                                            selectedId={
                                                selectedId
                                            }
                                            lastResult={
                                                lastResult
                                            }
                                            revealId={
                                                revealId
                                            }
                                            disabled={
                                                phase !==
                                                "playing" ||
                                                timeLeft === 0
                                            }
                                            onClick={() =>
                                                handleAnswer(
                                                    r.id
                                                )
                                            }
                                        />
                                    )
                                )}
                            </div>
                        )}

                        {/* =================================================
                            QUESTION À SAISIE LIBRE
                        ================================================= */}

                        {question.saisieLibre && (
                            <div
                                className="free-answer-container"
                                style={{
                                    display: "flex",
                                    flexDirection: "column",
                                    gap: 12,
                                    maxWidth: 600,
                                    margin: "24px auto",
                                }}
                            >
                                <input
                                    type="text"
                                    value={freeAnswer}
                                    onChange={(e) =>
                                        setFreeAnswer(
                                            e.target.value
                                        )
                                    }
                                    onKeyDown={(e) => {
                                        if (
                                            e.key ===
                                            "Enter"
                                        ) {
                                            handleFreeAnswerSubmit();
                                        }
                                    }}
                                    placeholder="Écris ta réponse..."
                                    disabled={
                                        phase !==
                                        "playing" ||
                                        timeLeft === 0
                                    }
                                    autoComplete="off"
                                    style={{
                                        width: "100%",
                                        padding: "16px",
                                        fontSize: "18px",
                                        borderRadius: "8px",
                                        border: "1px solid #ccc",
                                        boxSizing:
                                            "border-box",
                                    }}
                                />

                                <button
                                    type="button"
                                    onClick={
                                        handleFreeAnswerSubmit
                                    }
                                    disabled={
                                        phase !==
                                        "playing" ||
                                        timeLeft === 0 ||
                                        !freeAnswer.trim()
                                    }
                                    style={{
                                        padding: "14px 20px",
                                        fontSize: "16px",
                                        cursor:
                                            phase ===
                                            "playing" &&
                                            timeLeft > 0 &&
                                            freeAnswer.trim()
                                                ? "pointer"
                                                : "default",
                                    }}
                                >
                                    Valider ma réponse
                                </button>
                            </div>
                        )}

                        {/* -------------------------------------------------
                            FEEDBACK
                        ------------------------------------------------- */}

                        {phase === "answered" &&
                            lastResult && (
                                <div
                                    className={`game-feedback ${
                                        lastResult.correct
                                            ? "ok"
                                            : "ko"
                                    }`}
                                >
                                    {lastResult.correct
                                        ? "Bonne réponse !"
                                        : "Mauvaise réponse"}

                                    {" · "}

                                    <strong>
                                        +
                                        {
                                            lastResult.pointsEarned
                                        }{" "}
                                        pts
                                    </strong>

                                    {" · "}Total :{" "}
                                    {lastResult.score} pts
                                </div>
                            )}

                        {phase === "playing" &&
                            timeLeft === 0 &&
                            !lastResult && (
                                <div className="game-feedback ko">
                                    Temps écoulé !
                                </div>
                            )}

                        {scores && (
                            <AnsweredIndicators
                                scores={scores}
                                myName={myName}
                            />
                        )}
                    </div>
                )}

            {/* --------------------------------------------------------
                CLASSEMENT
            -------------------------------------------------------- */}

            {scores &&
                phase !== "waiting" &&
                phase !== "countdown" &&
                phase !== "finished" && (
                    <Leaderboard
                        scores={scores}
                        myName={myName}
                    />
                )}
        </div>
    );
}