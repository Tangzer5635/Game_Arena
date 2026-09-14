import {
    useCallback,
    useEffect,
    useRef,
    useState,
} from "react";

import { useNavigate, useParams } from "react-router-dom";

import { useAuth } from "../context/AuthContext";

import { getSalon } from "../services/salonService";

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

type Phase =
    | "waiting"
    | "countdown"
    | "playing"
    | "answered"
    | "reveal"
    | "finished";

const TIMER_SECONDS = 15;

function Game() {

    const { code } = useParams<{
        code: string;
    }>();

    const { currentUser } = useAuth();

    const navigate = useNavigate();

    const clientRef =
        useRef<Client | null>(null);

    const timerRef =
        useRef<ReturnType<typeof setInterval> | null>(null);

    const startedRef =
        useRef(false);

    const [
        connected,
        setConnected,
    ] = useState(false);

    const [
        isCreator,
        setIsCreator,
    ] = useState(false);

    const [
        phase,
        setPhase,
    ] = useState<Phase>("waiting");

    const [
        countdown,
        setCountdown,
    ] = useState<number | null>(null);

    const [
        question,
        setQuestion,
    ] = useState<GameQuestion | null>(null);

    const [
        scores,
        setScores,
    ] = useState<GameScores | null>(null);

    const [
        lastResult,
        setLastResult,
    ] = useState<GameAnswerResult | null>(null);

    const [
        selectedId,
        setSelectedId,
    ] = useState<number | null>(null);

    const [
        freeAnswer,
        setFreeAnswer,
    ] = useState("");

    const [
        revealId,
        setRevealId,
    ] = useState<number | null>(null);

    const [
        revealAnswer,
        setRevealAnswer,
    ] = useState<string | null>(null);

    const [
        finalScores,
        setFinalScores,
    ] = useState<GameScores | null>(null);

    const [
        timeLeft,
        setTimeLeft,
    ] = useState(TIMER_SECONDS);

    const clearTimer = useCallback(() => {

        if (timerRef.current) {

            clearInterval(
                timerRef.current
            );

            timerRef.current = null;
        }

    }, []);

    const startTimer = useCallback(() => {

        clearTimer();

        setTimeLeft(
            TIMER_SECONDS
        );

        timerRef.current =
            setInterval(() => {

                setTimeLeft(
                    previous => {

                        if (previous <= 1) {

                            clearTimer();

                            return 0;
                        }

                        return previous - 1;
                    }
                );

            }, 1000);

    }, [clearTimer]);

    /*
     * Récupération du salon pour savoir
     * si l'utilisateur connecté est le créateur.
     */
    useEffect(() => {

        if (!code || !currentUser) {
            return;
        }

        getSalon(code)
            .then(salon => {

                setIsCreator(
                    salon.createurId === currentUser.id
                );

            })
            .catch(error => {

                console.error(
                    "Impossible de récupérer le salon :",
                    error
                );

            });

    }, [code, currentUser]);

    /*
     * Connexion WebSocket.
     */
    useEffect(() => {

        if (!code || !currentUser) {
            return;
        }

        const client = connectToGame(
            code,
            currentUser.id,
            {

                onConnected: () => {

                    console.log(
                        "WebSocket connecté."
                    );

                    setConnected(true);
                },

                onCountdown: countdownData => {

                    setCountdown(
                        countdownData.value
                    );

                    setPhase(
                        "countdown"
                    );
                },

                onQuestion: receivedQuestion => {

                    console.log(
                        "Nouvelle question :",
                        receivedQuestion
                    );

                    setQuestion(
                        receivedQuestion
                    );

                    setPhase(
                        "playing"
                    );

                    setLastResult(null);

                    setSelectedId(null);

                    setFreeAnswer("");

                    setRevealId(null);

                    setRevealAnswer(null);

                    setCountdown(null);

                    startTimer();
                },

                onScores: receivedScores => {

                    setScores(
                        receivedScores
                    );
                },

                onAnswerResult: result => {

                    setLastResult(
                        result
                    );

                    setPhase(
                        "answered"
                    );
                },

                onReveal: reveal => {

                    clearTimer();

                    setRevealId(
                        reveal.correctReponseId
                    );

                    setRevealAnswer(
                        reveal.correctAnswer
                    );

                    setPhase(
                        "reveal"
                    );
                },

                onResults: results => {

                    clearTimer();

                    setFinalScores(
                        results
                    );

                    setPhase(
                        "finished"
                    );
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
     * Le créateur lance automatiquement
     * la partie lorsque sa connexion est prête.
     */
    useEffect(() => {

        if (
            connected &&
            isCreator &&
            !startedRef.current &&
            clientRef.current &&
            code
        ) {

            startedRef.current = true;

            sendStartGame(
                clientRef.current,
                code
            );
        }

    }, [
        connected,
        isCreator,
        code,
    ]);

    const handleAnswer = (
        responseId: number | null
    ) => {

        if (
            !clientRef.current ||
            !code ||
            phase !== "playing" ||
            timeLeft === 0
        ) {
            return;
        }

        setSelectedId(
            responseId
        );

        sendAnswer(
            clientRef.current,
            code,
            responseId,
            freeAnswer.trim()
        );
    };

    const submitFreeAnswer = () => {

        if (!freeAnswer.trim()) {
            return;
        }

        handleAnswer(null);
    };

    /*
     * Résultats finaux.
     */
    if (
        phase === "finished" &&
        finalScores
    ) {

        const sorted =
            Object.entries(
                finalScores.scores
            ).sort(
                ([, scoreA], [, scoreB]) =>
                    scoreB - scoreA
            );

        return (
            <div className="results-page">

                <h1>
                    🏆 Résultats
                </h1>

                <table className="scores-table">

                    <thead>

                    <tr>
                        <th>#</th>
                        <th>Joueur</th>
                        <th>Score</th>
                    </tr>

                    </thead>

                    <tbody>

                    {sorted.map(
                        ([username, score], index) => (

                            <tr key={username}>

                                <td>
                                    {index < 3
                                        ? ["🥇", "🥈", "🥉"][index]
                                        : index + 1}
                                </td>

                                <td>
                                    {username}
                                </td>

                                <td>
                                    {score}
                                </td>

                            </tr>
                        )
                    )}

                    </tbody>

                </table>

                <button
                    onClick={() =>
                        navigate("/dashboard")
                    }
                >
                    Retour au dashboard
                </button>

            </div>
        );
    }

    if (!code) {

        return (
            <p className="error">
                Code du salon manquant.
            </p>
        );
    }

    const timerPercentage =
        (timeLeft / TIMER_SECONDS) * 100;

    return (

        <div className="game-page">

            <div className="game-header">

                <h1>
                    Quiz Arena
                </h1>

                <p>

                    Salon :
                    {" "}

                    <strong>
                        {code}
                    </strong>

                    {" — "}

                    {connected
                        ? "✅ Connecté"
                        : "⏳ Connexion..."}

                </p>

            </div>

            {phase === "waiting" && (

                <div className="game-countdown">

                    <p>
                        En attente du lancement...
                    </p>

                    {isCreator && (

                        <button
                            onClick={() => {

                                if (
                                    clientRef.current &&
                                    code
                                ) {

                                    sendStartGame(
                                        clientRef.current,
                                        code
                                    );
                                }

                            }}
                            disabled={!connected}
                        >
                            🚀 Démarrer
                        </button>

                    )}

                </div>

            )}

            {phase === "countdown" && (

                <div className="game-countdown">

                    <p>
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

            {(
                phase === "playing" ||
                phase === "answered" ||
                phase === "reveal"
            ) && question && (

                <div
                    className="question-transition"
                    key={`${question.questionId}-${phase}`}
                >

                    <p className="game-progress">

                        Question{" "}
                        {question.questionNumber}
                        {" / "}
                        {question.totalQuestions}

                    </p>

                    {phase !== "reveal" && (

                        <>

                            <div className="timer-bar-track">

                                <div
                                    className={`timer-bar-fill ${
                                        timeLeft <= 5
                                            ? "urgent"
                                            : ""
                                    }`}
                                    style={{
                                        width: `${timerPercentage}%`,
                                    }}
                                />

                            </div>

                            <p
                                className={`timer-text ${
                                    timeLeft <= 5
                                        ? "urgent"
                                        : ""
                                }`}
                            >
                                {timeLeft}s
                            </p>

                        </>

                    )}

                    {phase === "reveal" && (

                        <div className="reveal-banner">

                            {revealAnswer
                                ? `⏳ Réponse : ${revealAnswer}`
                                : "Question suivante..."}

                        </div>

                    )}

                    <p className="game-question">

                        {question.text}

                    </p>

                    {question.saisieLibre ? (

                        <div className="free-answer">

                            <input
                                value={freeAnswer}
                                onChange={event =>
                                    setFreeAnswer(
                                        event.target.value
                                    )
                                }
                                onKeyDown={event => {

                                    if (
                                        event.key === "Enter"
                                    ) {

                                        submitFreeAnswer();
                                    }

                                }}
                                disabled={
                                    phase !== "playing" ||
                                    timeLeft === 0
                                }
                                placeholder="Tape ta réponse..."
                            />

                            <button
                                onClick={
                                    submitFreeAnswer
                                }
                                disabled={
                                    phase !== "playing" ||
                                    timeLeft === 0 ||
                                    !freeAnswer.trim()
                                }
                            >
                                Valider
                            </button>

                        </div>

                    ) : (

                        <div className="game-answers">

                            {question.reponses.map(
                                response => {

                                    let className =
                                        "answer-btn";

                                    if (
                                        phase === "reveal"
                                    ) {

                                        if (
                                            response.id ===
                                            revealId
                                        ) {

                                            className +=
                                                " correct";

                                        } else if (
                                            response.id ===
                                            selectedId
                                        ) {

                                            className +=
                                                " wrong";
                                        }

                                    } else if (
                                        phase === "answered" &&
                                        lastResult
                                    ) {

                                        if (
                                            response.id ===
                                            lastResult.correctReponseId
                                        ) {

                                            className +=
                                                " correct";

                                        } else if (
                                            response.id ===
                                            selectedId &&
                                            !lastResult.correct
                                        ) {

                                            className +=
                                                " wrong";
                                        }

                                    } else if (
                                        response.id ===
                                        selectedId
                                    ) {

                                        className +=
                                            " selected";
                                    }

                                    return (

                                        <button
                                            key={response.id}
                                            className={className}
                                            onClick={() =>
                                                handleAnswer(
                                                    response.id
                                                )
                                            }
                                            disabled={
                                                phase !== "playing" ||
                                                timeLeft === 0
                                            }
                                        >
                                            {response.text}
                                        </button>

                                    );
                                }
                            )}

                        </div>

                    )}

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
                                    ? "✅ Bonne réponse !"
                                    : "❌ Mauvaise réponse"}

                                {" "}

                                <strong>
                                    +{lastResult.pointsEarned} pts
                                </strong>

                                {lastResult.multiplier > 1 && (

                                    <span>
                                        {" · "}
                                        🔥 ×
                                        {lastResult.multiplier}
                                    </span>

                                )}

                                <br />

                                Score :
                                {" "}
                                {lastResult.score}

                                {" · "}

                                Série :
                                {" "}
                                {lastResult.streak}

                            </div>

                        )}

                    {phase === "playing" &&
                        timeLeft === 0 &&
                        !lastResult && (

                            <div className="game-feedback ko">

                                ⏰ Temps écoulé !

                            </div>

                        )}

                    {scores && (

                        <p className="game-waiting">

                            {scores.answeredCount}
                            {" / "}
                            {scores.totalPlayers}
                            {" joueurs ont répondu"}

                        </p>

                    )}

                </div>

            )}

            {scores &&
                phase !== "waiting" &&
                phase !== "countdown" &&
                phase !== "finished" && (

                    <div className="scores-section">

                        <h3>
                            Scores
                        </h3>

                        <table className="scores-table">

                            <thead>

                            <tr>
                                <th>Joueur</th>
                                <th>Score</th>
                            </tr>

                            </thead>

                            <tbody>

                            {Object.entries(
                                scores.scores
                            )
                                .sort(
                                    ([, scoreA], [, scoreB]) =>
                                        scoreB - scoreA
                                )
                                .map(
                                    ([username, score]) => (

                                        <tr key={username}>

                                            <td>
                                                {username}
                                            </td>

                                            <td>
                                                {score}
                                            </td>

                                        </tr>

                                    )
                                )}

                            </tbody>

                        </table>

                    </div>

                )}

        </div>
    );
}

export default Game;