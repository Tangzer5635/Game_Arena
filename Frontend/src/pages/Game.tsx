import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import {
    connectToGame,
    sendStartGame,
    sendAnswer,
    type GameQuestion,
    type GameScores,
    type GameAnswerResult,
} from "../services/gameSocket";
import type { Client } from "@stomp/stompjs";

type Phase = "waiting" | "playing" | "answered" | "finished";

export default function Game() {
    const { code } = useParams<{ code: string }>();
    const { currentUser } = useAuth();
    const navigate = useNavigate();
    const clientRef = useRef<Client | null>(null);

    const [connected, setConnected] = useState(false);
    const [phase, setPhase] = useState<Phase>("waiting");
    const [question, setQuestion] = useState<GameQuestion | null>(null);
    const [scores, setScores] = useState<GameScores | null>(null);
    const [lastResult, setLastResult] = useState<GameAnswerResult | null>(null);
    const [selectedId, setSelectedId] = useState<number | null>(null);
    const [finalScores, setFinalScores] = useState<GameScores | null>(null);

    useEffect(() => {
        if (!code || !currentUser) return;

        const client = connectToGame(code, currentUser.id, {
            onConnected: () => setConnected(true),
            onQuestion: (q) => {
                setQuestion(q);
                setPhase("playing");
                setLastResult(null);
                setSelectedId(null);
            },
            onScores: (s) => setScores(s),
            onAnswerResult: (r) => { setLastResult(r); setPhase("answered"); },
            onResults: (r) => { setFinalScores(r); setPhase("finished"); },
        });

        clientRef.current = client;
        return () => { client.deactivate(); };
    }, [code, currentUser]);

    const handleStart = () => {
        if (clientRef.current && code) sendStartGame(clientRef.current, code);
    };

    const handleAnswer = (reponseId: number) => {
        if (!clientRef.current || !code || !currentUser || phase !== "playing") return;
        setSelectedId(reponseId);
        sendAnswer(clientRef.current, code, currentUser.id, reponseId);
    };

    if (!code) return <p className="error">Code du salon manquant.</p>;

    // ── Résultats finaux ──
    if (phase === "finished" && finalScores) {
        const sorted = Object.entries(finalScores.scores).sort(([, a], [, b]) => b - a);
        return (
            <div className="results-page">
                <h1>🏆 Résultats</h1>
                <table className="scores-table">
                    <thead>
                        <tr><th>#</th><th>Joueur</th><th>Score</th></tr>
                    </thead>
                    <tbody>
                        {sorted.map(([username, score], i) => (
                            <tr key={username}>
                                <td><span className="medal">{i === 0 ? "🥇" : i === 1 ? "🥈" : i === 2 ? "🥉" : i + 1}</span></td>
                                <td>{username}</td>
                                <td>{score}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
                <button onClick={() => navigate("/dashboard")} style={{ marginTop: 24 }}>
                    Retour au dashboard
                </button>
            </div>
        );
    }

    // ── Jeu ──
    return (
        <div className="game-page">
            <div className="game-header">
                <h1>Quiz Arena</h1>
                <p>Salon : <strong>{code}</strong> — {connected ? "✅ Connecté" : "⏳ Connexion..."}</p>
            </div>

            {phase === "waiting" && (
                <div style={{ textAlign: "center" }}>
                    <p>En attente du lancement...</p>
                    <button onClick={handleStart} disabled={!connected}>
                        🚀 Démarrer le quiz
                    </button>
                </div>
            )}

            {(phase === "playing" || phase === "answered") && question && (
                <>
                    <p className="game-progress">
                        Question {question.questionNumber} / {question.totalQuestions}
                    </p>

                    <p className="game-question">{question.text}</p>

                    <div className="game-answers">
                        {question.reponses.map((r) => {
                            let cls = "answer-btn";
                            if (phase === "answered" && lastResult) {
                                if (r.id === lastResult.correctReponseId) cls += " correct";
                                else if (r.id === selectedId && !lastResult.correct) cls += " wrong";
                            } else if (r.id === selectedId) {
                                cls += " selected";
                            }

                            return (
                                <button
                                    key={r.id}
                                    className={cls}
                                    onClick={() => handleAnswer(r.id)}
                                    disabled={phase !== "playing"}
                                >
                                    {r.text}
                                </button>
                            );
                        })}
                    </div>

                    {phase === "answered" && lastResult && (
                        <div className={`game-feedback ${lastResult.correct ? "ok" : "ko"}`}>
                            {lastResult.correct ? "✅ Bonne réponse !" : "❌ Mauvaise réponse"}
                            {" — "}Score : {lastResult.score}
                        </div>
                    )}

                    {scores && (
                        <p className="game-waiting">
                            {scores.answeredCount} / {scores.totalPlayers} joueurs ont répondu
                            {scores.answeredCount < scores.totalPlayers && " — en attente..."}
                        </p>
                    )}
                </>
            )}

            {scores && phase !== "waiting" && phase !== "finished" && (
                <div className="scores-section">
                    <h3>Scores</h3>
                    <table className="scores-table">
                        <thead><tr><th>Joueur</th><th>Score</th></tr></thead>
                        <tbody>
                            {Object.entries(scores.scores).map(([username, score]) => (
                                <tr key={username}><td>{username}</td><td>{score}</td></tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </div>
    );
}
